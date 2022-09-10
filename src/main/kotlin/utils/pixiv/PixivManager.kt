package org.celery.utils.pixiv

import exceptions.PixivException
import org.celery.utils.sql.PixivSql
import org.celery.utils.sql.PixivSql.illusts
import org.celery.utils.sql.insertOrUpdate
import org.celery.utils.sql.update
import config.pixiv.config.ConfigData
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import net.mamoe.mirai.utils.MiraiLogger
import org.ktorm.dsl.eq
import org.ktorm.dsl.or
import org.ktorm.entity.find
import org.celery.utils.pixiv.pixivClient.PixivClientPool
import xyz.cssxsh.pixiv.*
import xyz.cssxsh.pixiv.apps.*
import xyz.cssxsh.pixiv.exception.AppApiException
import java.io.File
import java.time.LocalDate

private const val MAX_RETRY = 4

object PixivManager {
    val logger = MiraiLogger.Factory.create(this::class, "PixivClientManager")

    private val client: PixivClientPool.AuthClient
        get() {
//           println("获取一个client实例")
            return PixivClientPool.client()
        }


    /**
     * @receiver 父文件夹
     */
    suspend fun File.saveAll(info: IllustInfo, overWrite: Boolean = false) {
        if (!exists()) mkdirs()
        resolve("${info.pid}.json").apply illustInfo@{
            if (exists() && !overWrite) return@illustInfo
            saveIllustInfo(info, overWrite)
//            logger.info("保存${info.pid}的元数据成功 在${this@saveAll.name}")
        }
        if (info.type == WorkContentType.UGOIRA) resolve("${info.pid}_ugoira.json").apply ugoira@{
            if (exists() && !overWrite) return@ugoira
            if (!ConfigData.config.useClient) {
                logger.warning("未启用客户端,pid为:${info.pid}的插画无法获取Gif元数据")
                return
            }
            runCatching {
//                    logger.info("获取${info.pid}的Gif元数据...")
                var ugoira: UgoiraMetadata? = null
                do {
                    try {
                        ugoira = client.ugoiraMetadata(info.pid).ugoira
                    } catch (e: AppApiException) {
                        handle(e, info.pid)
                    }
                } while (ugoira == null)
                saveUgoiraInfo(ugoira, overWrite)
//                    logger.info("保存${info.pid}的Gif元数据成功 在${this@saveAll.name}")
            }.onFailure {
                logger.error("获取${info.pid}的Gif元数据失败 $it")
            }

        }
//        if (info.type==WorkContentType.MANGA)
//            logger.warn("暂不支持保存漫画元数据")
    }

    private fun File.saveIllustInfo(info: IllustInfo, overWrite: Boolean) {
        if (exists() && !overWrite) return
        writeText(PixivJson.encodeToString(IllustInfo.serializer(), info))
        return
    }

    private fun File.saveUgoiraInfo(info: UgoiraMetadata, overWrite: Boolean) {
        if (exists() && !overWrite) return
        writeText(PixivJson.encodeToString(UgoiraMetadata.serializer(), info))
        return
    }

    private fun handle(exception: AppApiException, pid: Long? = null) {
        when {
            "Rate Limit" in exception.message -> {
                synchronized(client) {
                    PixivClientPool.limit(client.uid)
                }
            }
            else -> {
                logger.error("pid: $pid ,error: $exception")
                throw exception
            }
        }
    }

    class Handler {
        private val counter = mutableMapOf<Long, Int>()
        fun handleAll(exception: Exception, pid: Long) {
            when (exception) {
                is CancellationException -> {
                    counter.putIfAbsent(pid, 0)?.let {
                        counter[pid] = it + 1
                    }

                    if (counter[pid]!! >= MAX_RETRY) throw exception
                    else logger.warning("ignore($pid),retry=${counter[pid]},err=${exception.javaClass.simpleName}")
                }
                else -> {
                    logger.error("pid: $pid ,error: $exception")
                    throw exception
                }
            }
        }
    }

    suspend fun illustDetail(pid: Long, update: Boolean = false): IllustSingle {
        if (!update) try {
            return PixivSql.database?.illusts?.find {
                (PixivSql.Illusts.pid eq pid).let { expression ->
                    var declare = expression
                    listOf<Long>().forEach { it1 ->
                        declare = declare or (PixivSql.Illusts.pid eq it1)
                    }
                    declare
                }
            }?.let {
                if (it.deleted) {
                    throw PixivException.DeletedException()
                }
                IllustSingle(it.origin.deserializeFromJson())

            } ?: throw NullPointerException()
        } catch (_: NullPointerException) {
        }
        var illustSingle: IllustSingle? = null
        val handler = Handler()
        while (illustSingle == null) {
            try {
                var info: IllustInfo?
                illustSingle = client.illustDetail(pid)

                if (!illustSingle.illust.visible) {
                    logger.info("$pid 不可见,将切换客户端尝试")
                    var trys = 2
                    do {
                        try {
                            illustSingle = PixivClientPool.client { it.ageLimit == AgeLimit.R18G }.let { it ->
                                logger.info("已找到pixiv客户端")
                                it.illustDetail(pid).also { it.illust.update() }
                            }
                        } catch (e: NoSuchElementException) {
                            logger.warning("没有找到合适的客户端," + PixivClientPool.clients.values.joinToString(
                                "\n"
                            ) { it.auth!!.user.name + " " + it.ageLimit.name })
                            throw e
                        } catch (e: Exception) {
                            logger.error(e)
                            throw e
                        }
                    } while (--trys > 0 && illustSingle?.illust?.visible == false)
                    if (illustSingle?.illust?.visible == false) {
                        logger.warning("$pid 无法获取信息(R18)")
                        throw Exception()
//                            errorList.add(pid)
                    } else {
//                            println(PixivJson.encodeToString(info))
                    }
//                    info.let {
                    //                            DownloaderManager.downloadFolder.resolve(pid.toString()).saveAll(it, true)
//                    }

//                    info
                }
            } catch (e: AppApiException) {
                handle(e, pid)
            } catch (e: Exception) {
                handler.handleAll(e, pid)
            }
        }
        PixivSql.launch {
            illustSingle.illust.insertOrUpdate()
        }
        return illustSingle
    }

    suspend fun userIllusts(
        uid: Long,
        type: WorkContentType? = null,
        filter: FilterType? = null,
        offset: Long = 0,
        url: String = USER_ILLUSTS,
    ): IllustData {
        var illustData: IllustData? = null
        while (illustData == null) {
            try {
                illustData = client.userIllusts(uid, type, filter, offset, url)
            } catch (e: AppApiException) {
                handle(e)
            }
        }
        PixivSql.launch {
            illustData.illusts.forEach { it.insertOrUpdate() }
        }
        return illustData
    }

    suspend fun searchIllust(
        word: String,
        target: SearchTarget? = null,
        sort: SearchSort? = null,
        duration: SearchDuration? = null,
        min: Long? = null,
        max: Long? = null,
        start: LocalDate? = null,
        end: LocalDate? = null,
        translated: Boolean? = null,
        merge: Boolean? = null,
        offset: Long? = null,
        filter: FilterType? = null,
        url: String = SEARCH_ILLUST,
    ): IllustData {
        var illustData: IllustData? = null
        while (illustData == null) {
            try {
                illustData = client.searchIllust(
                    word, target, sort, duration, min, max, start, end, translated, merge, offset, filter, url
                )
            } catch (e: AppApiException) {
                handle(e)
            }
        }
        PixivSql.launch {

        }
        return illustData
    }

    suspend fun ugoiraMetadata(pid: Long): UgoiraInfo {
        var ugoiraInfo: UgoiraInfo? = null
        val handler = Handler()
        while (ugoiraInfo == null) {
            try {
                ugoiraInfo = client.ugoiraMetadata(pid)
            } catch (e: AppApiException) {
                handle(e)
            } catch (e: Exception) {
                handler.handleAll(e, pid)
            }
        }
        return ugoiraInfo
    }

    suspend fun illustRelated(
        pid: Long,
        filter: FilterType? = null,
        offset: Long? = null,
        url: String = ILLUST_RELATED,
    ): IllustData {
        var illustData: IllustData? = null
        while (illustData == null) {
            try {
                illustData = client.illustRelated(pid, filter, offset, url)
            } catch (e: AppApiException) {
                handle(e)
            }
        }
        return illustData
    }
}
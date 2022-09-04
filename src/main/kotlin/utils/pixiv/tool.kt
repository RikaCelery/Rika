package pixivClient

import com.celery.com.celery.rika.Rika
import com.celery.rika.utils.pixiv.MyPixivDownloader
import com.celery.rika.utils.pixiv.PixivManager.saveAll
import com.celery.rika.utils.sql.insertOrUpdate
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import xyz.cssxsh.pixiv.SearchTarget
import xyz.cssxsh.pixiv.apps.IllustInfo
import xyz.cssxsh.pixiv.apps.searchIllust
import xyz.cssxsh.pixiv.exception.AppApiException
import java.io.File

private val logger = Rika.logger
//fun Collection<IllustInfo>.downloadAll() = this.forEach {
//    if (it.type== WorkContentType.ILLUST)
//        DownloaderManager.illustList.add(it)
//
//    if (it.type== WorkContentType.UGOIRA)
//        DownloaderManager.gifList.add(it)
//
//    if (it.type== WorkContentType.MANGA) {
//        println("not suppot ${it.pid},download as illust")
//        DownloaderManager.illustList.add(it)
//    }
//}
suspend fun IllustInfo.getFiles(n: Int? = null): MutableList<File> {
    val list = mutableListOf<File>()
    supervisorScope {
        this@getFiles.save()
        getOriginImageUrls().forEachIndexed { index, url ->
            if (n != null && index >= n) return@forEachIndexed
            launch {
                if (Rika.DEBUG_MODE) logger.debug("${url.fullPath.substringAfterLast('/')}下载中...")
                val picName = url.fullPath.substringAfterLast('/')
                val file = Rika.resolveDataFile("PictureLib\\Ero\\${pid}\\$picName")
                if (file.exists()) {
                    if (Rika.DEBUG_MODE) logger.debug("exist $file")
                    list.add(file)
                } else {
                    MyPixivDownloader.download(url)
                        .apply { file.parentFile.mkdirs(); file.writeBytes(this) }
                    if (Rika.DEBUG_MODE) logger.debug("${file.name}下载完毕")
                    list.add(file)
                }
            }
        }
    }
    return list
}

suspend fun IllustInfo.save(overWrite: Boolean = false) {
    Rika.dataFolder.resolve("PictureLib\\Ero\\${this.pid}").saveAll(this, overWrite)
}

suspend fun PixivClientPool.AuthClient.searchIllustrate(
    keyWord: String, offset: Long = 0,
    targetResultAmmount: Int,
    result: MutableList<IllustInfo> = mutableListOf(),
    target: SearchTarget = SearchTarget.EXACT_MATCH_FOR_TAGS,
    predicate: (illustInfo: IllustInfo) -> Boolean,
): Pair<MutableList<IllustInfo>, Long?> {
    var client = this
    logger.info("searching $keyWord offset: $offset")
    val temp = try {
        client.searchIllust(keyWord, offset = offset, target = SearchTarget.EXACT_MATCH_FOR_TAGS)
    } catch (e: AppApiException) {
        if ("Rate Limit" in e.message) {
            PixivClientPool.limit(client.uid)
            client = PixivClientPool.client()
            client.searchIllust(keyWord, offset = offset, target = SearchTarget.EXACT_MATCH_FOR_TAGS)
        } else
            throw e
    }
    temp.illusts.forEach {
        launch { it.insertOrUpdate() }
        if (predicate(it)) {
            result.add(it)
        }
    }
    var last = offset
    if (result.size < targetResultAmmount && temp.nextUrl != null) try {
        val pair = client.searchIllustrate(
            keyWord = keyWord,
            offset = temp.nextUrl!!.substringAfterLast('=').toLong(),
            targetResultAmmount = targetResultAmmount,
            result = result,
            predicate = predicate
        )
        last = pair.second ?: offset
    } catch (e: Exception) {
        logger.info("${target.name} searchIllustrate $keyWord offset: $offset  got: ${result.size}")
    }
    return result to last
}
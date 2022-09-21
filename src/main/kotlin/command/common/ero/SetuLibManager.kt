package command.common.ero

import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.command.common.ero.impl.SetuLibLocal
import org.celery.command.common.ero.impl.SetuLibWeb
import org.celery.command.common.ero.impl.SetuPixivLazyLib
import org.celery.config.main.Keys
import org.celery.config.main.PublicConfig
import java.io.File

object SetuLibManager : Collection<SetuLib> {
    private val setuLibs: MutableList<SetuLib> = mutableListOf()
    val logger = MiraiLogger.Factory.create(this::class)
    private val defaultWeb = mapOf(
        "alias" to "别名1,别名2",
        "apiUrl" to "https://",
        "responseType" to "image 或 json",
        "picKey" to "key",
        "libName" to "web-1",
        "save" to "false"
    )
    private val defaultLocal = mapOf(
        "alias" to "别名1,别名2",
        "libPath" to "C:/image 或 image",
        "libName" to "web-1"
    )
    val saveFolder = File(PublicConfig[Keys.MAX_COUNT_DEFAULT, Rika.dataFolderPath.toString()])
    fun reload() {
        setuLibs.clear()
        setuLibs.add(SetuPixivLazyLib)
        PublicConfig["WebSetuAPI", mutableListOf(defaultWeb)].forEach { map ->
            try {
                setuLibs.add(
                    SetuLibWeb(
                        alias = map["alias"]!!.split(","),
                        apiUrl = map["apiUrl"]!!,
                        responseType = map["responseType"],
                        picKey = map["picKey"],
                        libName = map["libName"]!!,
                        save = map["save"]!!.toBoolean()
                    )
                )
            } catch (e: Exception) {
                logger.error("网络涩图库配置错误${map.toMap()}")
            }
        }
        PublicConfig["LocalSetu", mutableListOf(defaultLocal)].forEach {
            val map = it
            try {
                setuLibs.add(
                    SetuLibLocal(
                        alias = map["alias"]!!.split("|"),
                        libPath = map["libPath"]!!,
                        libName = map["libName"]!!
                    )
                )
            } catch (e: Exception) {
                logger.error("网络涩图库配置错误${it.toMap()}")
            }
        }
        setuLibs.forEach {
            logger.debug("已初始化lib: " + it.libName + "\t\talias: " + it.alias)
        }
    }


    override val size: Int
        get() = setuLibs.size

    override fun isEmpty(): Boolean {
        return setuLibs.isEmpty()
    }

    override fun iterator(): Iterator<SetuLib> {
        return setuLibs.iterator()
    }

    override fun containsAll(elements: Collection<SetuLib>): Boolean {
        return setuLibs.containsAll(elements)
    }

    override fun contains(element: SetuLib): Boolean {
        return setuLibs.contains(element)
    }

    init {
        try {
            reload()
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}
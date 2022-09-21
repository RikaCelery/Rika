package org.celery.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.utils.file.createParentFolder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer

abstract class AutoReloadable(private val fileName: String, prettyPrint: Boolean = false) {
    protected val logger by lazy {
        MiraiLogger.Factory.create(this::class)
    }
    protected val pluginConfigsFile = Rika.configFolder.resolve("$fileName.json").createParentFolder()
    protected var lastModified = 0L
    val atomicBoolean = AtomicBoolean(false)
    open val modules = mutableListOf<SerializersModule>()
    open val jsonSerializer =
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            modules.forEach {
                serializersModule += it
            }
            this.prettyPrint = prettyPrint
        }

    fun reload() {
        try {
            load()
            save()
            lastModified = pluginConfigsFile.lastModified()
        } catch (_: Exception) {
            pluginConfigsFile.parentFile.resolve("callConfigs.json.bac").delete()
            pluginConfigsFile.renameTo(pluginConfigsFile.parentFile.resolve("callConfigs.json.bac"))
            save()
            lastModified = pluginConfigsFile.lastModified()
            load()
            logger.warning("插件配置文件加载出错,已重新生成,原文件已重命名为callConfigs.json.bac")
        }
    }

    abstract fun load()
    abstract fun save()
    val timer:Timer = timer("auto-save-timer-for:$fileName", true, 0, 500) {
        if (pluginConfigsFile.lastModified() != lastModified) {
            lastModified = pluginConfigsFile.lastModified()
            try {
                reload()
            } catch (e: Exception) {
                logger.error("${fileName}重载失败", e)
            }
        }
        if (atomicBoolean.get())
            try {
                save()
                lastModified = pluginConfigsFile.lastModified()
            } catch (e: Exception) {
                logger.error("${fileName}保存失败", e)
            } finally {
                atomicBoolean.set(false)
            }
    }

}
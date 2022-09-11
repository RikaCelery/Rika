package org.celery.config.main

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.debug
import net.mamoe.mirai.utils.info
import org.celery.Rika
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object PublicConfig {
    private val logger by lazy {
        MiraiLogger.Factory.create(this::class)
    }
    private val pluginConfigsFile = Rika.configFolder.resolve("pluginConfigs.json")

    private var lastModified  = pluginConfigsFile.lastModified()

    val atomicBoolean = AtomicBoolean(true)

    val modules = mutableListOf<SerializersModule>()

    var jsonSerializer = Json {
        ignoreUnknownKeys = true
        modules.forEach {
            serializersModule += it
        }
        prettyPrint = true
    }

    private var pluginConfigs = mutableMapOf<String, String>()

    val keys: List<String>
        get() = pluginConfigs.keys.toList()
    val entries: List<Map.Entry<String,String>>
        get() = pluginConfigs.entries.toList()

    operator fun set(key: String, value: String) {
        atomicBoolean.set(true)
        pluginConfigs[key] = value
    }

    operator fun get(key: String): String? {
        return pluginConfigs[key]
    }

    fun save() {
        logger.debug{ "save publicConfig.json" }
        pluginConfigsFile.writeText(jsonSerializer.encodeToString(pluginConfigs))
    }

    fun load() {
        pluginConfigs = jsonSerializer.decodeFromString(pluginConfigsFile.readText())
    }

    fun reload() {
        try {
            load()
            save()
        } catch (_: Exception) {
            pluginConfigsFile.parentFile.resolve("pluginConfigs.json.bac").delete()
            pluginConfigsFile.renameTo(pluginConfigsFile.parentFile.resolve("pluginConfigs.json.bac"))
            save()
            load()
            logger.warning("插件配置文件加载出错,已重新生成,原文件已重命名")
        }
    }

    init {
        reload()
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (pluginConfigsFile.lastModified()!=lastModified){
                    lastModified = pluginConfigsFile.lastModified()
                    try {
                        load()
                        logger.info{"重载配置成功"}
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (atomicBoolean.get())
                    try {
                        save()
                    } catch (e: Exception) {
                        logger.error("配置文件加载出错,重新生成失败", e)
                    } finally {
                        atomicBoolean.set(false)
                    }
            }
        }, 1000, 1000)
    }
}
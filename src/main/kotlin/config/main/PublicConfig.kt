package org.celery.config.main

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.Rika
import org.celery.utils.serialization.defaultJson
import java.util.*

object PublicConfig {
    private val logger by lazy {
        MiraiLogger.Factory.create(this::class)
    }
    private val pluginConfigsFile = Rika.configFolder.resolve("pluginConfigs.json")

    val modules = mutableListOf<SerializersModule>()

    var jsonSerializer = Json {
        ignoreUnknownKeys = true
        modules.forEach {
            serializersModule += it
        }
    }


    fun set(key: String, value: String) {
        pluginConfigs[key] = value
    }

    fun get(key: String): String? {
        return pluginConfigs[key]
    }

    var pluginConfigs = mutableMapOf<String, String>()
    fun save() {
        pluginConfigsFile.writeText(defaultJson.encodeToString(pluginConfigs))
    }

    private fun load() {
        pluginConfigs = defaultJson.decodeFromString(pluginConfigsFile.readText())
    }

    fun reload() {
        try {
            load()
            save()
        } catch (_: Exception) {
            save()
            load()
            logger.warning("插件配置文件加载出错,已重新生成")
        }
    }

    init {
        reload()
        Timer().schedule(object : TimerTask() {
            override fun run() {
                try {
                    save()
                } catch (e: Exception) {
                    logger.error("配置文件加载出错,重新生成失败", e)
                }
            }
        }, 1000, 1000)
    }
}
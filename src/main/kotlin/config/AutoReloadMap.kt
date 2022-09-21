package org.celery.config

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.celery.utils.serialization.defaultJson

abstract class AutoReloadMap(fileName: String, prettyPrint: Boolean = false) : AutoReloadable(fileName, prettyPrint) {

    override fun save() {
        pluginConfigsFile.writeText(defaultJson.encodeToString(pluginConfigs))
        lastModified = pluginConfigsFile.lastModified()
    }

    override fun load() {
        pluginConfigs =
            defaultJson.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                pluginConfigsFile.readText()
            ) as MutableMap<String, String>
    }
    val usages:MutableList<String>?=null
    private var pluginConfigs: MutableMap<String, String> = usages?.associateWith { "!a帮助" }?.toMutableMap() ?:mutableMapOf()
    val keys: List<String>
        get() = pluginConfigs.keys.toList()
    val entries: List<Map.Entry<String, String>>
        get() = pluginConfigs.entries.toList()

    operator fun set(key: String, value: String) {
        synchronized(this) {
            if (pluginConfigs[key] != value) {
                pluginConfigs[key] = value
                atomicBoolean.set(true)
            }
        }
    }

    operator fun get(key: String): String? {
        synchronized(this) {
            return pluginConfigs[key]
        }
    }

    inline operator fun <reified T> set(key: String, value: T) {
        if (value is String) {
            if (this[key] != value) {
                this[key] = value
                atomicBoolean.set(true)
            }
        } else if (value is Enum<*>) {
            if (this[key] != value) {
                this[key] = value.name
            }
        } else {
            if (this[key] != jsonSerializer.encodeToString(value)) {
                this[key] = jsonSerializer.encodeToString(value)
                atomicBoolean.set(true)
            }
        }
    }

    /**
     * 反序列化指定值,key不存在时自动新建
     */
    inline operator fun <reified T> get(key: String, defaultValue: T? = null): T {
        if (defaultValue is String) {
            if (this[key] == null) {
                this[key] = defaultValue
            }
            return this[key]!! as T
        }
        if (defaultValue is Enum<*>) {
//            println(defaultValue)
//            println(this[key])
            if (this[key] == null) {
                this[key] = defaultValue.name
            }
            return jsonSerializer.decodeFromString("\"" + this[key]!! + "\"")
        }
        if (this[key] == null) {
            if (defaultValue == null)
                throw NoSuchElementException("$key not exist")
            else {
                this[key] = jsonSerializer.encodeToString(defaultValue)
            }
        }
        return jsonSerializer.decodeFromString(this[key]!!)
    }

    fun getOrNull(key: String): String? {
        return pluginConfigs[key]
    }

    inline fun <reified T> getOrNull(key: String): T? {
        if (this[key] == null) return null
        return jsonSerializer.decodeFromString(this[key]!!)
    }


}
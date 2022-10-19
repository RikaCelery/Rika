package org.celery.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * 可以自动重载和保存数据的配置文件
 */
abstract class Reloadable(val path: String) {
    companion object {
        val list = mutableListOf<Reloadable>()
    }

    /**
     * Yaml序列化/反序列化器
     */
    val mapper by lazy {
        val mapper = ObjectMapper(YAMLFactory())
        mapper.registerModule(
            KotlinModule.Builder().withReflectionCacheSize(512).configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false).configure(KotlinFeature.NullIsSameAsDefault, true)
                .configure(KotlinFeature.SingletonSupport, false).configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
        mapper
    }

    /**
     * 显示调试输出
     */
    var debugMode = false

    /**
     * 存储数据
     */
    var data = mutableMapOf<String, Any?>()

    /**
     * 数据是否发生变化
     */
    val isModified = AtomicBoolean(false)

    /**
     * 数据文件位置
     */
    private val file = File("$path.yaml").canonicalFile.apply {
        if (parentFile.exists().not()) parentFile.mkdirs()
        if (exists().not()) createNewFile()
    }

    /**
     * 上次修改时间，用于热重载
     */
    private var lastModified = file.lastModified()

    /**
     * 获取反序列化后的结果
     *
     * 该实现是线程安全的
     */
    inline operator fun <reified T : Any> get(key: String, defaultValue: T? = null): T {
        synchronized(this) {
            val data = data[key]
            if (data != null) {
                // linkedHashMap
                try {
                    return mapper.convertValue<T>(data)
                } catch (e: Exception) {
                    println(e)
                    this.data[key] = defaultValue
                    isModified.set(true)
                    return defaultValue!!
                }
            }
            if (defaultValue == null) throw NoSuchElementException("$key not exist.")
            this.data[key] = defaultValue
            isModified.set(true)
        }
        return defaultValue!!
    }

    /**
     * 获取反序列化后的结果
     *
     * 该实现是线程安全的
     */
    inline fun <reified T> getOrDefault(key: String, defaultValue: T): T {
        synchronized(this) {
            val value = data[key]
            if (value != null) {
                // linkedHashMap
                try {
                    return mapper.convertValue<T>(value)
                } catch (e: Exception) {
                    println(e)
                    this.data[key] = defaultValue
                    isModified.set(true)
                    return defaultValue!!
                }
            }else data[key] = defaultValue
            isModified.set(true)
            return defaultValue
        }
    }

    /**
     * 获取反序列化后的结果
     *
     * 该实现是线程安全的
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <reified T : Any> getOrDefault(key: String, defaultValue: () -> T?): T? {
        contract {
            callsInPlace(defaultValue,InvocationKind.AT_MOST_ONCE)
        }
        synchronized(this) {
            val value = data[key]
            if (value != null) {
                // linkedHashMap
                try {
                    return mapper.convertValue<T>(value)
                } catch (e: Exception) {
                    println(e)
                    this.data[key] = defaultValue()
                    isModified.set(true)
                    return defaultValue()
                }
            } else data[key] = defaultValue()
            isModified.set(true)
            return data[key] as T
        }
    }

    /**
     * 获取反序列化后的结果
     *
     * 该实现是线程安全的
     */
    inline fun <reified T : Any> getOrNull(key: String): T? {
        synchronized(this) {
            val value = data[key]
            if (value != null) {
                // linkedHashMap
                try {
                    return mapper.convertValue<T>(value)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }
            }
            isModified.set(true)
            return null


        }
    }

    /**
     * 存入数据
     *
     * 该实现是线程安全的
     */
    inline operator fun <reified T : Any> set(key: String, value: T) {
        synchronized(this) {
            if (data[key] != value) {
                data[key] = value
                isModified.set(true)
            }
        }
    }

    @Synchronized
    fun load() {
        data.clear()
        data =
            mapper.readValue<Map<String, Any?>>(file.readText()).filter { it.value != null } as MutableMap<String, Any?>
        lastModified = file.lastModified()
        isModified.set(false)
    }

    @Synchronized
    fun save() {
        file.writeText(
            mapper.writeValueAsString(data)
        )
        lastModified = file.lastModified()
        isModified.set(false)
    }

    /**
     * 从文件重载数据（该操作会覆盖内存数据！）
     */
    fun reload() {
        try {
            load()
        } catch (e: Exception) {
            save()
        }
    }

    fun clear() {
        data.clear()
        isModified.set(true)
    }


    private val reloader: Timer = timer("auto-reloader:$path", true, 0, 1000) {
        if (isModified.get()) {
//            colorln("save change").cyan()
            try {
                save()
            } catch (e: Exception) {
                e.printStackTrace()
                reload()
            }
            return@timer
        }
        if (lastModified != file.lastModified()) {
            reload()
        }

    }


    init {
        try {
            load()
        } catch (e: Exception) {
            save()
        }
        list.add(this)
        reloader
    }
}
package org.celery.command.controller

import kotlinx.serialization.DeserializationStrategy
import org.celery.Rika
import org.celery.config.main.PublicConfig
import org.celery.config.main.PublicConfig.jsonSerializer
import org.celery.utils.file.createParentFolder
import java.io.File

interface CCommand: Limitable,CommandInfo {
    override val commandId: String
        get() = TODO("Not yet implemented")
    override var defultEnable: Boolean
        get() = true
        set(value) {}
    override var defultCountLimit: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var defultCallCountLimitMode: BlockRunMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var defultMinCooldown: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    /**
     * 拿一个文件
     */
    fun getDataFile(relative: String): File {
        val file = Rika.dataFolderPath.resolve("pluginDataFolder").resolve(commandId).resolve(relative).toFile()
//        if (file.parentFile.exists().not())
//            file.parentFile.mkdirs()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    /**
     * value 必须是Serializable的，否则需要自行覆盖[PublicConfig.jsonSerializer]
     */
    fun <T> getConfig(deserializer: DeserializationStrategy<T>, key: String): T {
        val commandName = commandId
        val newKey = commandName + "." + key
        if (PublicConfig.pluginConfigs[newKey] == null) {
            throw NoSuchElementException("$key not exist")
        }
        return jsonSerializer.decodeFromString(deserializer, PublicConfig.pluginConfigs[newKey]!!)
    }

    /**
     * value 必须是Serializable的，否则需要自行覆盖[PublicConfig.jsonSerializer]
     */
    fun <T> getConfigOrNull(deserializer: DeserializationStrategy<T>, key: String): T? {
        val commandName = commandId
        val newKey = commandName + "." + key
        if (PublicConfig.pluginConfigs[newKey] == null) {
            return null
        }
        return jsonSerializer.decodeFromString(deserializer, PublicConfig.pluginConfigs[newKey]!!)
    }
}
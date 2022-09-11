package org.celery.command.controller

import kotlinx.serialization.KSerializer
import org.celery.Rika
import org.celery.config.main.PublicConfig
import org.celery.config.main.PublicConfig.jsonSerializer
import org.celery.utils.file.createParentFolder
import java.io.File

interface CCommand: Limitable,CommandInfo {
    override val commandId: String
        get() = "Not yet implemented"
    override var defaultEnable: Boolean
        get() = true
        set(value) {}
    override var defaultCountLimit: Int
        get() = -1
        set(value) {}
    override var defaultCallCountLimitMode: BlockRunMode
        get() = BlockRunMode.Global
        set(value) {}
    override var defaultCoolDown: Long
        get() = -1
        set(value) {}



    /**
     * 拿一个文件
     */
    fun getPublicTempFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve(relative).toFile()
        file.createParentFolder()
        return file
    }
    /**
     * 拿一个文件
     */
    fun getOrCreatePublicTempFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }
    /**
     * 拿一个文件
     */
    fun getPublicDataFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("publicDataFolder").resolve(relative).toFile()
        file.createParentFolder()
        return file
    }
    /**
     * 拿一个文件
     */
    fun getOrCreatePublicDataFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("pluginDataFolder").resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }
    /**
     * 拿一个文件
     */
    fun getDataFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("pluginDataFolder").resolve(parentName).resolve(relative).toFile()
        file.createParentFolder()
        return file
    }
    /**
     * 拿一个文件,不存在则新建
     */
    fun getOrCreateDataFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("pluginDataFolder").resolve(parentName).resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }
    /**
     * 拿一个文件
     */
    fun getTempFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve("temp_"+parentName).resolve(relative).toFile()
        file.createParentFolder()
        return file
    }
    /**
     * 拿一个文件,不存在则新建
     */
    fun getOrCreateTempFile(relative: String,parentName:String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve("temp_"+parentName).resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    fun perCheck(){
        //do someThing
    }

    /**
     * value 必须是Serializable的，否则需要自行添加[PublicConfig.modules]
     */
    fun <T> getConfig(serializer: KSerializer<T>, key: String, defaultValue: T?=null): T {
        val commandName = commandId
        val newKey = "$commandName.$key"
        if (PublicConfig[newKey] == null) {
            if (defaultValue==null)
                throw NoSuchElementException("$key not exist")
            else
                PublicConfig[newKey] = jsonSerializer.encodeToString(serializer, defaultValue)
        }
        return jsonSerializer.decodeFromString(serializer, PublicConfig[newKey]!!)
    }
    fun  getConfig(key: String, defaultValue: String?=null): String {
        val commandName = commandId
        val newKey = "$commandName.$key"
        if (PublicConfig[newKey] == null) {
            if (defaultValue==null)
                throw NoSuchElementException("$key not exist")
            else
                PublicConfig[newKey] = defaultValue
        }
        return PublicConfig[newKey]!!
    }

    /**
     * value 必须是Serializable的，否则需要自行覆盖[PublicConfig.jsonSerializer]
     */
    fun <T> getConfigOrNull(serializer: KSerializer<T>, key: String): T? {
        val commandName = commandId
        val newKey = "$commandName.$key"
        if (PublicConfig[newKey] == null) {
            return null
        }
        return jsonSerializer.decodeFromString(serializer, PublicConfig[newKey]!!)
    }

    /**
     * value 必须是Serializable的，否则需要自行覆盖[PublicConfig.jsonSerializer]
     */
    fun <T> requireConfig(serializer: KSerializer<T>, key: String, defaultValue: T): T {
        val newKey = "$commandId.$key"
        if (PublicConfig[newKey] == null) {
            PublicConfig[newKey] = jsonSerializer.encodeToString(serializer, defaultValue)
        }
        return jsonSerializer.decodeFromString(serializer,PublicConfig[newKey]!!)
    }

}
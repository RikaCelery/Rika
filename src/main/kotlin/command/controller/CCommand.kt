package org.celery.command.controller

import org.celery.Rika
import org.celery.config.main.PublicConfig
import org.celery.utils.file.createParentFolder
import java.io.File

interface CCommand : Limitable, CommandInfo {



    /**
     * 拿一个文件
     */
    fun getPublicTempFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve(relative).toFile()
        file.createParentFolder()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getOrCreatePublicTempFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getPublicDataFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("publicDataFolder").resolve(relative).toFile()
        file.createParentFolder()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getOrCreatePublicDataFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("pluginDataFolder").resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getDataFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("pluginDataFolder").resolve(parentName).resolve(relative).toFile()
        file.createParentFolder()
        return file
    }

    /**
     * 拿一个文件,不存在则新建
     */
    fun getOrCreateDataFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("pluginDataFolder").resolve(parentName).resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    /**
     * 拿一个文件
     */
    fun getTempFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve("temp_" + parentName).resolve(relative).toFile()
        file.createParentFolder()
        return file
    }

    /**
     * 拿一个文件,不存在则新建
     */
    fun getOrCreateTempFile(relative: String, parentName: String = commandId): File {
        val file = Rika.dataFolderPath.resolve("temp").resolve("temp_" + parentName).resolve(relative).toFile()
        file.createParentFolder()
        if (file.exists().not())
            file.createNewFile()
        return file
    }

    fun perCheck() {
        //do someThing
    }

}


/**
 * value 必须是Serializable的，否则需要自行添加[PublicConfig.modules]
 */
inline fun <reified T : Any> CCommand.getConfig(key: String, defaultValue: T? = null): T {
    val commandName = commandId
    val newKey = "$commandName.$key"
    return PublicConfig[newKey, defaultValue]
}

/**
 * value 必须是Serializable的，否则需要自行添加[PublicConfig.modules]
 */
inline fun <reified T : Any> CCommand.setConfig(key: String, value: T, commandName: String = commandId): T {
    val newKey = "$commandName.$key"
    PublicConfig[newKey] = value

    return value
}


/**
 * value 必须是Serializable的，否则需要自行覆盖[PublicConfig.jsonSerializer]
 */
inline fun <reified T : Any> CCommand.getConfigOrNull(key: String): T? {
    val commandName = commandId
    val newKey = "$commandName.$key"
    if (PublicConfig.getOrNull(newKey) != null)
        return PublicConfig[newKey, null]
    else
        return null
}
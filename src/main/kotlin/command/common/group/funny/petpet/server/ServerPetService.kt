package command.common.group.funny.petpet.server

import command.common.group.funny.petpet.server.ServerConfig.Companion.getConfig
import command.common.group.funny.petpet.share.BasePetService
import net.mamoe.mirai.utils.cast
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class ServerPetService : BasePetService() {
    var port = 2333
    var path = "data/xmmt.dituon.petpet"
    var threadPoolSize = 10
    var headless = true
    fun readConfig() {
        val configFile = File("config.json")
        try {
            if (!configFile.exists()) { //save default config
                val `is` = this.javaClass.classLoader.getResourceAsStream("config.json")!!
                Files.write(Paths.get("config.json"), `is`.readAllBytes())
            }
            val (port1, dataPath, threadPoolSize1, gifMaxSize, gifEncoder, headless1) = getConfig(
                BasePetService.getFileStr(
                    configFile
                )
            )
            port = port1
            path = dataPath
            threadPoolSize = threadPoolSize1
            headless = headless1
            super.setGifMaxSize(gifMaxSize.cast())
            super.encoder = gifEncoder
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }
}
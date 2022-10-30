package command.common.group.funny.petpet.server

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.Executors

object WebServer {
    val petService: ServerPetService = ServerPetService()
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        petService.readConfig()
        if (petService.headless) System.setProperty("java.awt.headless", "true")
        petService.readData(File(petService.path))
        val httpServer = HttpServer.create(InetSocketAddress(petService.port), 0)
        httpServer.createContext("/petpet", PetHttpHandler())
        httpServer.executor = Executors.newFixedThreadPool(petService.threadPoolSize)
        httpServer.start()
        println("PetpetWebServer started in port " + petService.port)
        println("API-URL: 127.0.0.1:" + petService.port + "/petpet")
    }
}
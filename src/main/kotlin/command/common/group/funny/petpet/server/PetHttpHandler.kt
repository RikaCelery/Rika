package command.common.group.funny.petpet.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import command.common.group.funny.petpet.share.AvatarData
import command.common.group.funny.petpet.share.KeyData
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer
import java.util.function.Consumer

class PetHttpHandler : HttpHandler {
    override fun handle(httpExchange: HttpExchange) {
        try {
            val query = httpExchange.requestURI.rawQuery
            if (query == null) {
                val json = StringBuilder("{\"petData\": [")
                WebServer.petService.dataMap.forEach(BiConsumer { key: String?, (_, avatar1): KeyData ->
                    json.append("{\"key\": \"").append(key).append("\", ").append("\"types\": [")
                    avatar1.forEach(Consumer { (type): AvatarData ->  //necessary data
                        json.append("\"").append(type).append("\", ")
                    })
                    if (!avatar1.isEmpty()) json.delete(json.length - 2, json.length)
                    json.append("]}, ")
                })
                if (!WebServer.petService.dataMap.isEmpty()) json.delete(json.length - 2, json.length)
                json.append("]}")
                handleResponse(httpExchange, json.toString())
                return
            }
            val parser = CommandParser(query)
            handleResponse(httpExchange, parser.imagePair!!.first, parser.imagePair!!.second)
            parser.close()
        } catch (ignored: Exception) {
            handleResponse(httpExchange, 400)
        }
    }

    private fun handleResponse(httpExchange: HttpExchange, rCode: Int) {
        try {
            httpExchange.sendResponseHeaders(rCode, 0)
            val out = httpExchange.responseBody
            out.flush()
            out.close()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun handleResponse(httpExchange: HttpExchange, input: InputStream, type: String) {
        val imageBytes = input.readAllBytes()
        httpExchange.responseHeaders.add("Content-Type:", "image/$type")
        httpExchange.sendResponseHeaders(200, imageBytes.size.toLong())
        val out = httpExchange.responseBody
        out.write(imageBytes)
        out.flush()
        out.close()
    }

    @Throws(Exception::class)
    private fun handleResponse(httpExchange: HttpExchange, responseJson: String) {
        val responseContentByte = responseJson.toByteArray(StandardCharsets.UTF_8)
        httpExchange.responseHeaders.add("Content-Type:", "application/json;charset=utf-8")
        httpExchange.sendResponseHeaders(200, responseContentByte.size.toLong())
        val out = httpExchange.responseBody
        out.write(responseContentByte)
        out.flush()
        out.close()
    }
}
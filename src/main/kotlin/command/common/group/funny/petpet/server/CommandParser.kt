package command.common.group.funny.petpet.server

import command.common.group.funny.petpet.share.BaseConfigFactory
import command.common.group.funny.petpet.share.TextExtraData
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

class CommandParser(command: String) {
    private var parameterList: HashMap<String, String>? = HashMap()
    var imagePair: Pair<InputStream, String>? = null
        private set

    init {
        val queryList = command.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (query in queryList) {
            val parameter = query.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            parameterList!![parameter[0]] = URLDecoder.decode(parameter[1], StandardCharsets.UTF_8)
        }
        parser()
    }

    private fun parser() {
        val textList = if (get("textList") != null) Arrays.asList(
            *get("textList")!!.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()) else ArrayList()
        imagePair = WebServer.petService.generateImage(
            get("key"),
            BaseConfigFactory.getGifAvatarExtraDataFromUrls(
                get("fromAvatar"), get("toAvatar"), get("groupAvatar"), get("botAvatar")
            ), TextExtraData(
                (if (get("fromName") != null) get("fromName") else "from")!!,
                (if (get("toName") != null) get("toName") else "to")!!,
                (if (get("groupName") != null) get("groupName") else "group")!!,
                textList
            ), null
        )
    }

    operator fun get(key: String): String? {
        println("DEBUG: input: " + key + '=' + parameterList!![key])
        return parameterList!![key]
    }

    fun close() {
        parameterList = null
        imagePair = null
    }
}
package command.common.group.funny.petpet.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import command.common.group.funny.petpet.share.Encoder

@Serializable
data class ServerConfig(
    val port: Int = 2333,
    val dataPath: String = "data/xmmt.dituon.petpet",
    val threadPoolSize: Int = 10,
    val gifMaxSize: List<Int> = emptyList(),
    val gifEncoder: Encoder = Encoder.BUFFERED_STREAM,
    val headless: Boolean = true
) {
    companion object {
        @JvmStatic
        fun getConfig(str: String): ServerConfig {
            return Json.decodeFromString(str)
        }
    }
}
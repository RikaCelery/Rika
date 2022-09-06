package command.common.grass_cutter


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GrassCutterStat(
    @SerialName("retcode")
    var retcode: Int = 0, // 0
    @SerialName("status")
    var status: Status = Status()
) {
    @Serializable
    data class Status(
        @SerialName("maxPlayer")
        var maxPlayer: Int = 0, // -1
        @SerialName("playerCount")
        var playerCount: Int = 0, // 0
        @SerialName("version")
        var version: String = "" // 3.0.0
    )
}
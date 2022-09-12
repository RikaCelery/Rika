package command.common.group.funny.marry_member.model

@kotlinx.serialization.Serializable
data class MarryResult(
    val husband:Long,
    val wife: Long,
    val type: MarryType,
    val success:Boolean=true
) {
    enum class MarryType {
        Normal,XiaoSan,Single
    }
    fun contains(id:Long): Boolean {
        return husband==id||wife==id
    }
}

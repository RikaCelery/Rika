package command.common.what_anime


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

private val Double.format: String
    get() =
        "%02d:%02d".format(this.toInt().div(60),this.toInt().mod(60))


@Serializable
data class TraceMoeResponse(
    @SerialName("error")
    var error: String = "",
    @SerialName("frameCount")
    var frameCount: Int = 0, // 745506
    @SerialName("result")
    var results: List<Result> = listOf()
) {
    @Serializable
    data class Result(
        @SerialName("anilist")
        var anilist: Anilist = Anilist(),
        @SerialName("episode")
        var episode: JsonElement? = null, // null
        @SerialName("filename")
        var filename: String = "", // Nekopara - OVA (BD 1280x720 x264 AAC).mp4
        @SerialName("from")
        var from: Double = 0.0, // 97.75
        @SerialName("image")
        var image: String = "", // https://media.trace.moe/image/99939/Nekopara%20-%20OVA%20(BD%201280x720%20x264%20AAC).mp4.jpg?t=98.335&now=1653892514&token=xxxxxxxxxxxxxx
        @SerialName("similarity")
        var similarity: Double = 0.0, // 0.9440424588727485
        @SerialName("to")
        var to: Double = 0.0, // 98.92
        @SerialName("video")
        var video: String = "" // https://media.trace.moe/video/99939/Nekopara%20-%20OVA%20(BD%201280x720%20x264%20AAC).mp4?t=98.335&now=1653892514&token=xxxxxxxxxxxxxx
    ) {
        val formated: String
            get() =
                "<span><similarity>相似度${similarity.let { if (it<90) "%.2f%(较低)".format(it) else "%.2f%".format(it) }}</similarity>\n"+
                        "位置：第%s集的%s</span>".format(episode?.jsonPrimitive?.content?:"?",from.format)


        @Serializable
        data class Anilist(
            @SerialName("id")
            var id: Int = 0, // 99939
            @SerialName("idMal")
            var idMal: Int = 0, // 34658
            @SerialName("isAdult")
            var isAdult: Boolean = false, // false
            @SerialName("synonyms")
            var synonyms: List<String> = listOf(),
            @SerialName("title")
            var title: Title = Title()
        ) {
            val titleDisplay:String
                get() = buildString {
                    if (title.native.isNotBlank()) append("中文名: ${title.chinese}\n")
                    if (title.english.isNotBlank()) append("英文名: ${title.english}\n")
                    if (title.native.isNotBlank()) append("一般名字: ${title.native}\n")
                    if (title.romaji.isNotBlank()) append("罗马音: ${title.romaji}\n")
                    if (synonyms.isNotEmpty()) append(synonyms.joinToString("\n","别名:\n"))
                }.trim().ifBlank { "没有名字" }
            @Serializable
            data class Title(
                @SerialName("english")
                var english: String = "", // null
                @SerialName("native")
                var native: String = "", // ネコぱらOVA
                @SerialName("chinese")
                var chinese: String = "", //
                @SerialName("romaji")
                var romaji: String = "" // Nekopara OVA
            ){
            }
        }
    }
}
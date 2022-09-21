package command.common.tool.github.saucenao


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.text.StringEscapeUtils
import org.celery.utils.serialization.defaultJson

private val JsonElement.content: String
    get() = try {
        jsonArray.joinToString(", ") { it.jsonPrimitive.content }
    } catch (e:Exception) {
        e.printStackTrace()
        toString()
    }
private val Any.html: String
    get() = StringEscapeUtils.escapeHtml4(this.toString())


@Serializable
data class SauceNaoResponse(
    @SerialName("header")
    var header: Header = Header(),
    @SerialName("results")
    var results: List<Result> = listOf()
) {
    @Serializable
    data class Header(
        @SerialName("account_type")
        var accountType: String = "", // 1
        @SerialName("index")
        var index: Map<Int, MapValue> = mapOf(),
        @SerialName("long_limit")
        var longLimit: String = "", // 100
        @SerialName("long_remaining")
        var longRemaining: Int = 0, // 66
        @SerialName("minimum_similarity")
        var minimumSimilarity: Double = 0.0, // 16.119999999999997
        @SerialName("query_image")
        var queryImage: String = "", // CVeNczpEU.gif
        @SerialName("query_image_display")
        var queryImageDisplay: String = "", // userdata/CVeNczpEU.gif.png
        @SerialName("results_requested")
        var resultsRequested: Int = 0, // 16
        @SerialName("results_returned")
        var resultsReturned: Int = 0, // 16
        @SerialName("search_depth")
        var searchDepth: String = "", // 128
        @SerialName("short_limit")
        var shortLimit: String = "", // 4
        @SerialName("short_remaining")
        var shortRemaining: Int = 0, // 3
        @SerialName("status")
        var status: Int = 0, // 0
        @SerialName("user_id")
        var userId: String = "" // 63391
    ) {
        @Serializable
        data class MapValue(
            @SerialName("id")
            var id: Int = 0, // 0
            @SerialName("parent_id")
            var parentId: Int = 0, // 0
            @SerialName("results")
            var results: Int = 0, // 1
            @SerialName("status")
            var status: Int = 0 // 0
        )
    }

    @Serializable
    data class Result(
        @SerialName("data")
        var `data`: Data = Data(),
        @SerialName("header")
        var header: Header = Header()
    ) {
        val formated: String
            get() {
                val s = "<similarity>相似度${header.similarity.html}%</similarity>\n" + when (header.indexId) {
                    in arrayOf(5,51,52,53) -> """
                        <db-name>[Pixiv](插画)</db-name>
                        ${header.indexName}
                        PID:${data.pixivId}
                        标题:${data.title}
                        UID:${data.memberId}
                        作者:${data.memberName}
                        """.trimIndent()
                    6 -> """
                        <db-name>[Pixiv Historical](插画) (这可能是一个已经消失的作品)</db-name>
                        ${header.indexName}
                        PID:${data.pixivId}
                        标题:${data.title}
                        UID:${data.memberId}
                        作者:${data.memberName}
                        """.trimIndent()
                    9 -> """
                        <db-name>[Danbooru]</db-name>
                        ${header.indexName}
                        源:${data.source}
                        作者:${data.creator?.jsonPrimitive?.content}
                        UID:${data.memberId}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                        """.trimIndent()
                    10 -> """
                        <db-name>[Drawr Images]</db-name>
                        ${header.indexName}
                        标题:${data.title}
                        UID:${data.memberId}
                        作者:${data.memberName}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                        """.trimIndent()
                    12 -> """
                        <db-name>[Yande.re]</db-name>
                        ${header.indexName}
                        来源:${data.source}
                        作者:${data.creator}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                        """.trimIndent()
                    18 -> """
                        <db-name>[N-Hentai]</db-name> 
                        ${header.indexName}
                        标题:${data.source}
                        标题-EN:${data.jpName}
                        标题-JP:${data.engName}
                        作者:${data.creator?.content}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                    """.trimIndent()
                    21 -> """
                        <db-name>[Anime*](动漫)</db-name> 
                        ${header.indexName}
                        标题:${data.source}
                        第${data.part}集${data.estTime}
                        year:${data.year}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                    """.trimIndent()
                    22 -> """
                        <db-name>[***](动漫)</db-name> 
                        ${header.indexName}
                        标题:${data.source}
                        第${data.part}集${data.estTime}
                        year:${data.year}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                    """.trimIndent()
                    34 -> """
                        <db-name>[DeviantArt]</db-name> 
                        ${header.indexName}
                        ID:${data.daId}
                        标题:${data.title}
                        作者主页链接:${data.authorUrl}
                        作者:${data.authorName}
                    """.trimIndent()
                    36 -> """
                        <db-name>[Madokami (Manga)]</db-name> 
                        ${header.indexName}
                        标题:${data.source}
                        part:${data.part}
                        mu_id:${data.muId}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                    """.trimIndent()
                    37 -> """
                        <db-name>[MangaDex]</db-name> 
                        ${header.indexName}
                        标题:${data.source}
                        part:${data.part}
                        mu_id:${data.muId}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                    """.trimIndent()
                    38 -> """
                        <db-name>[E-Hentai]</db-name> 
                        ${header.indexName}
                        标题:${data.source}
                        标题-EN:${data.jpName}
                        标题-JP:${data.engName}
                        作者:${data.creator?.content }
                    """.trimIndent()
                    41 -> """
                        <db-name>[Twitter]</db-name> 
                        ${header.indexName}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                    """.trimIndent()
                    42 -> """
                        <db-name>[Furry Network]</db-name> 
                        ${header.indexName}
                        标题:${data.title}
                        作者:${data.authorName}
                        作者Url:${data.authorUrl}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                    """.trimIndent()
                    43 -> """
                        <db-name>[Patreon]</db-name>
                        ${header.indexName}
                        ID:${data.id}
                        标题:${data.title}
                        UID:${data.userId}
                        作者:${data.userName}
                        """.trimIndent()
                    371 -> """
                        <db-name>[MangaDex2]</db-name>
                        ${header.indexName}
                        标题:${data.source}
                        part:${data.part}
                        mu_id:${data.muId}
                        malId:${data.malId}
                        ext_urls:${data.extUrls?.joinToString("\n", "\n")}
                        """.trimIndent()
                    else -> defaultJson.encodeToString(serializer(), this).also { println("\n" + it + "\n") }
                }
                return "<span>$s</span>"
            }

        @Serializable
        data class Data(
            @SerialName("anidb_aid")
            var anidbAid: Int? = null, // 69
            @SerialName("anilist_id")
            var anilistId: Int? = null, // 21
            @SerialName("as_project")
            var asProject: String? = null, // 8eygBE
            @SerialName("author_name")
            var authorName: String? = null, // MaxwellFury
            @SerialName("author_url")
            var authorUrl: String? = null, // https://skeb.jp/@calicchio23
            @SerialName("company")
            var company: String? = null, // Selen
            @SerialName("creator")
            var creator: JsonElement? = null,
            @SerialName("creator_name")
            var creatorName: String? = null, // 笹倉ささくら🔞Skeb募集中
            @SerialName("da_id")
            var daId: String? = null, // 874130995
            @SerialName("eng_name")
            var engName: String? = null, // [VipCaptions] The Costume 2
            @SerialName("est_time")
            var estTime: String? = null, // 00:12:59 / 00:23:20
            @SerialName("ext_urls")
            var extUrls: List<String>? = null,
            @SerialName("fa_id")
            var faId: Int? = null, // 3507676
            @SerialName("getchu_id")
            var getchuId: String? = null, // 587705
            @SerialName("id")
            var id: String? = null, // 48855170
            @SerialName("imdb_id")
            var imdbId: String? = null, // tt1121931
            @SerialName("jp_name")
            var jpName: String? = null,
            @SerialName("mal_id")
            var malId: Int? = null, // 21
            @SerialName("member_id")
            var memberId: Int? = null, // 7996040
            @SerialName("member_name")
            var memberName: String? = null, // 立浪
            @SerialName("mu_id")
            var muId: Int? = null, // 4451
            @SerialName("part")
            var part: String? = null, // 299
            @SerialName("path")
            var path: String? = null, // /@calicchio23/works/79
            @SerialName("pixiv_id")
            var pixivId: Int? = null, // 51318798
            @SerialName("published")
            var published: String? = null, // 2021-03-17T11:11:49.000Z
            @SerialName("service")
            var service: String? = null, // patreon
            @SerialName("service_name")
            var serviceName: String? = null, // Patreon
            @SerialName("source")
            var source: String? = null, // The Costume 2
            @SerialName("title")
            var title: String? = null, // 血界まとめ２
            @SerialName("type")
            var type: String? = null, // Manga
            @SerialName("user_id")
            var userId: String? = null, // 14792976
            @SerialName("user_name")
            var userName: String? = null, // Etiennet
            @SerialName("year")
            var year: String? = null // 1999
        )

        @Serializable
        data class Header(
            @SerialName("dupes")
            var dupes: Int = 0, // 0
            @SerialName("hidden")
            var hidden: Int = 0, // 0
            @SerialName("index_id")
            var indexId: Int = 0, // 5
            @SerialName("index_name")
            var indexName: String = "", // Index #5: Pixiv Images - 51318798_p3.jpg
            @SerialName("similarity")
            var similarity: Float = 0f, // 15.12
            @SerialName("thumbnail")
            var thumbnail: String = "" // https://img1.saucenao.com/res/pixiv/5131/manga/51318798_p3.jpg?auth=3SEn6iuSont5-aYrn53h1w&exp=1663095600
        )
    }
}
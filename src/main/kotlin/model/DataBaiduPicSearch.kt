package model


import org.celery.utils.http.HttpUtils.downloader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource

@Serializable
data class DataBaiduPicSearch(
    @SerialName("bdFmtDispNum")
    var displayNum: String = "", // 约72,800
    @SerialName("data")
    var `data`: List<DataData> = listOf(),
    @SerialName("displayNum")
    var totalNum: Int = 0, // 总大小 72800
    @SerialName("gsm")
    var hexEndIndex: String = "", // 最后一个元素的hex索引值
    @SerialName("queryExt")
    var queryWord: String = "" // 万叶
) {
    @Serializable
    data class DataData(
        @SerialName("bdImgnewsDate")
        var bdImgnewsDate: String? = null, // 2021-07-12 00:00
        @SerialName("fromPageTitle")
        var fromPageTitle: String = "", // 原神<strong>万叶</strong>手书制作幕后及原画公开
        @SerialName("fromPageTitleEnc")
        var fromPageTitleEnc: String = "", // 原神万叶手书制作幕后及原画公开
        @SerialName("fromURLHost")
        var fromURLHost: String? = null, // www.bilibili.com
        @SerialName("height")
        var height: Int = -1, // 2160
        @SerialName("width")
        var width: Int = -1, // 4096
        @SerialName("bdSetImgNum")
        var setSize: Int = -1, // 4096
        @SerialName("pageNum")
        var pageNum: Int = -1, // 在总结果中的页码
        @SerialName("thumbURL")
        var thumbURL: String = "",
        @SerialName("replaceUrl")
        var replaceUrl: List<DataReplaceUrl> = listOf(),
        @SerialName("DataSet")
        var imageSet: List<DataSet> = listOf()
    ) {
        fun getOriLink(): String? {
            return replaceUrl.firstOrNull()?.objUrl
        }

        suspend fun getImageMessage(sender: Member, group: Group): Message {
            if (setSize > 0) {
                val builder = ForwardMessageBuilder(group)
                builder.add(sender, PlainText(getInfo()))
                imageSet.forEach {
                    builder.add(sender, it.getMessage(group))
                }
                return builder.build()
            }
            if (replaceUrl.isEmpty()) {
                println(this@DataData)
            }
            val image = try {
                downloader(replaceUrl.first().objUrl).inputStream().toExternalResource().use { resource ->
                    (group.uploadImage(resource))
                }

            } catch (e: Exception) {
                println("原图上传失败,使用缩略图")
                this.thumbURL.let {
                    downloader(it).inputStream().toExternalResource().use { resource ->
                        (group.uploadImage(resource))
                    }
                }
            }
            return image
        }

        fun getInfo(): String {
            return "${fromPageTitleEnc.ifEmpty { fromPageTitle.replace(Regex("</?\\w+>"), "") }}\n" +
                    replaceUrl.first().fromUrl
        }

        @Serializable
        data class DataReplaceUrl(
            @SerialName("FromUrl")
            var fromUrl: String = "", // http://www.bilibili.com/read/cv12119312/
            @SerialName("ObjUrl")
            var objUrl: String = "" // https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fi0.hdslb.com%2Fbfs%2Farticle%2F3f7e81d9eb65b60622e6faa577742adc3d94c4b0.jpg&refer=http%3A%2F%2Fi0.hdslb.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=auto?sec=1657795771&t=8096d77a92e6ce292e751618b3694f37
        )

        @Serializable
        data class DataSet(
            @SerialName("bdImgnewsDate")
            var bdImgnewsDate: String = "", // 2021-07-19 15:19
            @SerialName("fromPageTitle")
            var fromPageTitle: String = "", // 枫原<strong>万叶</strong>yyds_原神_游戏_手机游戏
            @SerialName("fromPageTitleEnc")
            var fromPageTitleEnc: String = "",
            @SerialName("spn")
            var index: Int = -1, // 在set结果中的页码
            @SerialName("objURL")
            var objURL: String = "", // http://ci.xiaohongshu.com/005b671f-6a44-a55f-ba59-7f66aad10f39?imageView2/2/w/1080/format/jpg
            @SerialName("fromURL")
            var fromURL: String = "", // http://www.xiaohongshu.com/discovery/item/60f52789000000002103490e
            @SerialName("height")
            var height: Int = 0, // 1080
            @SerialName("width")
            var width: Int = 0 // 1080
        ) {
            suspend fun getMessage(subject: Contact): Message {
                val s = try {
                    downloader(objURL).inputStream().toExternalResource().use { resource ->
                        subject.uploadImage(resource)
                    }
                } catch (e: Exception) {
                    PlainText(e.toString())
                }
                return s
            }
        }
    }
}
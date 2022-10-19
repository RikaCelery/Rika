package command.common.tool.saucenao

import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.nextMessageOrNull
import org.celery.command.controller.CommandExecutor
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.onLocked
import org.celery.command.controller.abs.throwOnFailure
import org.celery.command.controller.abs.withlock
import org.celery.utils.contact.simpleStr
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.selenium.styles.SauceNaoStyle
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import org.celery.utils.toImage
import java.net.URLEncoder

private const val SAUCENAO_INIT_KEY = "请填写saucenao api key"


object SauceNaoPicSearch : Command(
    "识图",5, "实用工具",
    "saucenao以图搜图",
    "识图[图片]",
    "maxresult: 一次性最多返回多少个仓库信息\nshow_link: 发送仓库全名",
    ""
) {
    @Command("^识图|搜图$|识图$|^搜图\\s*[图片]$|^识图\\s*[图片]$")
    suspend fun MessageEvent.handle():ExecutionResult{
        withlock(subject.id,0){
        process()
        }.onLocked {
            sendMessage("急你妈")
            return ExecutionResult.LimitCall
        }.throwOnFailure()
        return ExecutionResult.Success
    }

    private suspend fun MessageEvent.process(): ExecutionResult {
        val key = config["key", SAUCENAO_INIT_KEY]
        if (key == SAUCENAO_INIT_KEY) {
            logger.warning("未配置api key,请到pluginConfig.json中配置")
            return ExecutionResult.Ignored("未配置key")
        }
        val resultSize = config["results", 5]
        val showAdult = config.getOrNull("${subject.simpleStr}.${sender.id}.showAdult") ?: config["${subject.simpleStr}.showAdult", false]
        val showThumb = config.getOrNull("${subject.simpleStr}.${sender.id}.showThumb") ?: config["${subject.simpleStr}.showThumb", false]


        var imageMessage = message.findIsInstance<Image>() ?: CommandExecutor.lastInstanceOrNull<Image>(this)?: kotlin.run {
            if (message[QuoteReply.Key]!=null){
                message[QuoteReply.Key]!!.source.originalMessage.findIsInstance<Image>()
            }
            else
                null
        }
        if (imageMessage == null) {
            sendMessage(At(sender) + PlainText(config["require_picture_message","图呢图呢图呢？赶紧的赶紧的"]))
            imageMessage = nextMessageOrNull(60 * 1000) {
                println(it.message.findIsInstance<Image>())
                it.message.findIsInstance<Image>() != null
            }?.findIsInstance<Image>()
            if (imageMessage == null) {
                sendMessage(config["no_message_received","图都不发你用jb的搜图😅😅😅"])
                return ExecutionResult.LimitCall
            }
        }
        sendMessage(At(sender) + PlainText(config["searching","好了！我已经在搜图了！"]))
        //normal

        val url =
            "https://saucenao.com/search.php?db=999&output_type=2&numres=%d&url=%s&api_key=%s".let { if (!showAdult) "$it&hide=2" else it }
                .format(resultSize, withContext(Dispatchers.IO) {
                    URLEncoder.encode(imageMessage.queryUrl(), "utf8")
                }, key)
        val string = String(HttpUtils.downloader(url))
        val results = defaultJson.decodeFromString(SauceNaoResponse.serializer(), string)
        buildMessageChain {
            +At(sender)
            +PlainText(config["get_result","你的搜图结果！！"])
            if (results.results.maxOf { it.header.similarity } <= 50) {
                +PlainText(config["low_similarity","\n我草！这是什么jb相似度😅八成是没搜到"])
            }
            +SharedSelenium.render(
                buildString {
                    results.results.sortedByDescending { it.header.similarity }.let { list ->
                        if (list.any { it1 -> it1.header.similarity >= 85 }) list.filter { it.header.similarity >= 85 }
                        else list
                    }.forEachIndexed { _, result ->
                        append("<div>")
                        append(result.formated.replace("\n", "<br>"))
                        append("\n")
                        if (showThumb&&result.header.hidden!=1&&result.header.hidden!=2||showAdult) append(
                            "<p><img src=\"${result.header.thumbnail}\"></p>"
                        ) else if (showThumb)
                            append(
                                "<p>${config["diable_r18_message","R18显示被关闭"]}</p>"
                            ) else append(
                            "<p>${config["diable_thumb_message","缩略图显示已被关闭"]}</p>"
                        )
                        append("</div>")
                    }
                }.trimIndent(), SauceNaoStyle
            ).toImage(subject)

        }.sendTo(subject)
        return ExecutionResult.Success

    }
}
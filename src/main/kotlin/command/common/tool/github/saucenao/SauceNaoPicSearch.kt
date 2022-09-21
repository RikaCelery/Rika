package org.celery.command.common.saucenao

import command.common.tool.github.saucenao.SauceNaoResponse
import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.nextMessageOrNull
import org.celery.command.controller.CommandExecutor
import org.celery.command.controller.RegexCommand
import org.celery.command.controller.getConfig
import org.celery.command.controller.getConfigOrNull
import org.celery.utils.contact.simpleStr
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.selenium.styles.SauceNaoStyle
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import org.celery.utils.toImage
import java.net.URLEncoder

private const val SAUCENAO_INIT_KEY = "请填写saucenao api key"

object SauceNaoPicSearch : RegexCommand(
    "识图", "^识图".toRegex(), 5, "识图[图片]", secondaryRegexs = arrayOf(Regex("^以图搜图"))
) {
    init{
        defaultCountLimit = 8
        showTip = true
    }
    override var blockSubjectAction: suspend MessageEvent.() -> Any = {
        sendMessage(At(sender) + PlainText("你别急！！！，有人用着呢！！！"))
        delay(1000)
    }

    @Command
    suspend fun MessageEvent.handle(): ExecutionResult {
        val key = getConfig("key", SAUCENAO_INIT_KEY)
        if (key == SAUCENAO_INIT_KEY) {
            logger.warning("未配置api key,请到pluginConfig.json中配置")
            return ExecutionResult.Ignored("未配置key")
        }
        val resultSize = getConfig("results", 5)
        val showAdult = getConfigOrNull("${subject.simpleStr}.${sender.id}.showAdult") ?: getConfig(
            "${subject.simpleStr}.showAdult", false
        )
        val showThumb = getConfigOrNull("${subject.simpleStr}.${sender.id}.showThumb") ?: getConfig(
            "${subject.simpleStr}.showThumb", false
        )

        var imageMessage = message.findIsInstance<Image>() ?: CommandExecutor.lastInstanceOrNull<Image>(this)
        if (imageMessage == null) {
            sendMessage(At(sender) + PlainText("图呢图呢图呢？赶紧的赶紧的"))
            imageMessage = nextMessageOrNull(60 * 1000) {
                println(it.message.findIsInstance<Image>())
                it.message.findIsInstance<Image>() != null
            }?.findIsInstance<Image>()
            if (imageMessage == null) {
                sendMessage("图都不发你用jb的搜图😅😅😅")
                return ExecutionResult.LimitCall
            }
        }
        sendMessage(At(sender) + PlainText("好了！我已经在搜图了！"))
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
            +PlainText("你的搜图结果！！")
            if (results.results.maxOf { it.header.similarity } <= 50) {
                +PlainText("\n我草！这是什么jb相似度😅八成是没搜到")
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
                                "<p>R18显示被关闭</p>"
                            ) else append(
                            "<p>缩略图显示已被关闭</p>"
                        )
                        append("</div>")
                    }
                }.trimIndent(), SauceNaoStyle
            ).toImage(subject)

        }.sendTo(subject)
        return ExecutionResult.Success

    }
}
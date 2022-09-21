package command.common.tool.github.what_anime

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


private const val API_TRACE_MOE = "https://api.trace.moe"

object WhatAnime : RegexCommand(
    "识番", "^识番".toRegex(), 5, "识番[图片]", secondaryRegexs = arrayOf(Regex("^以图搜番"),Regex("^这是什么番"),Regex("^搜动漫"),Regex("^这是什么动漫"))
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
        var api = getConfig("api", API_TRACE_MOE)
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
                sendMessage("图都不发你让我用啥的搜番？")
                return ExecutionResult.LimitCall
            }
        }
        sendMessage(At(sender) + PlainText("好了！我已经在搜了！"))
        //normal
        api = api+"/search?anilistInfo&cutBorders&url=" +withContext(Dispatchers.IO) {
            URLEncoder.encode(imageMessage.queryUrl(), "utf8")
        }
//        println(api)
        val string = HttpUtils.getStringContent(api)
        val traceMoeResponse = defaultJson.decodeFromString(TraceMoeResponse.serializer(), string)
        if (traceMoeResponse.results.isEmpty()){
            sendMessage("你妈，我没查到！")
            return ExecutionResult.LimitCall
        }
        buildMessageChain {
            +At(sender)
            +PlainText("你的搜图结果！！")
            if (traceMoeResponse.results.maxOf { it.similarity } <= 50) {
                +PlainText("\n我草！这是什么jb相似度😅八成是没搜到")
            }
            +SharedSelenium.render(
                buildString {
                    var index = 0
                    traceMoeResponse.results.sortedByDescending { it.similarity }.let { list ->
                        if (list.any { it1 -> it1.similarity >= 85 }) list.filter { it.similarity >= 85 }
                        else list
                    }.forEachIndexed { _, result ->
                        if (index++>resultSize)
                            return@forEachIndexed
                        append("<div>")
                        append(result.formated.replace("\n", "<br>"))
                        append("\n")
                        if (showThumb&&!result.anilist.isAdult||showAdult) append(
                            """ <p>
                                    <img src="${result.image}" alt="">
                                </p>
                        """.trimIndent()
                        ) else append(
                            """<p>
                                   缩略图显示被关闭或R18显示被关闭
                               </p>
                            """.trimIndent()
                        )
                        append("</div>")
                    }
                }.trimIndent(), SauceNaoStyle
            ).toImage(subject)

        }.sendTo(subject)
        return ExecutionResult.Success

    }
}
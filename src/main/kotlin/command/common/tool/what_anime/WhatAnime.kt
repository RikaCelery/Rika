package command.common.tool.what_anime

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


private const val API_TRACE_MOE = "https://api.trace.moe"

object WhatAnime: Command(
    "识番",
    usage = "识番[图片]"
){

    @Command("^识番|搜番$|识番$|^搜番\\s*[图片]$|^识番\\s*[图片]$")
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
        var api = config.get("api", API_TRACE_MOE)
        val resultSize = config["results", 5]
        val showAdult = config.getOrNull("${subject.simpleStr}.${sender.id}.showAdult") ?: config["${subject.simpleStr}.showAdult", false]
        val showThumb = config.getOrNull("${subject.simpleStr}.${sender.id}.showThumb") ?: config["${subject.simpleStr}.showThumb", false]

        var imageMessage = message.findIsInstance<Image>() ?: CommandExecutor.lastInstanceOrNull<Image>(this)?: run {
            if (message[QuoteReply]!=null){
                message[QuoteReply]!!.source.originalMessage.toMessageChain().findIsInstance<Image>()?.also {
                    println("find${it.imageId}")
                }
            } else
                null
        }
        if (imageMessage == null) {
            sendMessage(At(sender) + PlainText("图呢图呢图呢？赶紧的赶紧的"))
            imageMessage = nextMessageOrNull(60 * 1000) {
                println(it.message.findIsInstance<Image>())
                it.message.findIsInstance<Image>() != null
            }?.findIsInstance<Image>()
            if (imageMessage == null) {
                sendMessage("图都不发你让我用啥搜番？")
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
//        println(string)
        val traceMoeResponse = defaultJson.decodeFromString(TraceMoeResponse.serializer(), string)
        if (traceMoeResponse.results.isEmpty()){
            sendMessage("你妈，我没查到！")
            return ExecutionResult.LimitCall
        }
        buildMessageChain {
            +At(sender)
            +PlainText("你的搜图结果！！")
            if (traceMoeResponse.results.maxOf { it.similarity } <= 0.50) {
                +PlainText("\n我草！这是什么jb相似度😅八成是没搜到")
            }
            +SharedSelenium.render(
                buildString {
                    traceMoeResponse.results.sortedByDescending { it.similarity }.let { list ->
                        if (list.any { it1 -> it1.similarity >= 0.85 }) list.filter { it.similarity >= 0.85 }
                        else list
                    }.forEachIndexed { index, result ->
                        if (index>resultSize)
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
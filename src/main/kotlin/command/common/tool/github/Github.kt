package org.celery.command.common.github

import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.command.controller.getConfig
import org.celery.utils.http.HttpUtils
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import org.celery.utils.toImage
import java.net.URLEncoder

object Github: RegexCommand(
    "github", "^github\\s+(.+)".toRegex(),
    5,
    "github  <仓库名称>",
    configInfo = "maxresult: 一次性最多返回多少个仓库信息\nshow_link: 发送仓库全名"

) {
    private const val githubAPI = "https://api.github.com/search/repositories"
    init {

    }
    @Command
    suspend fun MessageEvent.handle1(eventMatchResult: EventMatchResult): ExecutionResult {
        val array = getResult(eventMatchResult)

        if (array==null){
            sendMessage("我草，我也不知道")
            return ExecutionResult.LimitCall
        }
        sendMessage(renderImages(array))
        return ExecutionResult.Success
    }

    private suspend fun MessageEvent.renderImages(array: JsonArray): MessageChain {
        return buildMessageChain {
            array.forEachIndexed { index, element ->
                +HttpUtils.downloader("https://opengraph.githubassets.com/0/" + element.jsonObject["full_name"]!!.jsonPrimitive.content).toImage(subject)
                if (getConfig("show_link", false))
                +PlainText(element.jsonObject["full_name"]!!.jsonPrimitive.content)
            }
        }
    }

    private suspend fun getResult(eventMatchResult: EventMatchResult): JsonArray? {
        val content = HttpUtils.getStringContent(
            "%s?q=%s&per_page=%d".format(
                githubAPI,
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(eventMatchResult[1], "utf8")
                },
                getConfig("maxresult", 1)
                ),true
        )
        val jsonElement = defaultJson.parseToJsonElement(content).jsonObject
        val array =
            if (jsonElement["total_count"]!!.jsonPrimitive.int != 0) jsonElement.jsonObject["items"]!!.jsonArray else null
        return array
    }
}
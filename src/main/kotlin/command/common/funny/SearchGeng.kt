package org.celery.command.common.funny

import events.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import org.celery.utils.toImage
import java.net.URLEncoder

object SearchGeng:RegexCommand(
    "搜梗", "^搜梗\\s*(.+)".toRegex(),
    5,
    "搜梗<梗名>",
    configInfo = "max：最大渲染数量"

) {
    private const val gzsAPI = "https://api.iyk0.com/gzs"
    override var defaultCountLimit: Int = 20
    override var defaultCoolDown: Long = 2400
    override var defaultCallCountLimitMode: BlockRunMode = BlockRunMode.PureUser
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
                if (index > getConfig(Int.serializer(), "max", 3))
                    return@forEachIndexed
                +SharedSelenium.render(element.jsonObject["title"]!!.jsonPrimitive.content).toImage(subject)
            }
        }
    }

    private suspend fun getResult(eventMatchResult: EventMatchResult): JsonArray? {
        val content = HttpUtils.getStringContent(
            "%s?msg=%s".format(gzsAPI,
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(eventMatchResult[1], "utf8")
                })
        )
        val jsonElement = defaultJson.parseToJsonElement(content).jsonObject
        val array =
            if (jsonElement["data"] == null || jsonElement["data"]!!.jsonArray.isEmpty()) jsonElement.jsonObject["data"]!!.jsonArray else null
        return array
    }
}
package org.celery.command.common.funny

import events.ExecutionResult
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.command.controller.abs.Command
import org.celery.utils.http.HttpUtils
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.serialization.defaultJson
import org.celery.utils.toImage

object NetEase:Command(
    "网易云评论"
) {
    private const val repingURL = "https://api.vvhan.com/api/reping"
    @Command("网易云评论")
    suspend fun MessageEvent.handle(): ExecutionResult {
        val jsonElement = defaultJson.parseToJsonElement(HttpUtils.getStringContent(repingURL))
        if (jsonElement.jsonObject["success"]?.jsonPrimitive?.booleanOrNull!=true)
            return ExecutionResult.Failed()
        sendMessage(SharedSelenium.render(jsonElement.jsonObject["data"]!!.jsonObject["content"]!!.jsonPrimitive.content).toImage(subject))
        return ExecutionResult.Success
    }
}
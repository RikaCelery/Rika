package command.common.group.funny

import events.ExecutionResult
import kotlinx.serialization.json.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.content
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.config.main.function.CountLimit
import org.celery.data.TempData
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage

object TalkWithMe : Command(
    "聊sao"
) {
    init {
        CountLimit.getOrNull<Int>("$commandId.mode") ?: kotlin.run {
            setLimitMode(1, 1, 1, false)
        }
    }

    private val replyMap: JsonObject?
        get() = try {
            val json = Json.parseToJsonElement(getOrCreateDataFile("replyData.json").readText())
            json.jsonObject
        } catch (e: Exception) {
            logger.error("replyMap init failed", e)
            null
        }


    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val index = TempData["$commandId.index.${group.simpleStr}", 0]
        if (index + 1 > maxCount) {
            sendMessage(limitMessage)
            return ExecutionResult.LimitCall
        }
        TempData["$commandId.index.${group.simpleStr}"] = index + 1
        eventMatchResult.getdata<JsonArray>().randomOrNull()?.let {
            group.sendMessage(it.jsonPrimitive.content.deserializeMiraiCode())
        }
        return ExecutionResult.Success
    }

    @Trigger("handle")
    fun GroupMessageEvent.match(): EventMatchResult? {
        if (!message.content.contains(Regex("^rika"))) return null
        val messageContent = message.content.replace(Regex("^rika"), "").trim().lowercase()
        if (config["blackwords", listOf("性交")].any { messageContent.contains(it) })
            return null
        val reply = replyMap?.filter { messageContent.contains(it.key) }
            ?.ifEmpty { null }?.values?.random()?.jsonArray
        reply?.let { logger.debug("获取${message.content}的回复: $reply") }
        return reply?.let { EventMatchResult(null, 0, it) }
    }

    private val limitMessage: String
        get() = config["after_max_count", "行了行了一个个的都tmd收收味球球了"]
    private val maxCount: Int
        get() = config["max_count", 10]

}
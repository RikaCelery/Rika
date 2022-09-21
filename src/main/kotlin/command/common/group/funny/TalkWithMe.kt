package command.common.group.funny

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContact
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.content
import org.celery.command.controller.BlockRunMode
import org.celery.command.controller.Call
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.exceptions.CommandAbortException

object TalkWithMe : RegexCommand(
    "聊sao", "\\w{2,20}".toRegex()
) {
    private val replyMap: JsonObject?
        get() =
            try {
                val json = Json.parseToJsonElement(getOrCreateDataFile("replyData.json").readText())
                json.jsonObject
            } catch (e: Exception) {
                logger.error("replyMap init failed", e)
                null
            }

    init {
        defaultCoolDown = 2000
        defaultCountLimit = 10
        defaultEnable = false
        defaultCallCountLimitMode = BlockRunMode.Subject
        defaultBlockRunModeMode = BlockRunMode.Subject
    }
//
//    override var defaultCoolDown: Long = 2000
//    override var defaultCountLimit: Int = 10
//    override var defaultEnable: Boolean = false
//    override var defaultCallCountLimitMode: BlockRunMode = BlockRunMode.Subject
//    override var defaultBlockRunModeMode: BlockRunMode = BlockRunMode.Subject

    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult) {
        eventMatchResult.getdata<JsonArray>().randomOrNull()?.let {
            runBlocking {
                group.sendMessage(it.jsonPrimitive.content.deserializeMiraiCode())
            }
        }
    }

    @Matcher
    override suspend fun MessageEvent.match(): EventMatchResult? {
        if (!message.content.contains(Regex("^rika")))
            return null

        val messageContent = message.content.replace(Regex("^rika"), "")
        val reply = replyMap?.filter { messageContent.trim().lowercase().contains(it.key) }
            ?.ifEmpty { null }?.values?.random()?.jsonArray
        reply?.let { logger.debug("获取${this.message.content}的回复: $reply") }
        return reply?.let { EventMatchResult(null, 0, it) }
    }

    override val limitMessage: String
        get() = "行了行了一个个的都tmd收收味球球了"

    @OptIn(ConsoleExperimentalApi::class)
    override suspend fun Bot.limitNotice(call: Call, finalLimit: Int) {
        getContact(call.subjectId!!, false).sendMessage(PlainText(limitMessage))
        addCall(call)
        throw CommandAbortException()
    }
}
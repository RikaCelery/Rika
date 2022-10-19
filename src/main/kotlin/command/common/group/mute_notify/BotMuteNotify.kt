package org.celery.command.common.group.mute_notify

import kotlinx.coroutines.*
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.ConcurrencyKind
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.BotMuteEvent
import net.mamoe.mirai.event.events.BotUnmuteEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.simpleStr
import org.celery.utils.strings.placeholder

object BotMuteNotify : Command(
    "Bot禁言提醒"
) {
    private val defaultMessage: String
        get() = config["default", "[mirai:at:{operator_id}]{time}秒是吧？"]
    private val defaultMessage2: String
        get() = config["default_unmute_by_other_operator", "[mirai:at:{cancel_operator_id}]谢谢奥，[mirai:at:{operator_id}]就他妈你禁言我{time}秒是吧？"]

    init {
        config["variables", "member_id," +
                "member_name_card," +
                "member_id," +
                "member_name_card_or_nick," +
                "operator_id," +
                "operator_name_card," +
                "operator_name_card_or_nick," +
                "cancel_operator_id," +
                "cancel_operator_name_card," +
                "cancel_operator_name_card_or_nick," +
                "time"
        ]
    }

    @Command
    suspend fun BotMuteEvent.process(eventMatchResult: EventMatchResult) {
        val map = eventMatchResult.getdata<Map<String, String>>()
        val time = map["time"]!!.toInt()
        val autoreply = map["message"]!!
        val scope = CoroutineScope(SupervisorJob())
        val listener = bot.eventChannel.parentScope(scope).subscribe<BotUnmuteEvent>(concurrency = ConcurrencyKind.LOCKED) {
            if (it.group == this@process.group) {
                if (it.operator == this@process.operator) {
                    delay(200)
                    group.sendMessage(config["unmute_by_same_operator", "？玩儿呢"].replaced(this, time))
                    return@subscribe ListeningStatus.STOPPED
                }
                delay(200)
                group.sendMessage(config["unmute_by_other_operator", defaultMessage2].replaced(this@process,this, time))
                return@subscribe ListeningStatus.STOPPED
            } else return@subscribe ListeningStatus.LISTENING
        }
        scope.launch {
            delay(time.toLong()*1000)
            if (listener.isCompleted.not())
                listener.cancel()
        }
        listener.join()
        if (listener.isCancelled){
            group.sendMessage(autoreply.deserializeMiraiCode())
        }
        scope.cancel()

    }

    @Trigger("process")
    fun BotMuteEvent.handle(): EventMatchResult {
        return EventMatchResult(
            data = mapOf(
                "message" to config[group.simpleStr, defaultMessage].replaced(this).serializeToMiraiCode(),
                "time" to durationSeconds.toString()
            )
        )
    }
}

private fun String.replaced(botMuteEvent: BotMuteEvent) = this.placeholder(
    "member_id" to botMuteEvent.bot.id.toString(),
    "member_name_card_or_nick" to botMuteEvent.bot.nameCardOrNick,
    "operator_id" to botMuteEvent.operator.id.toString(),
    "operator_name_card" to botMuteEvent.operator.nameCard,
    "operator_name_card_or_nick" to botMuteEvent.operator.nameCardOrNick,
    "time" to botMuteEvent.durationSeconds.toString(),
).deserializeMiraiCode(botMuteEvent.group)

private fun String.replaced(botUnmuteEvent: BotUnmuteEvent, time: Int) = this.placeholder(
    "member_id" to botUnmuteEvent.bot.id.toString(),
    "member_name_card_or_nick" to botUnmuteEvent.bot.nameCardOrNick,
    "cancel_operator_id" to botUnmuteEvent.operator.id.toString(),
    "cancel_operator_name_card" to botUnmuteEvent.operator.nameCard,
    "cancel_operator_name_card_or_nick" to botUnmuteEvent.operator.nameCardOrNick,
    "time" to time
).deserializeMiraiCode(botUnmuteEvent.group)

private fun String.replaced(botMuteEvent: BotMuteEvent,botUnmuteEvent: BotUnmuteEvent, time: Int) = this.placeholder(
    "member_id" to botUnmuteEvent.bot.id.toString(),
    "member_name_card_or_nick" to botUnmuteEvent.bot.nameCardOrNick,
    "operator_id" to botMuteEvent.operator.id.toString(),
    "operator_name_card" to botMuteEvent.operator.nameCard,
    "operator_name_card_or_nick" to botMuteEvent.operator.nameCardOrNick,
    "cancel_operator_id" to botUnmuteEvent.operator.id.toString(),
    "cancel_operator_name_card" to botUnmuteEvent.operator.nameCard,
    "cancel_operator_name_card_or_nick" to botUnmuteEvent.operator.nameCardOrNick,
    "time" to time
).deserializeMiraiCode(botUnmuteEvent.group)
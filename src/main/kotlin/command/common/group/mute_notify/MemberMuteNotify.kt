package org.celery.command.common.group.mute_notify

import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MemberMuteEvent
import net.mamoe.mirai.event.events.isByBot
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.simpleStr
import org.celery.utils.number.randomInt
import org.celery.utils.sendMessage
import org.celery.utils.strings.placeholder

object MemberMuteNotify : Command(
    "禁言提醒"
) {
    private const val defaultMessage = "可怜的{member_name_card_or_nick}被{operator_name_card_or_nick}禁言了{time}秒"
    @Command
    suspend fun MemberMuteEvent.process(eventMatchResult: EventMatchResult) {
        val (messages, count) = eventMatchResult.getdata<Pair<List<String>, Int>>()
        for (message in messages.take(count).shuffled()) {
            sendMessage(message.replaced(this))
            delay(randomInt(1000,2000).toLong())
        }
    }

    @Trigger("process")
    fun MemberMuteEvent.handle(): EventMatchResult? {
        if (isByBot) return null
        if (member.id == bot.id) return null
        if (!data["triggerUser", listOf<Long>()].contains(member.id)) return null
        val get =
            data[member.simpleStr, listOf(data["default", defaultMessage])]
        val count = data["${member.simpleStr}.count", data["default.count", 1]]
        return if (get.isNotEmpty()) EventMatchResult(data = get to count)
        else null
    }
}

private fun String.replaced(memberMuteEvent: MemberMuteEvent) = this.placeholder(
    "member_id" to memberMuteEvent.member.id.toString(),
    "member_name_card" to memberMuteEvent.member.nameCard,
    "member_name_card_or_nick" to memberMuteEvent.member.nameCardOrNick,
    "operator_id" to memberMuteEvent.operator!!.id.toString(),
    "operator_name_card" to memberMuteEvent.operator!!.nameCard,
    "operator_name_card_or_nick" to memberMuteEvent.operator!!.nameCardOrNick,
    "time" to memberMuteEvent.durationSeconds.toString(),
)


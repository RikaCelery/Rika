package command.common.group.exit_notify

import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MemberLeaveEvent
import org.celery.command.controller.EventCommand
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.getConfigOrNull
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeConsts
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

object MemberExitNotify : EventCommand<MemberLeaveEvent>(
    "退群通知",
) {

    @Command
    suspend fun MemberLeaveEvent.Quit.handleActive() {
        val message = getConfigOrNull<String>(group.simpleStr) ?: return
        sendMessage(
            message.replace("{duration}", (System.currentTimeMillis().div(1000) - member.joinTimestamp).formatted)
                .replace("{join}", LocalDateTime.ofEpochSecond(member.joinTimestamp.toLong(), 0, UTC).formatted)
                .replace("{id}", member.id.toString()).replace("{name}", member.nameCardOrNick)
        )
    }

    @Command
    suspend fun MemberLeaveEvent.Kick.handleInvite() {
        if (operator == user) return
        val message = getConfigOrNull<String>(group.simpleStr + ".踢出") ?: return
        sendMessage(
            message.replace("{duration}", (System.currentTimeMillis().div(1000) - member.joinTimestamp).formatted)
                .replace("{join}", LocalDateTime.ofEpochSecond(member.joinTimestamp.toLong(), 0, UTC).formatted)
                .replace("{id}", member.id.toString()).replace("{operator_id}", operator!!.id.toString())
                .replace("{operator_name}", member.nameCardOrNick).replace("{name}", member.nameCardOrNick)
        )
    }

    @Matcher(TriggerSubject.Group)
    fun MemberLeaveEvent.handle(): EventMatchResult? {
        return if (this@handle is MemberLeaveEvent) EventMatchResult(null) else null
    }
}


private val Long.formatted: String
    get() {
        val dur = this
        val year = dur / TimeConsts.YEAR
        val mon = dur % TimeConsts.YEAR / TimeConsts.MON
        val day = dur % TimeConsts.MON / TimeConsts.DAY
        val hour = dur % TimeConsts.DAY / TimeConsts.HOUR
        val min = dur % TimeConsts.HOUR / TimeConsts.MIN
        val sec = dur % TimeConsts.MIN
        return "${year}年${mon}月${day}天${hour}时${min * 60 + sec}s"
    }
private val LocalDateTime.formatted: String
    get() = "${year}年${monthValue}月${dayOfMonth}日${hour}时${minute}分${second}秒"

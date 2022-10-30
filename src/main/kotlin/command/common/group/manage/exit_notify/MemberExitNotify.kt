package command.common.group.manage.exit_notify

import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MemberLeaveEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage
import org.celery.utils.strings.placeholder
import org.celery.utils.time.TimeConsts
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC


object MemberExitNotify : Command(
    "退群通知",
) {
    val defaultKick: String
        get() = config["default.踢出", "{name}({id})离开了我们，他陪伴了我们{duration}"]
    val default: String
        get() = config["default", ""]
    var enableList: List<Long>
        get() = config["enable_group", listOf()]
    set(value) {
        config["enable_group"]=value
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

    @Command
    suspend fun MemberLeaveEvent.Quit.handleActive() {
        val message = config[group.simpleStr, default]
        if (message.isBlank()) return
        sendMessage(
            message.placeholder(
                "name" to member.nameCardOrNick,
                "duration" to (System.currentTimeMillis().div(1000) - member.joinTimestamp).formatted,
                "join" to LocalDateTime.ofEpochSecond(member.joinTimestamp.toLong(), 0, UTC).formatted,
                "id" to member.id.toString()
            )
        )
    }

    @Command
    suspend fun MemberLeaveEvent.Kick.handleInvite() {

        if (operator == null) return
        val message = config.getOrDefault(group.simpleStr + ".踢出", defaultKick)
        if (message.isBlank()) return
        sendMessage(
            message.placeholder(
                "name" to member.nameCardOrNick,
                "duration" to (System.currentTimeMillis().div(1000) - member.joinTimestamp).formatted,
                "join" to LocalDateTime.ofEpochSecond(member.joinTimestamp.toLong(), 0, UTC).formatted,
                "id" to member.id.toString(),
                "operator_id" to operator!!.id.toString(),
                "operator_name" to member.nameCardOrNick
            )
        )
    }

    @Trigger("handleActive")
    fun MemberLeaveEvent.Quit.matchQuiet(): EventMatchResult? {
        if (enableList.contains(groupId)) return EventMatchResult()
        else return null
    }

    @Trigger("handleInvite")
    fun MemberLeaveEvent.Quit.matchInvite(): EventMatchResult? {
        if (enableList.contains(groupId)) return EventMatchResult()
        else return null
    }
}



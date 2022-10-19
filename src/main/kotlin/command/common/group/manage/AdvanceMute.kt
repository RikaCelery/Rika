package command.common.group.manage

import events.ExecutionResult
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.GroupTools
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeConsts

object AdvanceMute : Command(
    "mute"
) {
    @Command("^(?:mute|禁言)\\s*((?:@?[\\d\\S]+\\s*)+)", permissions = arrayOf(RequirePermission.MEMBER,RequirePermission.OPERATOR))
    suspend fun GroupMessageEvent.muteMember(eventMatchResult: EventMatchResult): ExecutionResult {
        val regex1 = Regex(" (\\d{1,6})[s秒m分钟h小时d天mon月year年]*$", RegexOption.IGNORE_CASE)
        val time: String = regex1.find(eventMatchResult[1])?.value?.trim() ?: "120s"
        val member = eventMatchResult[1].replace(regex1, "").ifBlank { null }

        member ?: return ExecutionResult.Ignored("member 为空") //member 为空退出
        if (member == "list") {
            group.members.filter { it.isMuted }.joinToString("\n") { "${it.id} -> ${it.muteTimeRemaining}s" }.ifBlank {
                sendMessage("没有人被禁言~")
                null
            }?.let {
                sendMessage(it)
            }

            return ExecutionResult.Success
        }

        if (Rika.DEBUG_MODE) Rika.logger.debug("$subject $sender 禁言成员:$member")
        //获取禁言对象(可能有多个)
        val targets = if (Regex("(all)|(全体(成员)?)").matches(member.split(' ').first())) null
        else if (message.filterIsInstance<At>().isNotEmpty()) {
            //获取成员,过滤机器人自己and管理员
            message.filterIsInstance<At>().mapNotNull { group.members[it.target] }.filter { !it.isOperator() }
        } else {
            member.split(' ').filterNot(String::isBlank).map {
                println(it)
                GroupTools.getUserOrNull(group, it.trim())?.let { group[it.id]!! } ?: run {
                    sendMessage("未找到成员${it}或有多个成员匹配,请尝试使用qq号")
                    return ExecutionResult.Ignored("未找到该成员,请尝试使用qq号")
                }

            }
        }
        //解析禁言时间
        val duration = when {
            Regex("\\d+[s秒]?", RegexOption.IGNORE_CASE).matches(time) -> {
                Regex("^\\d+").find(time)!!.value.toInt()
            }
            Regex("\\d+((分钟)|m|分)", RegexOption.IGNORE_CASE).matches(time) -> {
                Regex("^\\d+").find(time)!!.value.toInt() * TimeConsts.MIN
            }
            Regex("\\d+((小时)|h|时)", RegexOption.IGNORE_CASE).matches(time) -> {
                Regex("^\\d+").find(time)!!.value.toInt() * TimeConsts.HOUR
            }
            Regex("\\d+[d天]", RegexOption.IGNORE_CASE).matches(time) -> {
                Regex("^\\d+").find(time)!!.value.toInt() * TimeConsts.DAY
            }
            Regex("\\d+((mon)|月)", RegexOption.IGNORE_CASE).matches(time) -> {
                Regex("^\\d+").find(time)!!.value.toInt() * TimeConsts.MON
            }
            Regex("\\d+((year)|年|y)", RegexOption.IGNORE_CASE).matches(time) -> {
                Regex("^\\d+").find(time)!!.value.toInt() * TimeConsts.YEAR
            }
            else -> {
                Rika.logger.warning("$subject $sender 时长解析失败:$time")
                return ExecutionResult.Ignored("$subject $sender 时长解析失败:$time")
            }
        }
        Rika.logger.info("禁言目标:$targets 禁言时长:$duration")
        if (sender.isOperator() || sender.isSuperUser()) {
            if (targets == null) {
                group.settings.isMuteAll = time != "0"
                return ExecutionResult.Success
            }
            if (targets.any { it.isOperator() }) {
                sendMessage("我超，没权限")
                return ExecutionResult.Ignored("我超，没权限")
            }
            if (duration > TimeConsts.MON) {
                sendMessage("这太长啦~，不能超过一个月哦")
                targets.forEach {
                    it.mute(TimeConsts.MON)
                    delay(1000)
                }
                return ExecutionResult.Success
            }
            if (duration == 0) {
                targets.forEach {
                    it.apply { if (it.isMuted) it.unmute() }
                    delay(1000)
                }
                return ExecutionResult.Success
            }
            targets.forEach {
                if (!it.isMuted) {
                    it.mute(duration)
                    delay(1300)
                }
            }
        } else {//普通群友发送
            if (subject.botAsMember.isOperator()) {
                sender.mute(120)
                sendMessage("😅")
            }
        }
        return ExecutionResult.Success
    }


}
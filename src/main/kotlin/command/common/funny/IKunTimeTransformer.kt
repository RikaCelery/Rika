package org.celery.command.common.funny

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeConsts

object IKunTimeTransformer:Command(
    "坤坤换算",
) {
    @Command("^(\\d+)(s|秒|m|min|分钟|h|小时|d|天|mon|月|year|年)?是(?:多少|几)[坤鲲]")
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val time = eventMatchResult[1]+eventMatchResult[2].ifBlank { "s" }
        //解析时间
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
        sendMessage("我算出来了！${time}是${"%g".format(duration.toFloat().div(TimeConsts.YEAR*2.5))}鲲!!!")
        return ExecutionResult.Success
    }
}
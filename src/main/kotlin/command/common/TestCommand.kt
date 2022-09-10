package org.celery.command.common

import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.buildMessageChain
import org.celery.command.controller.RegexCommand
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeUtils
import utils.time.LunarCalendarUtil

object TestCommand : RegexCommand("test", "^test$".toRegex()) {
    @Command
    suspend fun MessageEvent.handle1() {
        sendMessage(buildMessageChain {
            var l = LunarCalendarUtil.getLunarDetails(
                TimeUtils.getNowYear().toString(),
                TimeUtils.getNowMonth().toString(),
                TimeUtils.getNowDay().toString()
            )
            append(l + "\n")
            l = LunarCalendarUtil.getLunarYearMonthDay(
                TimeUtils.getNowYear().toString(),
                TimeUtils.getNowMonth().toString(),
                TimeUtils.getNowDay().toString())
            append(l + "\n")
            l = LunarCalendarUtil.getLunarMonthDay(
                TimeUtils.getNowYear().toString(),
                TimeUtils.getNowMonth().toString(),
                TimeUtils.getNowDay().toString())
            append(l + "\n")
            //            append("节气:" + l.termString +"\n")
//            append("干支历:" + l.cyclicalDateString +"\n")
//            append("星期" + l.dayOfWeek +"\n")
//            append("农历" + l.lunarMonth+" "+l.lunarDay +"\n")
//            val t = Calendar.getInstance()
//            t[1985, 10] = 17
//
//            val jieqi: Array<Date> = SolarTermsUtil.jieqilist(1940)
//            for (i in SolarTermsUtil.solarTerm.indices) {
//                append(SolarTermsUtil.solarTerm.get(i))
//                append(jieqi[i].month.plus(1).toString() + "月")
//                append(jieqi[i].date.toString()+"\n")
//            }
        })
    }

}
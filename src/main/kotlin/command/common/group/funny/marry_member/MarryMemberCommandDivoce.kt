package org.celery.command.common.group.funny.marry_member


import command.common.group.funny.marry_member.data.MarryMemberData
import command.common.group.funny.marry_member.data.MarryMemberData.getHusband
import command.common.group.funny.marry_member.data.MarryMemberData.getQingren
import command.common.group.funny.marry_member.data.MarryMemberData.getWife
import command.common.group.funny.marry_member.data.MarryMemberData.getXiaoSan
import command.common.group.funny.marry_member.data.MarryMemberData.newMap
import command.common.group.funny.marry_member.model.MarryResult.MarryType.Normal
import command.common.group.funny.marry_member.model.MarryResult.MarryType.Single
import events.ExecutionResult
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.number.probability
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeConsts

object MarryMemberCommandDivoce : RegexCommand(
    "闹离婚", "^闹离婚$".toRegex(), normalUsage = "闹离婚",
    description = "和当前娶的群友离婚",
) {
    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (newMap[group.id] == null) {
            newMap[group.id] = mutableListOf()
        }

        if (setCoolDown(TimeConsts.HOUR)) {
            sendMessage("爬，不许离婚")
            return ExecutionResult.LimitCall
        }
//        if (newMap[group.id]?.any { it.wife == member.id } == true && newMap[group.id]?.any { it.husband == sender.id } == false) {
        if (probability(0.9)) {
            return ExecutionResult.LimitCall
        }
        when {
            sender.getHusband() != null -> {
                sendMessage("离婚成功力\n天涯何处无芳草，何必单恋一枝花？")
                newMap[group.id]!!.removeIf { it.wife==sender.id&&it.type== Normal }
                val qingren = sender.getQingren()
                if (qingren !=null&&newMap[group.id]!!.any { it.contains(qingren)&&(it.type== Normal ||it.type== Single) }.not()){
//                    sendMessage("话说$qingren")
                }
            }
            sender.getWife() == null -> {
                sendMessage("离婚成功力\n天涯何处无芳草，何必单恋一枝花？")
                newMap[group.id]!!.removeIf { it.husband==sender.id&&it.type== Normal }
                if (sender.getXiaoSan().isNullOrEmpty().not()){
//                    sendMessage("")
                }
            }
            MarryMemberData.isSingle(group.id, sender.id) -> {
                sendMessage("你tm不是高贵的单身人士吗你闹jb的离婚")
            }
            else -> {
                sendMessage("unknown stat.")
            }
        }

        return ExecutionResult.Success
    }

}
package command.common.group.funny.marry_member


import command.common.group.funny.marry_member.data.MarryMemberData
import command.common.group.funny.marry_member.data.MarryMemberData.getHusband
import command.common.group.funny.marry_member.data.MarryMemberData.getWife
import command.common.group.funny.marry_member.data.MarryMemberData.getXiaoSan
import command.common.group.funny.marry_member.data.MarryMemberData.newMap
import command.common.group.funny.marry_member.model.MarryResult
import events.ExecutionResult
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.cast
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.getAvatar
import org.celery.utils.contact.GroupTools
import org.celery.utils.number.probability
import org.celery.utils.sendMessage
import org.celery.utils.time.TimeConsts

object MarryMemberCommandBeXioaSan : RegexCommand(
    "当小三", "^当\\s*(.+)\\s*的小三".toRegex(), normalUsage = "当<TA>的小三",
    description = "当小三",
) {
    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val target = GroupTools.getUserOrNull(group, eventMatchResult.getResult().groupValues[1].trim())?.cast<Member>()
        if (target == null) {
            sendMessage("没找见这个人欸.")
            return ExecutionResult.LimitCall
        }
        if (newMap[group.id] == null) {
            newMap[group.id] = mutableListOf()
        }
        if (newMap[group.id]!!.any { it.contains(sender.id)&&it.type==MarryResult.MarryType.Normal }){
            sendMessage("不许乱搞！！！")
            return ExecutionResult.Success
        }
        if (newMap[group.id]!!.any { it.contains(sender.id)&&it.type==MarryResult.MarryType.Single }){
            sendMessage("? 宁今天单身,就别到处拈花惹草了奥")
            return ExecutionResult.Success
        }

        if (setCoolDown(TimeConsts.HOUR)) {
            sendMessage("爬，不许当了")
            return ExecutionResult.LimitCall
        }
//        if (newMap[group.id]?.any { it.wife == member.id } == true && newMap[group.id]?.any { it.husband == sender.id } == false) {
        if (probability(0.2)) {
            sendMessage("就你他妈天天NTR别人是吧？414plz")
            return ExecutionResult.LimitCall
        }
        if (target.getWife() != null && target.getHusband() == null) {
            val marryResult = MarryResult(
                target.id, sender.id, MarryResult.MarryType.XiaoSan
            )
            if (newMap[group.id]!!.contains(marryResult)) {
                sendMessage("你已经当了他的小三了😡😡")
                return ExecutionResult.LimitCall
            }
            newMap[group.id]!!.add(
                marryResult
            )
            val message = buildMessageChain {
                +At(sender)
                +PlainText("今天你当了")
                +target.getAvatar(subject)
                +PlainText("[${target.nameCardOrNick}][${target.id}]的小三~")
                val xiaoSanList = target.getXiaoSan()
                if (xiaoSanList != null && xiaoSanList.isNotEmpty()) {
                    val xiaoSan = target.getXiaoSan()
                    if (xiaoSan != null && xiaoSan.size > 1) {
                        +(PlainText("\n哦顺便说一下，还有${xiaoSan.size - 1}个人在和你一起当他的小三哦~"))
                    }
                    if (xiaoSanList.size in 3..4) {
                        +PlainText("\n[${target.nameCardOrNick}][${target.id}]这么抢手的吗?")
                    } else if ((xiaoSanList.size >= 4)) {
                        +PlainText("\n我草你们是疯了吗?😨")
                    }
                }
            }
            group.sendMessage(message)
        } else {
            if (target.getHusband() != null)
                sendMessage("他已经被娶啦")
            else if (target.getWife() == null)
                sendMessage("他还是单身呢")
            else if (MarryMemberData.isSingle(group.id, target.id))
                sendMessage("${target.nameCardOrNick}是高贵的单身人士！")
            else
                sendMessage("unknown stat.")
        }
        return ExecutionResult.Success
    }

}
package org.celery.command.common.group.manage

import events.ExecutionResult
import events.ExecutionResult.Ignored
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.utils.cast
import org.celery.command.common.group.manage.RequireHeader.needConfirm
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.command.controller.setConfig
import org.celery.utils.contact.GroupTools.findMemberOrNull
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.toImage
import org.openqa.selenium.Dimension

object RequireHeaderConfirm : RegexCommand(
    "修改头衔", "^修改头衔\\s*(.*)\\s+(.+)".toRegex(), normalUsage = "",
    description = "修改头衔|同意群员头衔申请",
    example = "修改头衔 <群员> <头衔> \n(同意|拒绝)头衔 <序号> \n查看头衔申请",
    secondaryRegexs = arrayOf(Regex("^(同意|拒绝)头衔\\s+(\\d+)"), Regex("^查看头衔申请$"))
) {
    init {
        defaultCountLimit = -1
        defaultCoolDown = 0
    }

    @Command(ExecutePermission.SuperUser)
    suspend fun GroupMessageEvent.onload(eventMatchResult: EventMatchResult): ExecutionResult {
        if (group.botPermission.isOwner().not()) {
            return Ignored()
        }
        when (eventMatchResult.getIndexedResult().first) {
            //修改头衔
            0 -> {
                val member = group.findMemberOrNull(eventMatchResult[1].trim())
                val new = eventMatchResult[2]
                if (member == null) {
                    sendMessage("我他妈找不到你说的这个${eventMatchResult[1]}！")
                    return Ignored()
                }
                member.cast<NormalMember>().specialTitle = new
                delay(1000)
                sendMessage("成了！！！")
            }
            //(同意|拒绝)头衔
            1 -> {
                when(eventMatchResult[1]){
                    "同意"->{
                        val element = needConfirm[group.id]?.toList()?.get(eventMatchResult[2].toInt())?:return Ignored()
                        needConfirm[group.id]!!.remove(element.first)
                        group.members[element.first]!!.specialTitle = element.second
                        sendMessage("好！我改完了")
                        RequireHeader.getRequirableNames(group.id)
                        setConfig("${group.id}.requirable", RequireHeader.getRequirableNames(group.id).apply{
                            add(element.second)
                        }, RequireHeader.commandId)
                    }
                    "拒绝"->{

                        val element = needConfirm[group.id]?.toList()?.get(eventMatchResult[2].toInt())?:return Ignored()
                        sendMessage("好！我知道了！")
                        RequireHeader.getRequirableNames(group.id)
                        setConfig("${group.id}.not_requirable", RequireHeader.getRequirableNames(group.id).apply{
                            add(element.second)
                        }, RequireHeader.commandId)
                    }
                }
            }
            //查看头衔申请
            2 -> {
                buildMessageChain {
                    +SharedSelenium.render(buildString {
                        needConfirm[group.id]?.toList()?.forEachIndexed { index, pair ->
                            append("$index. ${pair.first}:${pair.second}")
                        }?:append("你群还没有申请任何头衔捏")
                    }, Dimension(300,0)).toImage(subject)
                }.sendTo(subject)
            }
        }
        return ExecutionResult.Success
    }

}



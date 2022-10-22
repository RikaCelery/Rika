package org.celery.command.builtin

import events.ExecutionResult
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.NEWLimitable
import org.celery.utils.contact.GroupTools
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage

object CallControl:Command(
    commandId = "次数限制",
    usage = "设置<功能名>次数[次数][群成员]"
){
    @Command("设置(.*)次数\\s?(\\d+)\\s?(.+)")
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val result = eventMatchResult.getResult()
        val user = GroupTools.getUserOrNull(subject,result.groupValues[3])
        if (user==null&&eventMatchResult[3].toLongOrNull()==null){
            return ExecutionResult.Failed(message = "user mot found")
        }
        val command = Rika.allRegisteredCommand2.find { it.commandId.equals(result.groupValues[1],true) }
        val newLimit = result.groupValues[2].toIntOrNull()
        command?:return ExecutionResult.Ignored("command not found.")
        newLimit?:return ExecutionResult.Ignored("command not found.")
        when(this){
            is GroupMessageEvent->{
                if (newLimit>command.getLimit(subject.id,user!!.id,NEWLimitable.LimitMode.GLOBAL) && sender.isSuperUser().not()){
                    return ExecutionResult.Failed(exception = IllegalStateException("call limit can not greater than ${
                        command.getLimit(subject.id,
                            user.id, NEWLimitable.LimitMode.GLOBAL)
                    }"))
                }
                if (sender.isOperator()||sender.isSuperUser()){
                    command.setLimit(subject.id, user.id,newLimit,command.getLimitMode(subject.id,user.id))
                    sendMessage("${command.commandId}: 设置${user.id}的限制为${newLimit}")
//                    command.cast<Limitable>().apply {
//                        if (user!=null)
//                            setUserCountLimit(group.id,user.id,newLimit)
//                        else
//                            setGroupCountLimit(group.id,newLimit)
//                    }

                }else
                    ExecutionResult.Ignored("not operator or super_user.")
            }
            is FriendMessageEvent->{
                if (sender.isSuperUser()){
                    val id = user?.id?:eventMatchResult[3].toLong()
                    command.setLimit(subject.id,id,newLimit,NEWLimitable.LimitMode.USER)
                    sendMessage("${command.commandId}: 设置${id}的全局限制为${newLimit}")
//                    command.cast<Limitable>().apply {
//                        setGlobalLimit(newLimit)
//                    }
                }else
                    ExecutionResult.Ignored("not operator or super_user.")
            }
        }


        return ExecutionResult.Success
    }
}
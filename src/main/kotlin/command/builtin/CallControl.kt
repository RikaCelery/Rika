package org.celery.command.builtin

import events.ExecutionResult
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.utils.cast
import org.celery.Rika
import org.celery.command.controller.CCommand
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.Limitable
import org.celery.command.controller.RegexCommand
import org.celery.utils.group.GroupTools
import org.celery.utils.permission.isSuperUser

object CallControl:RegexCommand(
    "次数限制","设置(.*)次数\\s?(\\d+)\\s?(.*)".toRegex()
){
    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val result = eventMatchResult.getResult()
        val user = GroupTools.getUserOrNull(subject,result.groupValues[3])
        val command = Rika.allRegisterdCommand.find { it.commandId.equals(result.groupValues[1],true) }
        val newLimit = result.groupValues[2].toIntOrNull()
        command?:return ExecutionResult.Ignored("command not found.")
        newLimit?:return ExecutionResult.Ignored("command not found.")
        if (getGlobalLimit()!=null&&newLimit>getGlobalLimit()!!&&sender.isSuperUser().not()){
            return ExecutionResult.Faild(IllegalStateException("call limit can not greater than ${getGlobalLimit()}"))
        }
        when(this){
            is GroupMessageEvent->{
                if (sender.isOperator()||sender.isSuperUser()){
                    command.cast<Limitable>().apply {
                        if (user!=null)
                            setUserCountLimit(group.id,user.id,newLimit)
                        else
                            setGroupCountLimit(group.id,newLimit)
                    }

                }else
                    ExecutionResult.Ignored("not operator or super_user.")
            }
            is FriendMessageEvent->{
                if (sender.isSuperUser()){
                    command.cast<Limitable>().apply {
                        setGlobalLimit(newLimit)
                    }
                }else
                    ExecutionResult.Ignored("not operator or super_user.")
            }
        }


        return ExecutionResult.Success()
    }
}
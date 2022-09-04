package org.celery.command.builtin

import com.example.events.ExecutionResult
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand

object FunctionCallControl: RegexCommand(
    "功能开关",
    "(开启|关闭)\\s*(.+)".toRegex(),
    description = "开启或关闭某个功能"
){
    @Command
    suspend fun MessageEvent.handle(matchResult: EventMatchResult): ExecutionResult {
        val mode = matchResult.getResult().groupValues[1]
        val command = Rika.allRegisterdCommand.singleOrNull { it.commandId==matchResult.getResult().groupValues[2] }
            ?: return ExecutionResult.Ignored("command not found.")
        when(mode){
            "开启"->{
                if (subject==sender) {
                    command.enable()
                    subject.sendMessage("OK")
                }
                else{
                    if (sender.safeCast<NormalMember>()?.isOperator()==true){
                        if(command.enbaleFor(subject.id))
                        subject.sendMessage("OK")
                        else
                        subject.sendMessage("OK")
                    }
                    else
                        return ExecutionResult.Ignored("Not Operator")
                }
            }
            "关闭"->{
                if (subject==sender) {
                    command.disable()
                    subject.sendMessage("OK")
                }
                else{
                    if (sender.safeCast<NormalMember>()?.isOperator()==true) {
                        if(command.disableFor(subject.id))
                            subject.sendMessage("OK")
                        else
                            subject.sendMessage("OK")
                    }
                    else
                        return ExecutionResult.Ignored("Not Operator")
                }
            }
            else-> return ExecutionResult.Ignored("mode:$mode is invalid.")
        }
        return ExecutionResult.Success()
    }
}
object ConsoleFunctionCallControlEnable: SimpleCommand(
    Rika,"开启",description = "指令开关,用于控制台使用"
){
    @Handler
    suspend fun ConsoleCommandSender.handle(commandId:String){
        val command = Rika.allRegisterdCommand.singleOrNull { it.commandId==commandId }
            ?: error("command not found.")
        command.enable()
        sendMessage("OK")
    }
}
object ConsoleFunctionCallControlDisable: SimpleCommand(
    Rika,"关闭", description = "指令开关,用于控制台使用"
){
    @Handler
    suspend fun ConsoleCommandSender.handle(commandId:String){
        val command = Rika.allRegisterdCommand.singleOrNull { it.commandId==commandId }
            ?: error("command not found.")
        command.disable()
        sendMessage("OK")
    }
}

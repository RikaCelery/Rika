package org.celery.command.builtin

import events.ExecutionResult
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage


/**
 * 开关某个指令
 */
object FunctionCallControl : Command(
    "功能开关",100,
    "管理",
    "开启或关闭某个功能",
    "开启|关闭<功能名>",
) {
    private fun getCommands(matchResult: EventMatchResult) =
        Rika.allRegisteredCommand2.filter { it.commandId.equals(matchResult[1].trim(), true) }.ifEmpty { null }

    @Command("^开启\\s*(.*)")
    suspend fun MessageEvent.handleEnable(eventMatchResult: EventMatchResult): ExecutionResult {
        when (this) {
            is FriendMessageEvent -> {
                if (sender.isSuperUser())
                    return enable(eventMatchResult)
            }
            is GroupMessageEvent -> {
                if (sender.isOperator() || sender.isSuperUser())
                    return enableInGroup(eventMatchResult)
            }
            else->{}
        }
        return ExecutionResult.Ignored
    }

    private suspend fun FriendMessageEvent.enable(matchResult: EventMatchResult): ExecutionResult {

        val commands = getCommands(matchResult)
        if (commands == null) {
            sendMessage(config["command_not_found", "没找到"])
            return ExecutionResult.Ignored("command not found.")
        }
        val map = commands.associateWith {
            it.enable()
        }
        sendMessage(
            map.mapKeys { it.key.commandId }.toList()
                .joinToString { it.first + ": " + if (it.second) "开启成功" else "已开启" })
        return ExecutionResult.Success
    }

    private suspend fun GroupMessageEvent.enableInGroup(matchResult: EventMatchResult): ExecutionResult {

        val commands = getCommands(matchResult)
        if (commands == null) {
            sendMessage(config["command_not_found", "没找到"])
            return ExecutionResult.Ignored("command not found.")
        }
        val map = commands.associateWith {
            it.cleanTemporary(subject.id)
        }
        sendMessage(
            map.mapKeys { it.key.commandId }.toList()
                .joinToString { it.first + ": " + if (it.second) "开启成功" else "已开启或全局禁用" })
        return ExecutionResult.Success
    }

    @Command("关闭\\s*(.*)")
    suspend fun MessageEvent.handleDisable(eventMatchResult: EventMatchResult):ExecutionResult{
        when (this) {
            is FriendMessageEvent -> {
                if (sender.isSuperUser())
                    return disable(eventMatchResult)
            }
            is GroupMessageEvent -> {
                if (sender.isOperator() || sender.isSuperUser())
                    return disableInGroup(eventMatchResult)
            }
            else->{}
        }
        return ExecutionResult.Ignored()
    }

    private suspend fun FriendMessageEvent.disable(matchResult: EventMatchResult): ExecutionResult {

        val commands = getCommands(matchResult)
        if (commands == null) {
            sendMessage(config["command_not_found", "没找到"])
            return ExecutionResult.Ignored("command not found.")
        }
        val map = commands.associateWith {
            it.close()
        }
        sendMessage(
            map.mapKeys { it.key.commandId }.toList()
                .joinToString { it.first + ": " + if (it.second) "关闭成功" else "已关闭" })
        return ExecutionResult.Success
    }

    private suspend fun GroupMessageEvent.disableInGroup(matchResult: EventMatchResult): ExecutionResult {

        val commands = getCommands(matchResult)
        if (commands == null) {
            sendMessage(config["command_not_found", "没找到"])
            return ExecutionResult.Ignored("command not found.")
        }
        val map = commands.associateWith {
            it.closeTemporarily(subject.id)
        }
        sendMessage(
            map.mapKeys { it.key.commandId }.toList()
                .joinToString { it.first + ": " + if (it.second) "关闭成功" else "已关闭或全局禁用" })
        return ExecutionResult.Success
    }
}


/**
 * 开某个指令(控制台)
 */
object ConsoleFunctionCallControlEnable : SimpleCommand(
    Rika, "开启", description = "指令开关,用于控制台使用"
) {
    @Handler
    suspend fun ConsoleCommandSender.handle(commandId: String) {
        if (commandId == "*") {
            Rika.allRegisteredCommand2.forEach {
                it.enable()
            }
            sendMessage("OK")
            return
        }
        val command = Rika.allRegisteredCommand2.singleOrNull { it.commandId.equals(commandId, true) }
            ?: error("command not found.")
        command.enable()
        sendMessage("OK")
    }
}

/**
 * 关某个指令(控制台)
 */
object ConsoleFunctionCallControlDisable : SimpleCommand(
    Rika, "关闭", description = "指令开关,用于控制台使用"
) {
    @Handler
    suspend fun ConsoleCommandSender.handle(commandId: String) {
        if (commandId == "*") {
            Rika.allRegisteredCommand2.forEach {
                if (it.commandId != FunctionCallControl.commandId)
                    it.close()
            }
            sendMessage("OK")
            return
        }
        val command = Rika.allRegisteredCommand2.singleOrNull { it.commandId.equals(commandId, true) }
            ?: error("command not found.")
        command.close()
        sendMessage("OK")
    }
}

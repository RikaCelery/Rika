package org.celery.command.builtin

import events.ExecutionResult
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand

/**
 * 开关某个指令
 */
object FunctionCallControl : RegexCommand(
    "功能开关",
    "(开启|关闭)\\s*(.+)".toRegex(),
    normalUsage = "开启|关闭<功能名>",
    description = "开启或关闭某个功能"
) {
    @Command(ExecutePermission.Operator)
    suspend fun MessageEvent.handle(matchResult: EventMatchResult): ExecutionResult {
        val mode = matchResult.getResult().groupValues[1]
        val command =
            Rika.allRegisteredCommand.find { it.commandId.equals(matchResult.getResult().groupValues[2].trim(), true) }
                ?: return ExecutionResult.Ignored("command not found.")
        when (mode) {
            "开启" -> {
                if (subject == sender) {
                    command.enable()
                    subject.sendMessage("OK")
                } else {
                    if (command.enableFor(subject.id)) subject.sendMessage("OK")
                    else subject.sendMessage("OK")
                }
            }
            "关闭" -> {
                if (subject == sender) {
                    command.disable()
                    subject.sendMessage("OK")
                } else {
                    if (command.disableFor(subject.id)) subject.sendMessage("OK")
                    else subject.sendMessage("OK")
                }
            }
            else -> return ExecutionResult.Ignored("mode:$mode is invalid.")
        }
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
            Rika.allRegisteredCommand.forEach {
                it.enable()
            }
            sendMessage("OK")
            return
        }
        val command = Rika.allRegisteredCommand.singleOrNull { it.commandId.equals(commandId, true) }
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
            Rika.allRegisteredCommand.forEach {
                if (it.commandId != FunctionCallControl.commandId)
                    it.disable()
            }
            sendMessage("OK")
            return
        }
        val command = Rika.allRegisteredCommand.singleOrNull { it.commandId.equals(commandId, true) }
            ?: error("command not found.")
        command.disable()
        sendMessage("OK")
    }
}

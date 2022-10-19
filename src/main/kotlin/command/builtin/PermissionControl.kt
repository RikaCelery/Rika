package org.celery.command.builtin

import events.ExecutionResult
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.command.controller.abs.NEWLimitable
import org.celery.utils.contact.GroupTools
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage

object Ban : Command(
    "ban", example = "(.*)(?:拉黑|ban|加黑|加黑名单)\\s*(.+)\n(.*)(?:解黑|unban|解黑名单)\\s*(.+)"
) {
    private val banUser = Regex("U\\d+")
    private val banSubject = Regex("[GS]\\d+")
    private val banUserInSubject = Regex("[GS](\\d+)U(\\d+)")

    @Command("(.*)(?:拉黑|ban|加黑|加黑名单)\\s*(.+)")
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (this is GroupMessageEvent) {
            if (!sender.isSuperUser() && !sender.isOperator()) return ExecutionResult.Ignored()
            val member = GroupTools.getUserOrNull(group, eventMatchResult[2])
            if (member == null) {
                sendMessage("未找到指定用户")
                return ExecutionResult.Ignored()
            }
            if (eventMatchResult[1].isNotBlank()) {
                val command = Rika.allRegisteredCommand2.find { it.commandId == eventMatchResult[1].trim() }
                if (command == null) {
                    sendMessage("未找到指令")
                    return ExecutionResult.Ignored
                }
                commandBanUser(command, member)
                sendMessage("${command.commandId} ban: ${member.id}")
            } else {
                banUser(member)
                sendMessage("ban: ${member.id}")
            }
            return ExecutionResult.Success

        } else {
            if (sender.isSuperUser().not()) return ExecutionResult.Ignored
            if (eventMatchResult[1].isNotBlank()) {
                val command = Rika.allRegisteredCommand2.find { it.commandId == eventMatchResult[1].trim() }
                if (command == null) {
                    sendMessage("未找到指令")
                    return ExecutionResult.Ignored
                }
                if (eventMatchResult[2].matches(banUser)) {
                    command.ban(0, eventMatchResult[2].substring(1).toLong())
                    sendMessage("${command.commandId} ban user${eventMatchResult.get(2)} globally")
                } else if (eventMatchResult[2].matches(banSubject)) {
                    command.ban(eventMatchResult[2].substring(1).toLong(), 0)
                    sendMessage("${command.commandId} ban subject${eventMatchResult.get(2)} globally")
                } else {

                    val matches = banUserInSubject.find(eventMatchResult[2])?.groupValues
                    if (matches != null) {
                        command.ban(matches[1].toLong(), matches[2].toLong())
                        sendMessage("${command.commandId} ban user${matches[2]} in subject${matches[1]} globally")
                    }
                }
            }else{
                if (eventMatchResult[2].matches(banUser)) {
                    NEWLimitable.banAll(0, eventMatchResult[2].substring(1).toLong())
                    sendMessage("ban user${eventMatchResult.get(2)} globally")
                } else if (eventMatchResult[2].matches(banSubject)) {
                    NEWLimitable.banAll(eventMatchResult[2].substring(1).toLong(), 0)
                    sendMessage("ban subject${eventMatchResult.get(2)} globally")
                } else {
                    val matches = banUserInSubject.find(eventMatchResult[2])?.groupValues
                    if (matches != null) {
                        NEWLimitable.banAll(matches[1].toLong(), matches[2].toLong())
                        sendMessage("ban user${matches[2]} in subject${matches[1]} globally")
                    }
                }
            }
        }
        return ExecutionResult.Success
    }

    private fun GroupMessageEvent.commandBanUser(command: org.celery.command.controller.abs.Command, member: User) {
        command.ban(group.id, member.id)
    }

    private fun GroupMessageEvent.banUser(member: User) {
        NEWLimitable.banAll(group.id, member.id)
    }
}
object Unban : Command(
    "unban",
) {
    private val banUser = Regex("U\\d+")
    private val banSubject = Regex("[GS]\\d+")
    private val banUserInSubject = Regex("[GS](\\d+)U(\\d+)")

    @Command("(.*)(?:解黑|unban|解黑名单)\\s*(.+)")
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        if (this is GroupMessageEvent) {
            if (!sender.isSuperUser() && !sender.isOperator()) return ExecutionResult.Ignored()
            val member = GroupTools.getUserOrNull(group, eventMatchResult[2])
            if (member == null) {
                sendMessage("未找到指定用户")
                return ExecutionResult.Ignored()
            }
            if (eventMatchResult[1].isNotBlank()) {
                val command = Rika.allRegisteredCommand2.find { it.commandId == eventMatchResult[1].trim() }
                if (command == null) {
                    sendMessage("未找到指令")
                    return ExecutionResult.Ignored
                }
                commandUnBanUser(command, member)
                sendMessage("${command.commandId} unban: ${member.id}")
            } else {
                unbanUser(member)
                sendMessage("unban: ${member.id}")
            }
            return ExecutionResult.Success

        } else {
            if (sender.isSuperUser().not()) return ExecutionResult.Ignored
            if (eventMatchResult[1].isNotBlank()) {
                val command = Rika.allRegisteredCommand2.find { it.commandId == eventMatchResult[1].trim() }
                if (command == null) {
                    sendMessage("未找到指令")
                    return ExecutionResult.Ignored
                }
                if (eventMatchResult[2].matches(banUser)) {
                    command.unban(0, eventMatchResult[2].substring(1).toLong())
                    sendMessage("${command.commandId} unban user${eventMatchResult.get(2)} globally")
                } else if (eventMatchResult[2].matches(banSubject)) {
                    command.unban(eventMatchResult[2].substring(1).toLong(), 0)
                    sendMessage("${command.commandId} unban subject${eventMatchResult.get(2)} globally")
                } else {
                    val matches = banUserInSubject.find(eventMatchResult[2])?.groupValues
                    if (matches != null) {
                        command.unban(matches[1].toLong(), matches[2].toLong())
                        sendMessage("${command.commandId} unban user${matches[2]} in subject${matches[1]} globally")
                    }
                }
            }else{
                if (eventMatchResult[2].matches(banUser)) {
                    NEWLimitable.unbanAll(0, eventMatchResult[2].substring(1).toLong())
                    sendMessage("unban user${eventMatchResult.get(2)} globally")
                } else if (eventMatchResult[2].matches(banSubject)) {
                    NEWLimitable.unbanAll(eventMatchResult[2].substring(1).toLong(), 0)
                    sendMessage("unban subject${eventMatchResult.get(2)} globally")
                } else {
                    val matches = banUserInSubject.find(eventMatchResult[2])?.groupValues
                    if (matches != null) {
                        NEWLimitable.unbanAll(matches[1].toLong(), matches[2].toLong())
                        sendMessage("unban user${matches[2]} in subject${matches[1]} globally")
                    }
                }
            }
        }
        return ExecutionResult.Success
    }

    private fun GroupMessageEvent.commandUnBanUser(command: org.celery.command.controller.abs.Command, member: User) {
        command.unban(group.id, member.id)
    }

    private fun GroupMessageEvent.unbanUser(member: User) {
        NEWLimitable.unbanAll(group.id, member.id)
    }
}

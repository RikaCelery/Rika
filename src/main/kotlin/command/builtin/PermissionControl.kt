package org.celery.command.builtin

import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.MessageEvent
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.permission.isSuperUser

/**
 * 负责黑白名单的管理
 */

object AddBlackList : RegexCommand(
    "添加黑名单",
    "(?:添加)?(.+)黑名单(?:添加)?(.+)".toRegex(),
    normalUsage = "<功能名>黑名单添加<成员>",
) {
    @Command
    fun MessageEvent.handle(eventMatchResult: EventMatchResult) {
        val user = eventMatchResult.getResult().groupValues[2]
        val command =
            Rika.allRegisteredCommand.singleOrNull { it.commandId == eventMatchResult.getResult().groupValues[1] }
        println("user: $user")
        println("command: $command")
        command ?: return
        user.toLongOrNull() ?: return
        if (subject == sender) {//私聊
            if (sender.isSuperUser())//判断是否为超级用户
                addBlackUserGlobal(user.toLong())
        } else {
            if (sender.cast<NormalMember>().isOperator()) {//群聊中管理员或群主拉黑用户
                command.addBlackUserInGroup(subject.id, user.toLong())
            } else {
                //ignore
            }
        }
    }
}

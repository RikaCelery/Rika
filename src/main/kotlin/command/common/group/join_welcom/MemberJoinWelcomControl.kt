package org.celery.command.common.group.join_welcom

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.config.main.PublicConfig
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage

object MemberJoinWelcomControl : RegexCommand(
    "入群欢迎控制","^设置(邀请)?入群欢迎词\\s*(.+)".toRegex(), normalUsage = "设置[邀请]入群欢迎词<欢迎词>",
    example = "设置入群欢迎词 欢迎入群{name}({id})\n欢迎{invitor_name}({invitor_id})邀请入群{name}({id})"
) {
    @Command(ExecutePermission.Operator)
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult){
        if (eventMatchResult[1].isBlank())
            PublicConfig["MemberJoinWelcom.${group.simpleStr}"] = eventMatchResult[2]
        else
            PublicConfig["MemberJoinWelcom.${group.simpleStr}.邀请"] = eventMatchResult[2]
        sendMessage("OJBK")
    }
}
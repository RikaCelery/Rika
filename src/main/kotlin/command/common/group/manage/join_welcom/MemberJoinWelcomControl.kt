package org.celery.command.common.group.join_welcom

import events.ExecutionResult
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage

object MemberJoinWelcomControl : Command(
    "入群欢迎控制",
    example = "设置入群欢迎词 欢迎入群{name}({id})\n欢迎{invitor_name}({invitor_id})邀请入群{name}({id})"
) {
    @Command("^设置(邀请)?入群欢迎词\\s*(.*)", permissions = [RequirePermission.MEMBER,RequirePermission.OPERATOR])
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult.Success {
        if (MemberJoinWelcom.enableList.contains(group.id).not())
            MemberJoinWelcom.enableList = listOf(*MemberJoinWelcom.enableList.toTypedArray(), group.id)
        if (eventMatchResult[2].isBlank() && eventMatchResult[1].isBlank()) {
            sendMessage(
                "当前的入群欢迎词是: %s".format(
                    config.getOrDefault(
                        group.simpleStr, MemberJoinWelcom.default
                    )
                )
            )
            return ExecutionResult.Success
        }
        if (eventMatchResult[2].isBlank() && eventMatchResult[1].isNotBlank()) {
            sendMessage(
                "当前的邀请入群欢迎词是: %s".format(
                    config.getOrDefault(
                        group.simpleStr + ".邀请", MemberJoinWelcom.defaultInvite
                    )
                )
            )
            return ExecutionResult.Success
        }
        if (eventMatchResult[1].isBlank())
            MemberJoinWelcom.config[group.simpleStr] = eventMatchResult[2]
        else
            MemberJoinWelcom.config["${group.simpleStr}.邀请"] = eventMatchResult[2]
        sendMessage("OJBK")
        return ExecutionResult.Success
    }
}
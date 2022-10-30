package command.common.group.manage.exit_notify

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage

object MemberExitNotifyControl : Command(
    "退群通知控制",
) {
    @Command("设置(踢出)?退群提醒\\s*(.*)", permissions = [RequirePermission.MEMBER, RequirePermission.OPERATOR])
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult) {
        if (MemberExitNotify.enableList.contains(group.id).not())
            MemberExitNotify.enableList = listOf(*MemberExitNotify.enableList.toTypedArray(), group.id)
        if (eventMatchResult[2].isBlank()) {
            sendMessage(
                "当前的退群提示词是: %s".format(
                    MemberExitNotify.config.getOrDefault(
                        group.simpleStr, MemberExitNotify.default
                    )
                )
            )
            return
        }
        if (eventMatchResult[1].isBlank())
            MemberExitNotify.config[group.simpleStr] = eventMatchResult[2]
        else MemberExitNotify.config["${group.simpleStr}.踢出"] = eventMatchResult[2]
        sendMessage("OJBK")
    }
}

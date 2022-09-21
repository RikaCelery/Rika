package command.common.group.exit_notify

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.config.main.PublicConfig
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage

object MemberExitNotifyControl : RegexCommand(
    "退群通知控制","^设置(踢出)?退群提醒\\s*(.+)".toRegex(RegexOption.MULTILINE),
    example = "设置退群提醒 {name}({id})离开了群聊\\n入群时长:{duration},入群时间:{join},入群时间:{join}\n设置退群踢出提醒 呜呜{operator_name}({operator_id})踢掉了{name}({id})\n" +
            "入群时长:{duration},入群时间:{join}"
) {
    @Command(ExecutePermission.Operator)
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult){
        if (eventMatchResult[1].isBlank())
            PublicConfig["MemberExitNotify.${group.simpleStr}"] = eventMatchResult[2]
        else
            PublicConfig["MemberExitNotify.${group.simpleStr}.踢出"] = eventMatchResult[2]
        sendMessage("OJBK")
    }
}
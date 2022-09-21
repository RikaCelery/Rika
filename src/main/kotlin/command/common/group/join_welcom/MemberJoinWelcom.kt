package org.celery.command.common.group.join_welcom

import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MemberJoinEvent
import org.celery.command.controller.EventCommand
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.getConfigOrNull
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage

object MemberJoinWelcom : EventCommand<MemberJoinEvent>(
    "入群欢迎",
) {

    @Matcher(TriggerSubject.Group)
    fun MemberJoinEvent.handle() = if (this@handle is MemberJoinEvent) EventMatchResult(null) else null
    @Command
    suspend fun MemberJoinEvent.Active.handleActive(){
        val message = getConfigOrNull<String>(group.simpleStr)?:return
        sendMessage(
            message
                .replace("{id}",member.id.toString())
                .replace("{name}",member.nameCardOrNick)
        )
    }
    @Command
    suspend fun MemberJoinEvent.Invite.handleInvite(){
        val message = getConfigOrNull<String>(group.simpleStr + ".邀请") ?: return
        sendMessage(
            message
            .replace("{id}",member.id.toString())
            .replace("{invitor_id}",invitor.id.toString())
            .replace("{invitor_name}",member.nameCardOrNick)
            .replace("{name}",member.nameCardOrNick)
        )
    }
}
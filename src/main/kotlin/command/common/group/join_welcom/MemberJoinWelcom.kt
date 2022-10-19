package org.celery.command.common.group.join_welcom

import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MemberJoinEvent
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.simpleStr
import org.celery.utils.sendMessage
import org.celery.utils.strings.placeholder

@Suppress("unused")
object MemberJoinWelcom : Command(
    "入群欢迎",
) {
    val defaultInvite: String
        get() = config["default.邀请", "欢迎{name}({id}),邀请者{invitor_name}({invitor_id})"]
    val default: String
        get() = config["default", "欢迎{name}({id})"]
    var enableList: List<Long>
        get() = config["enable_group", listOf()]
        set(value) {
            config["enable_group"] = value
        }

    @Command
    suspend fun MemberJoinEvent.Active.handleActive() {
        val message = config.getOrDefault(group.simpleStr, default)
        sendMessage(
            message.placeholder(
                "id" to member.id.toString(),
                "name" to member.nameCardOrNick,
            )
        )
    }

    @Command
    suspend fun MemberJoinEvent.Invite.handleInvite() {
        val message = config.getOrDefault(
            group.simpleStr + ".邀请", defaultInvite
        )
        sendMessage(
            message.placeholder(
                "id" to member.id.toString(),
                "invitor_id" to invitor.id.toString(),
                "invitor_name" to member.nameCardOrNick,
                "name" to member.nameCardOrNick,
            )
        )
    }

    @Trigger("handleActive")
    fun MemberJoinEvent.Active.matchQuiet(): EventMatchResult? {
        if (enableList.contains(groupId)) return EventMatchResult()
        else return null
    }

    @Trigger("handleInvite")
    fun MemberJoinEvent.Invite.matchInvite(): EventMatchResult? {
        if (enableList.contains(groupId)) return EventMatchResult()
        else return null
    }
}
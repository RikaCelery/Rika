package command.common.group.manage

import events.ExecutionResult
import kotlinx.coroutines.delay
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.cast
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.contact.GroupTools
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage
import org.celery.utils.strings.placeholder

object KickMember : Command(
    "踢人"
) {
    @Command("^(?:kick|踢人?)\\s*(.+)", permissions = arrayOf(RequirePermission.MEMBER,RequirePermission.OPERATOR))
    suspend fun GroupMessageEvent.onload(eventMatchResult: EventMatchResult): ExecutionResult {
        val member = GroupTools.getUserOrNull(group,eventMatchResult.getResult().groupValues[1])?.cast<NormalMember>()
        val targetMember = member ?: group.get(message[QuoteReply.Key]!!.source.fromId)
        ?: return ExecutionResult.Ignored("target member not found.")
        val targetMembers = mutableListOf(targetMember.id).apply {
            addAll(message.filterIsInstance<At>().map { it.target })
        }.toHashSet().mapNotNull { group.members[it] }.filterNot { it == sender }
        if ((group.botAsMember).isOwner() && sender.isSuperUser()) {
            targetMembers.forEach {
                it.kick("")
                delay(1000)
            }
            sendMessage(config["members_kicked","{members}已经被移除"].placeholder(
                "members" to targetMembers.joinToString("\n") { it.id.toString()+"\n" }
            ))
            return ExecutionResult.Success
        } else {
            val members = targetMembers.filterNot {
                it.isOperator()
            }.onEach {
                it.kick("")
                delay(1000)
            }

            sendMessage(config["members_kicked","{members}已经被移除"].placeholder(
                "members" to members.joinToString("\n") { it.id.toString()+"\n" }
            ))
        }
        targetMember.kick("")
        return ExecutionResult.Success
    }

}



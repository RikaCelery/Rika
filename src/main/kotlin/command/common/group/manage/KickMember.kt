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
import org.celery.command.controller.RegexCommand
import org.celery.utils.group.GroupTools
import org.celery.utils.permission.isSuperUser

object KickMember : RegexCommand(
    "踢人", "^(?:kick|踢人?)\\s*(.+)".toRegex(), description = "踢人,仅管理员可用",
    normalUsage = "kick|踢人 <群员>",
) {
    override var defaultCountLimit: Int
        get() = 300
        set(value) {}
    override var defaultCoolDown: Long
        get() = 3000
        set(value) {}
    @Command(executePermission = [ExecutePermission.Operator])
    suspend fun GroupMessageEvent.onload(eventMatchResult: EventMatchResult): ExecutionResult {
        val member = GroupTools.getUserOrNull(group,eventMatchResult.getResult().groupValues[1])?.cast<NormalMember>()
        val targetMember = member ?: group.get(message[QuoteReply.Key]!!.source.fromId)
        ?: return ExecutionResult.Ignored("target member not found.")
        val targetMembers = mutableListOf(targetMember.id).apply {
            addAll(message.filterIsInstance<At>().map { it.target })
        }.toHashSet().mapNotNull { group.members[it] }.filterNot { it == sender }
        if ((bot as NormalMember).isOwner() && sender.isSuperUser()) {
            targetMembers.forEach {
                it.kick("")
                delay(1000)
            }
            return ExecutionResult.Success
        } else {
            targetMembers.filter {
                it.isOperator()
            }.forEach {
                it.kick("")
                delay(1000)
            }
        }
        targetMember.kick("")
        return ExecutionResult.Success
    }

}



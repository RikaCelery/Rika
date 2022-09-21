package org.celery.command.common.group.manage

import events.ExecutionResult
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOwner
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.utils.safeCast
import org.celery.command.controller.Call
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.command.controller.getConfig
import org.celery.utils.sendMessage
import java.util.*

object RequireHeader : RegexCommand(
    "申请头衔", "^申请头衔\\s*(.*)".toRegex(), normalUsage = "申请头衔 <头衔>",
    description = "申请头衔,每天只能改一次",
) {
    init {
        defaultCountLimit = 1
        defaultCoolDown = 10000
    }
    override suspend fun Bot.limitNotice(call: Call, finalLimit: Int) {
        //nothing
    }

    val needConfirm = Hashtable<Long, MutableMap<Long, String>>()

    fun getRequirableNames(groupId: Long): MutableList<String> =
        getConfig("${groupId}.requirable", mutableListOf())

    @Command
    suspend fun GroupMessageEvent.onload(eventMatchResult: EventMatchResult): ExecutionResult {
        val member = sender.safeCast<NormalMember>() ?: return ExecutionResult.Ignored()
        if (group.botPermission.isOwner()) {
            val require = eventMatchResult[1]
            if (getRequirableNames(group.id).contains(require).not()) {
                sendMessage("他妈 我不敢给你这头衔 你等群主或者管理同意")
                val map = needConfirm[group.id] ?: mutableMapOf()
                map[sender.id] = eventMatchResult[1]
                needConfirm[group.id] = map
            } else {
                member.specialTitle = require
            }
        }
        return ExecutionResult.Success
    }

}



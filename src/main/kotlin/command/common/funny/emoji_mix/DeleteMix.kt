package org.celery.command.common.funny.emoji_mix

import events.ExecutionResult
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.safeCast
import okhttp3.internal.toHexString
import org.celery.command.common.funny.emoji_mix.EmojiConsts.EMOJI_REGEX
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage

object DeleteMix : Command(
    "删除混表情合",
    usage = "删除表情混合<emoji1><emoji2><emoji3>...",
    description = "从emojimix图库内删除自定义图",
) {
    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val result1 = eventMatchResult[1]
        val emos = result1.codePoints().toArray().map { it.toHexString() }.sorted()
        val fileNamePrefix = emos.joinToString("+")
        val file = getOrCreateDataFile("emojiMix/$fileNamePrefix.png", EmojiMix.commandId)
        if (file.delete())
            sendMessage("删除成功")
        else sendMessage("不存在")
        return ExecutionResult.Success
    }

    private val regex = Regex("^删除混合表情(.{2,20})")
    private val secondaryRegexs = listOf(Regex("^删除表情混合(.{2,20})"), Regex("^表情混合删除(.{2,20})"))

    @Trigger("handle")
    fun MessageEvent.match(): EventMatchResult? {
        if (!sender.isSuperUser() && sender.safeCast<NormalMember>()?.isOperator() != true)
            return null
        return (regex.find(message.content)
            ?: secondaryRegexs.firstNotNullOfOrNull { it.find(message.content) })?.let { result ->
            val regex1 = EMOJI_REGEX.toRegex()
            if (result.groupValues[1].codePoints().count().toInt() == regex1.findAll(result.groupValues[1]).count())
                EventMatchResult(result)
            else
                null

        }
    }
}
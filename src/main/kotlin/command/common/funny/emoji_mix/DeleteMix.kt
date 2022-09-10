package org.celery.command.common.funny.emoji_mix

import events.ExecutionResult
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import okhttp3.internal.toHexString
import org.celery.command.common.funny.emoji_mix.EmojiConsts.EMOJI_REGEX
import org.celery.command.controller.CommandBlockMode
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.sendMessage

object DeleteMix : RegexCommand(
    "删除混合表情",
    regex = "^删除混合表情(.{2,20})".toRegex(),
    normalUsage = "添加表情混合<emoji1><emoji2>",
    description = "向emojimix图库内加图",
    example = "😂🤣\n😍😗",
    blockMode = CommandBlockMode.BLACKLIST,
    secondaryRegexs = arrayOf("^删除表情混合(.{2,20})".toRegex())
) {
    override var blockSub: Boolean = true

    @Command(ExecutePermission.Operator)
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val result1 = eventMatchResult[1]
        val emos = result1.codePoints().toArray().map { it.toHexString() }.sorted()
//        val e1 = result1.codePoints().toArray()[0]
//        val e2 = result1.codePoints().toArray()[1]
//        val emo1 = e1.toHexString()
//        val emo2 = e2.toHexString()
        val fileNamePrefix = emos.joinToString("+")
        val file = getDataFile("emojiMix/$fileNamePrefix.png", EmojiMix.commandId)
        if (file.delete())
            sendMessage("删除成功")
        else sendMessage("不存在")
        return ExecutionResult.Success
    }

    @Matcher
    override suspend fun MessageEvent.match(): EventMatchResult? {
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
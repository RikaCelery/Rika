package org.celery.command.common.funny.emoji_mix

import events.ExecutionResult
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.safeCast
import okhttp3.internal.toHexString
import org.celery.command.common.funny.emoji_mix.EmojiConsts.EMOJI_REGEX
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.http.HttpUtils
import org.celery.utils.interact.nextMessage
import org.celery.utils.permission.isSuperUser
import org.celery.utils.sendMessage

object AddMix : Command(
    "添加表情混合",
    usage = "添加表情混合<emoji1><emoji2>",
    description = "向emojimix图库内加图",
) {

    @Command
    suspend fun MessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val pic = nextMessage("请发送图片", 120, autoRecall = false) {
            it.filterIsInstance<Image>().isNotEmpty()
        }?.filterIsInstance<Image>()?.singleOrNull() ?: return ExecutionResult.LimitCall
        val result1 = eventMatchResult[1]
        val emos = result1.codePoints().toArray().map { it.toHexString() }.sorted()
        val fileNamePrefix = emos.joinToString("+")
        getOrCreateDataFile("emojiMix/$fileNamePrefix.png", EmojiMix.commandId).writeBytes(HttpUtils.downloader(pic.queryUrl()))
        sendMessage("添加成功")
        return ExecutionResult.Success
    }

    private val regex = Regex("^添加混合表情(.{2,20})")
    private val secondaryRegexs = listOf(Regex("^添加表情混合(.{2,20})"),Regex("^表情混合添加(.{2,20})"))

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
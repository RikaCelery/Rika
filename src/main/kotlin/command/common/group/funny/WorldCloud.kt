package org.celery.command.common.group.funny

import events.ExecutionResult
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain.Companion.deserializeJsonToMessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.Rika
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.RegexCommand
import org.celery.utils.commandline.runCommandReadText
import org.celery.utils.contact.GroupTools
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.sql.MessageEventSql
import org.celery.utils.time.TimeConsts.DAY
import org.celery.utils.time.TimeConsts.HOUR
import org.celery.utils.time.TimeConsts.MIN
import org.celery.utils.time.TimeConsts.MON
import org.celery.utils.time.TimeConsts.YEAR
import org.celery.utils.toImage
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object WorldCloud : RegexCommand(
    "词云", "^serm\\s*(.+)".toRegex(), normalUsage = "serm <qq号|群名片|@>", description = "展示某个群成员的词云",
    example = "serm @osuie\nserm 322617712",
) {
    @Command
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val  member = eventMatchResult[1].trim()
        if (member.isBlank()) {
            sendMessage("你吗？发个空白老子查啥")
            logger.debug("isBlank! 查询成员:$member")
            return ExecutionResult.LimitCall
        }
        logger.info("${subject.id} ${sender.id} 查询成员:$member")
        val memberId = try {
            member.toLongOrNull() ?: member.substring(1).trim().toLong()
        } catch (e: Exception) {
            GroupTools.getUserOrNull(group, member)?.id ?: run {
                sendMessage("我草,未找到该成员,请尝试使用qq号")
                return ExecutionResult.Ignored("member not found")
            }
        }
        sendMessage("OK别急,我在查了 -> $memberId")
        val joinInfo = (subject)[memberId]?.let {
            val dur = System.currentTimeMillis() / 1000 - it.joinTimestamp
            val year = dur / YEAR
            val mon = dur % YEAR / MON
            val day = dur % MON / DAY
            val hour = dur % DAY / HOUR
            val min = dur % HOUR / MIN
            val sec = dur % MIN
            "入群时间:${
                LocalDateTime.ofEpochSecond(it.joinTimestamp.toLong(), 0, ZoneOffset.UTC).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )
            }\n" +
                    "加群时间:${year}年${mon}月${day}天${hour}时${min}分${sec}秒\n"
        }
        val result = MessageEventSql.getMessages(group.id, memberId)
        if (result.isEmpty()) {
            sendMessage("我草,没查到")
            return ExecutionResult.Success
        }
        val lastTime = (subject)[memberId]?.lastSpeakTimestamp ?: result.maxOf { it.time }
//                val old = EventsSqlOld().init().getMessages(group.id, memberId)
        println(result.size)
//                println(old.size)
//                result.addAll(old)
        val lsatTimeStr = LocalDateTime.ofEpochSecond(lastTime.toLong(), 0, ZoneOffset.ofHours(8))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val count = result.size
        val tmp = getTempFile("${group.id}/${sender.id}/words.txt")
            .apply { parentFile.mkdirs();delete() }
        result.forEach { sss ->
            try {
                sss.msg.deserializeJsonToMessageChain().filterIsInstance<PlainText>().forEach sub@{
                    if (it.content.contains(Regex("(^/?serm)|(你的QQ暂不支持查看视频短片)|(mirai:image)|(mirai:app)|(mirai:file)")))
                        return@sub
                    tmp.appendText(it.content + "\n")
                }
            } catch (e: Exception) {
                Rika.logger.warning(e.toString())
            }
        }
        val script = getPublicDataFile("scripts/ciyun.py")
        val targetImage = tmp.parentFile.resolve("image.jpg")
        ("python $script \"${tmp.absolutePath}\" \"${targetImage.absolutePath}\"".runCommandReadText())
        val image = targetImage.toExternalResource().use {
            group.uploadImage(it)
        }

        group.sendMessage(
            buildMessageChain {
                +At(sender)
                +SharedSelenium.render((joinInfo ?: "") + "上次说话是在$lsatTimeStr\n在本群发了$count 条消息").toImage(subject)
                +image
            }
        )
        logger.debug("serm finish")
        return ExecutionResult.Success
    }

}
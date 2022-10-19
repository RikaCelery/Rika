package org.celery.command.common.group.funny

import events.ExecutionResult
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChain.Companion.deserializeJsonToMessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.celery.command.controller.EventMatchResult
import org.celery.command.controller.abs.Command
import org.celery.utils.commandline.runCommandReadText
import org.celery.utils.contact.GroupTools
import org.celery.utils.selenium.SharedSelenium
import org.celery.utils.sendMessage
import org.celery.utils.sql.MessageEventSql
import org.celery.utils.strings.placeholder
import org.celery.utils.time.TimeConsts.DAY
import org.celery.utils.time.TimeConsts.HOUR
import org.celery.utils.time.TimeConsts.MIN
import org.celery.utils.time.TimeConsts.MON
import org.celery.utils.time.TimeConsts.YEAR
import org.celery.utils.toImage
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object WorldCloud : Command(
    "词云"
) {
    @Command("^serm\\s*(.+)")
    suspend fun GroupMessageEvent.handle(eventMatchResult: EventMatchResult): ExecutionResult {
        val member = eventMatchResult[1].trim()
        if (member.isBlank()) {
            sendMessage(config["empty_member", "你吗？发个空白老子查啥"])
            logger.debug("查询成员isBlank!")
            return ExecutionResult.LimitCall
        }
        logger.info("${subject.id} ${sender.id} 查询成员:$member")
        val memberId = try {
            member.toLongOrNull() ?: member.substring(1).trim().toLong()
        } catch (e: Exception) {
            GroupTools.getUserOrNull(group, member)?.id ?: run {
                sendMessage(config["member_not_nound", "我草,未找到该成员,请尝试使用qq号"])
                return ExecutionResult.Ignored("member not found")
            }
        }
        sendMessage(config["searching", "OK别急,我在查了 -> {member_id}"].placeholder("member_id" to memberId))
        val joinInfo = (subject)[memberId]?.let {
            val dur = System.currentTimeMillis() / 1000 - it.joinTimestamp
            val year = dur / YEAR
            val mon = dur % YEAR / MON
            val day = dur % MON / DAY
            val hour = dur % DAY / HOUR
            val min = dur % HOUR / MIN
            val sec = dur % MIN
            "入群时间:{join_time}\n加群时间:{year}年{mon}月{day}天{hour}时{min}分{sec}秒\n".placeholder(
                "year" to year,
                "mon" to mon,
                "day" to day,
                "hour" to hour,
                "min" to min,
                "sec" to sec,
                "join_time" to LocalDateTime.ofEpochSecond(it.joinTimestamp.toLong(), 0, ZoneOffset.UTC).format(
                    DateTimeFormatter.ofPattern(config["time_format", "yyyy-MM-dd HH:mm:ss"])
                ),
            )
        }

        val result = MessageEventSql.getMessages(group.id, memberId)
        if (result.isEmpty()) {
            sendMessage(config["empty_result","我草,没查到"])
            return ExecutionResult.Success
        }

        val lastTime = (subject)[memberId]?.lastSpeakTimestamp ?: result.maxOf { it.time }
//                val old = EventsSqlOld().init().getMessages(group.id, memberId)
        println(result.size)
        //                println(old.size)
//                result.addAll(old)
        val lastTimeStr = LocalDateTime.ofEpochSecond(lastTime.toLong(), 0, ZoneOffset.ofHours(8))
            .format(DateTimeFormatter.ofPattern(config["time_format", "yyyy-MM-dd HH:mm:ss"]))

        val count = result.size
        val tmp = getTempFile("${group.id}/${sender.id}/words.txt").apply { parentFile.mkdirs();delete() }
        result.forEach{ sss ->
            try {
                sss.msg.deserializeJsonToMessageChain().filterIsInstance<PlainText>().forEach sub@{
                    if (it.content.contains(Regex("(^/?serm)|(你的QQ暂不支持查看视频短片)|(mirai:image)|(mirai:app)|(mirai:file)"))) return@sub
                    tmp.appendText(it.content + "\n")
                }
            } catch (e: Exception) {
                logger.warning(e)
            }
        }

        val script = getResource("scripts/ciyun.py")
        val targetImage = tmp.parentFile.resolve("image.jpg")
        ("python $script \"${tmp.absolutePath}\" \"${targetImage.absolutePath}\"".runCommandReadText())
        val image = targetImage.toExternalResource().use {
            group.uploadImage(it)
        }

        group.sendMessage(buildMessageChain {
            +At(sender)
            +SharedSelenium.render((joinInfo ?: "") + "上次说话是在{last_speak_time}\n在本群发了{message_count} 条消息".placeholder(
                "last_speak_time" to lastTimeStr,
                "message_count" to count
            )).toImage(subject)
            +image
        })
        return ExecutionResult.Success
    }

}
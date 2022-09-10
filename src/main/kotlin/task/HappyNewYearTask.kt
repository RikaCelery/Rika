package org.celery.task

import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import org.celery.Rika
import org.celery.utils.task_controller.AbstractBotTask
import org.celery.utils.time.TimeUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class HappyNewYearTask : AbstractBotTask(months = 1, days = 1) {
    private val LocalDateTime.string: String
        get() {
            return format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时mm分ss秒"))
        }
    private val LocalDate.seconds: Long
        get() {
            val now = LocalTime.now()
            return toEpochSecond(now, ZoneOffset.UTC)
        }

    override fun whenTrigger(date: LocalDateTime) {
        var count = 0
        val visitedGroupList = mutableListOf<Long>()
        Rika.globalEventChannel().subscribe<GroupMessageEvent> {
            if (group.id !in visitedGroupList) {
                try {
                    group.sendMessage(genNewYearMessage(++count))
                } catch (_: Exception) {
                    return@subscribe ListeningStatus.LISTENING
                }
            }
            if (count < 3) ListeningStatus.LISTENING
            else ListeningStatus.STOPPED
        }
    }

    private fun genNewYearMessage(count: Int): Message {
        val text = PlainText("新年快乐！！！恭喜你们成为在${TimeUtils.now("yyyy年")}第${count}个发言的群聊\n(｡･∀･)ﾉﾞ")
        return text
    }

}

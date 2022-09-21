package org.celery.task

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.utils.task_controller.AbstractBotTask
import java.time.LocalDateTime

class NewTask : AbstractBotTask(hours = 12, minutes = 30) {
    override val logger = MiraiLogger.Factory.create(this::class)

    override fun whenTrigger(date: LocalDateTime) {
        runBlocking{
            Bot.getInstance(123456).getGroup(123456)?.sendMessage("sadio")
        }
    }
}
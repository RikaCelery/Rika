package org.celery.task

import org.celery.utils.task_controller.AbstractBotTask
import java.time.LocalDateTime

class CommandDataAutoSave : AbstractBotTask() {
    override val period: Int
        get() = 10
    override val firstTime: LocalDateTime?
        get() = LocalDateTime.now()

    override fun run() {
//        Limitable.save()
    }

    override fun whenTrigger(date: LocalDateTime) {
    }
}
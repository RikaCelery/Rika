package org.celery.task

import org.celery.utils.task_controller.AbstractBotTask
import org.celery.command.controller.Limitable
import java.time.LocalDateTime

class CommandDataAutoSave : AbstractBotTask(seconds = 0) {
    override val period: Int?
        get() = 10

    override fun run() {
        Limitable.save()
    }

    override fun whenTrigger(date: LocalDateTime) {
    }
}
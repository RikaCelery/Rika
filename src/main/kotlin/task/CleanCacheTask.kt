package org.celery.task

import command.common.group.funny.marry_member.data.MarryMemberData
import net.mamoe.mirai.utils.MiraiLogger
import org.celery.config.main.function.CountRecords
import org.celery.data.TempData
import org.celery.utils.task_controller.AbstractBotTask
import org.celery.utils.time.TimeConsts
import org.celery.utils.time.TimeUtils
import java.time.LocalDateTime

class CleanCacheTask : AbstractBotTask(hours = 0, minutes = 0) {
    override val logger = MiraiLogger.Factory.create(this::class)
    override val firstTime: LocalDateTime
        get() = TimeUtils.getStartOfNextDay()
    override val period: Int
        get() = TimeConsts.DAY

    override fun whenTrigger(date: LocalDateTime) {
        logger.debug("clean:MarryMemberData.marriedMap")
        MarryMemberData.newMap.clear()
        logger.debug("clean:Command.callHistory")
        CountRecords.clear()
        logger.debug("clean:TempData")
        TempData.clear()
    }
}
package org.celery.utils.task_controller

import net.mamoe.mirai.utils.MiraiLogger
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.jvm.jvmName

object BotTaskController:Timer() {
    private val logger = MiraiLogger.Factory.create(this::class)
    private val tasks = mutableListOf<AbstractBotTask>()
    fun add(task: AbstractBotTask) {
        tasks.add(task)
    }

    fun registerAll() {
        tasks.forEach {
            val name = it::class.simpleName ?: it::class.jvmName

            val firstTime = it.getNextFirstTime()!!
            val period = it.getMinPeriod()
            var delayTime = firstTime.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            if (delayTime < 0) {
                logger.warning("$name is missed, start now.")
                delayTime=0
            }
            logger.debug( "$name will start at: ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(firstTime)} (delay ${delayTime}s)." )
            logger.debug( "$name will run every $period senconds." )
            scheduleAtFixedRate(it, delayTime*1000, period * 1000L)
        }
    }
}
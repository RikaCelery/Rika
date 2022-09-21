@file:Suppress("unused", "unused", "unused", "unused", "unused", "unused", "unused")

package org.celery.utils.task_controller

import net.mamoe.mirai.utils.MiraiLogger
import org.celery.utils.time.TimeConsts
import org.celery.utils.time.TimeUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors

/**
 * 在指定时刻执行
 */
@Suppress("unused", "unused", "unused", "unused", "unused", "unused", "unused")
abstract class AbstractBotTask(
    private val years: Int? = null,
    private val months: Int? = null,
    private val days: Int? = null,
    private val hours: Int? = null,
    private val minutes: Int? = null,
    private val seconds: Int? = null,
) : TimerTask() {
    constructor() : this(null, null, null, null, null, null)

    /**
     * 运行周期,单位为秒
     */
    open val period: Int? = null

    /**
     * 第一次运行的时间
     */
    open val firstTime: LocalDateTime? = null
    open val logger = MiraiLogger.Factory.create(this::class)

    abstract fun whenTrigger(date: LocalDateTime)

    fun getMinPeriod(): Int {
        val period0 = if (period == null) when {
            seconds != null -> TimeConsts.SEC
            minutes != null -> TimeConsts.MIN
            hours != null -> TimeConsts.HOUR
            days != null -> TimeConsts.DAY
            months != null -> TimeConsts.MON
            years != null -> TimeConsts.YEAR
            else -> {
                logger.warning("无法为${this::class.simpleName}找到合适的运行周期,使用默认值(每分钟)")
                TimeConsts.MIN
            }
        } else period!!
        return period0
    }

    fun getNextFirstTime(): LocalDateTime {
        val time = if (firstTime == null) when {
            seconds != null -> TimeUtils.getStartOfNextSecond()
            minutes != null -> TimeUtils.getStartOfNextMinute()
            hours != null -> TimeUtils.getStartOfNextHour()
            days != null -> TimeUtils.getStartOfNextDay()
            months != null -> TimeUtils.getStartOfNextMonth()
            years != null -> TimeUtils.getStartOfNextYear()
            else -> {
                logger.warning("无法为${this::class.simpleName}找到合适的启动时间,使用默认值(当前时间延后5s)")
                LocalDateTime.now().plusSeconds(5)
            }
        } else firstTime!!

        return time
    }

    /**
     * 判断给定日期是否会触发[whenTrigger]函数,重写该函数时应一并重写[firstTime]和[period]
     */
    open fun isTrigger(date: LocalDateTime): Boolean {
        return (years == null || years == TimeUtils.getNowYear()) && (months == null || months == TimeUtils.getNowMonth()) && (days == null || days == TimeUtils.getNowDay()) && (minutes == null || minutes == TimeUtils.getNowMinute()) && (seconds == null || minutes == TimeUtils.getNowSecond())
    }

    override fun run() {
        val executors = Executors.newCachedThreadPool()
        val date = LocalDateTime.now()
//        logger.debug("now is ${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date)} trigger = ${isTrigger(date)}")
        if (isTrigger(date)) try {
            executors.submit{ whenTrigger(date) }
        } catch (e: Exception) {
            logger.error("在${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date)}处理定时任务时出错", e)
        }
    }
}
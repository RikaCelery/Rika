package com.celery.rika.utils.time

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object TimeUtils {
    /**
     * yyyy:年MM:月dd:日HH:时mm:分ss:秒EEE:Wen
     */
    fun now(format: String): String = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern(format)
    )

    fun now(): String = now("yyyy年MM月dd日HH时mm分ss秒")
    fun nowSeconds(): Int = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8)).toInt()
    fun getNowDay(): Int {
        return Calendar.getInstance()[Calendar.DAY_OF_MONTH]
    }

    fun getNowHour(): Int {
        return Calendar.getInstance()[Calendar.HOUR_OF_DAY]
    }

    fun getNowMinute(): Int {
        return Calendar.getInstance()[Calendar.MINUTE]
    }

    fun getNowMonth(): Int {
        return Calendar.getInstance()[Calendar.MONTH] + 1
    }

    fun getNowSecond(): Int {
        return Calendar.getInstance()[Calendar.SECOND]
    }

    fun getNowYear(): Int {
        return Calendar.getInstance()[Calendar.YEAR]
    }

    fun getStartOfNextYear(): LocalDateTime {

        val localDateTime = LocalDateTime.now().plusYears(1)
        val year = localDateTime.year
        return (LocalDateTime.of(year, 1, 1, 0, 0, 0))
    }

    fun getStartOfNextMonth(): LocalDateTime {

        val localDateTime = LocalDateTime.now().plusMonths(1)
        val year = localDateTime.year
        val month = localDateTime.monthValue
        return (LocalDateTime.of(year, month, 1, 0, 0, 0))
    }

    fun getStartOfNextDay(): LocalDateTime {

        val localDateTime = LocalDateTime.now().plusDays(1)
        val year = localDateTime.year
        val month = localDateTime.monthValue
        val day = localDateTime.dayOfMonth
        return (LocalDateTime.of(year, month, day, 0, 0, 0))
    }

    fun getStartOfNextHour(): LocalDateTime {

        val localDateTime = LocalDateTime.now().plusHours(1)
        val year = localDateTime.year
        val month = localDateTime.monthValue
        val day = localDateTime.dayOfMonth
        val hour = localDateTime.hour
        return (LocalDateTime.of(year, month, day, hour, 0, 0))
    }

    fun getStartOfNextMinute(): LocalDateTime {

        val localDateTime = LocalDateTime.now().plusMinutes(1)
        val year = localDateTime.year
        val month = localDateTime.monthValue
        val day = localDateTime.dayOfMonth
        val hour = localDateTime.hour
        val minute = localDateTime.minute
//        println(LocalDateTime.of(year, month, day, hour, minute, 0).string)
        return (LocalDateTime.of(year, month, day, hour, minute, 0))
    }

    fun getStartOfNextSecond(): LocalDateTime {
        return LocalDateTime.now().plusSeconds(1)
    }

    private val LocalDateTime.seconds: Long
        get() {
            return toEpochSecond(ZoneOffset.ofHours(8))
        }

    private val LocalDateTime.string: String
        get() {
            return format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH时mm分ss秒"))
        }
}

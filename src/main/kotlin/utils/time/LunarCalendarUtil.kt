package utils.time

import java.util.*

class LunarCalendarUtil {
    private val LogFactory: Any? = null

    companion object {
        //农历（阴历）年，月，日
        private var yearCyl = 0
        private var monCyl = 0
        private var dayCyl = 0

        //公历（阳历）年，月，日
        private var year = 0
        private var month = 0
        private var day = 0
        private var isLeap = false
        private val lunarInfo = intArrayOf(
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5,
            0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, 0x04ae0,
            0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2,
            0x095b0, 0x14977, 0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40,
            0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, 0x06566, 0x0d4a0,
            0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7,
            0x0c950, 0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0,
            0x092d0, 0x0d2b2, 0x0a950, 0x0b557, 0x06ca0, 0x0b550, 0x15355,
            0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263,
            0x0d950, 0x05b57, 0x056a0, 0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0,
            0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6, 0x095b0,
            0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46,
            0x0ab60, 0x09570, 0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50,
            0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0, 0x0c960, 0x0d954,
            0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0,
            0x0cab5, 0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0,
            0x0a5b0, 0x15176, 0x052b0, 0x0a930, 0x07954, 0x06aa0, 0x0ad50,
            0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6,
            0x0d250, 0x0d520, 0x0dd45, 0x0b5a0, 0x056d0, 0x055b2, 0x049b0,
            0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0
        )
        private val solarMonth = intArrayOf(
            31, 28, 31, 30, 31, 30, 31, 31, 30, 31,
            30, 31
        )
        private val Gan = arrayOf(
            "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛",
            "壬", "癸"
        )
        private val Zhi = arrayOf(
            "子", "丑", "寅", "卯", "辰", "巳", "午", "未",
            "申", "酉", "戌", "亥"
        )
        private val Animals = arrayOf(
            "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊",
            "猴", "鸡", "狗", "猪"
        )
        private val sTermInfo = intArrayOf(
            0, 21208, 42467, 63836, 85337, 107014,
            128867, 150921, 173149, 195551, 218072, 240693, 263343, 285989,
            308563, 331033, 353350, 375494, 397447, 419210, 440795, 462224,
            483532, 504758
        )
        private val nStr1 = arrayOf(
            "日", "一", "二", "三", "四", "五", "六", "七",
            "八", "九", "十"
        )
        private val nStr2 = arrayOf("初", "十", "廿", "卅", "　")
        private val monthNong = arrayOf(
            "", "正", "二", "三", "四", "五", "六", "七",
            "八", "九", "十", "冬", "腊"
        )
        private val yearName = arrayOf(
            "零", "壹", "贰", "叁", "肆", "伍", "陆",
            "柒", "捌", "玖"
        )

        /**
         * 传回农历 y年的总天数
         * @param y
         * @return
         */
        private fun lYearDays(y: Int): Int {
            var i: Int
            var sum = 348 //29*12
            i = 0x8000
            while (i > 0x8) {
                sum += if (lunarInfo[y - 1900] and i == 0) 0 else 1 //大月+1天
                i = i shr 1
            }
            return sum + leapDays(y) //+闰月的天数
        }

        /**
         * 传回农历 y年闰月的天数
         * @param y
         * @return
         */
        private fun leapDays(y: Int): Int {
            return if (leapMonth(y) != 0) {
                if (lunarInfo[y - 1900] and 0x10000 == 0) 29 else 30
            } else {
                0
            }
        }

        /**
         * 传回农历 y年闰哪个月 1-12 , 没闰传回 0
         * @param y
         * @return
         */
        private fun leapMonth(y: Int): Int {
            return lunarInfo[y - 1900] and 0xf
        }

        /**
         * 传回农历 y年m月的总天数
         * @param y
         * @param m
         * @return
         */
        private fun monthDays(y: Int, m: Int): Int {
            return if (lunarInfo[y - 1900] and (0x10000 shr m) == 0) 29 else 30
        }

        /**
         * 算出农历, 传入日期物件, 传回农历日期物件
         * 该物件属性有 .year .month .day .isLeap .yearCyl .dayCyl .monCyl
         * @param objDate
         */
        private fun Lunar1(objDate: Date) {
            var i: Int
            var leap = 0
            var temp = 0
            val cl = Calendar.getInstance()
            cl[1900, 0] = 31 //1900-01-31是农历1900年正月初一
            val baseDate = cl.time
            //1900-01-31是农历1900年正月初一
            var offset = ((objDate.time - baseDate.time) / 86400000).toInt() //天数(86400000=24*60*60*1000)
            //1899-12-21是农历1899年腊月甲子日
            dayCyl = offset + 40
            //1898-10-01是农历甲子月
            monCyl = 14
            //得到年数
            i = 1900
            while (i < 2050 && offset > 0) {

                //农历每年天数
                temp = lYearDays(i)
                offset -= temp
                monCyl += 12
                i++
            }
            if (offset < 0) {
                offset += temp
                i--
                monCyl -= 12
            }
            year = i //农历年份
            yearCyl = i - 1864 //1864年是甲子年
            leap = leapMonth(i) //闰哪个月
            isLeap = false
            i = 1
            while (i < 13 && offset > 0) {

                //闰月
                if (leap > 0 && i == leap + 1 && isLeap == false) {
                    --i
                    isLeap = true
                    temp = leapDays(year)
                } else {
                    temp = monthDays(year, i)
                }
                //解除闰月
                if (isLeap == true && i == leap + 1) {
                    isLeap = false
                }
                offset -= temp
                if (isLeap == false) {
                    monCyl++
                }
                i++
            }
            if (offset == 0 && leap > 0 && i == leap + 1) {
                if (isLeap) {
                    isLeap = false
                } else {
                    isLeap = true
                    --i
                    --monCyl
                }
            }
            if (offset < 0) {
                offset += temp
                --i
                --monCyl
            }
            month = i //农历月份
            day = offset + 1 //农历天份
        }

        /**
         * 传入 offset 传回干支, 0=甲子
         * @param num
         * @return
         */
        private fun cyclical(num: Int): String {
            return Gan[num % 10] + Zhi[num % 12]
        }

        /**
         * 中文日期
         * @param d
         * @return
         */
        private fun cDay(d: Int): String {
            var s: String
            when (d) {
                10 -> s = "初十"
                20 -> s = "二十"
                30 -> s = "三十"
                else -> {
                    s = nStr2[(d / 10)] //取商
                    s += nStr1[d % 10] //取余
                }
            }
            return s
        }

        private fun cYear(y: Int): String {
            var y = y
            var s = " "
            var d: Int
            while (y > 0) {
                d = y % 10
                y = (y - d) / 10
                s = yearName[d] + s
            }
            return s
        }

        private fun getYear(): Int {
            return year
        }

        private fun getMonth(): Int {
            return month
        }

        private fun getDay(): Int {
            return day
        }

        private fun getMonCyl(): Int {
            return monCyl
        }

        private fun getYearCyl(): Int {
            return yearCyl
        }

        private fun getDayCyl(): Int {
            return dayCyl
        }

        private fun getIsLeap(): Boolean {
            return isLeap
        }

        /**
         * 获取-农历详细日期
         * @param year
         * @param month
         * @param day
         * @return
         */
        fun getLunarDetails(year: String, month: String, day: String): String {
            val sDObj: Date
            var s: String
            val SY: Int
            val SM: Int
            val SD: Int
            val sy: Int
            SY = year.toInt()
            SM = month.toInt()
            SD = day.toInt()
            sy = (SY - 4) % 12
            val cl = Calendar.getInstance()
            cl[SY, SM - 1] = SD
            sDObj = cl.time
            //日期
            Lunar1(sDObj) //农历
            val lMDBuffer = StringBuffer()
            lMDBuffer.append("农历")
            lMDBuffer.append("【")
            lMDBuffer.append(Animals[sy])
            lMDBuffer.append("】")
            lMDBuffer.append(cYear(getYear()))
            lMDBuffer.append("年 ")
            lMDBuffer.append(if (getIsLeap()) "闰" else "")
            lMDBuffer.append(monthNong[getMonth()])
            lMDBuffer.append("月")
            lMDBuffer.append(if (monthDays(getYear(), getMonth()) == 29) "小" else "大")
            lMDBuffer.append(cDay(getDay()))
            lMDBuffer.append(" ")
            lMDBuffer.append(cyclical(getYearCyl()))
            lMDBuffer.append("年")
            lMDBuffer.append(cyclical(getMonCyl()))
            lMDBuffer.append("月")
            lMDBuffer.append(cyclical(getDayCyl()))
            lMDBuffer.append("日")
            return lMDBuffer.toString()
        }

        /**
         * 获取-农历年月日
         * @param year
         * @param month
         * @param day
         * @return
         */
        fun getLunarYearMonthDay(year: String, month: String, day: String): String {
            val sDObj: Date
            val SY: Int
            val SM: Int
            val SD: Int
            val sy: Int
            SY = year.toInt()
            SM = month.toInt()
            SD = day.toInt()
            sy = (SY - 4) % 12
            val cl = Calendar.getInstance()
            cl[SY, SM - 1] = SD
            sDObj = cl.time
            //日期
            Lunar1(sDObj) //农历
            val lMDBuffer = StringBuffer()
            lMDBuffer.append("农历")
            lMDBuffer.append(cyclical(getYearCyl()))
            lMDBuffer.append("(")
            lMDBuffer.append(Animals[sy])
            lMDBuffer.append(")年")
            lMDBuffer.append(monthNong[getMonth()])
            lMDBuffer.append("月")
            lMDBuffer.append(cDay(getDay()))
            return lMDBuffer.toString()
        }

        /**
         * 获取-农历月日
         * @param year
         * @param month
         * @param day
         * @return
         */
        fun getLunarMonthDay(year: String, month: String, day: String): String {
            val sDObj: Date
            var s: String
            val SY: Int
            val SM: Int
            val SD: Int
            SY = year.toInt()
            SM = month.toInt()
            SD = day.toInt()
            val cl = Calendar.getInstance()
            cl[SY, SM - 1] = SD
            sDObj = cl.time
            //日期
            Lunar1(sDObj) //农历
            val lMDBuffer = StringBuffer()
            lMDBuffer.append(monthNong[getMonth()])
            lMDBuffer.append("月")
            lMDBuffer.append(cDay(getDay()))
            return lMDBuffer.toString()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            println(getLunarDetails("1990", "12", "22"))
            println(getLunarDetails("2019", "1", "22"))
            println(getLunarDetails("2019", "2", "10"))
            println(getLunarDetails("2020", "03", "03"))
            println(getLunarYearMonthDay("1990", "12", "22"))
            println(getLunarYearMonthDay("2019", "1", "22"))
            println(getLunarYearMonthDay("2019", "2", "10"))
            println(getLunarYearMonthDay("2020", "03", "03"))
            println(getLunarMonthDay("1990", "12", "22"))
            println(getLunarMonthDay("2019", "1", "22"))
            println(getLunarMonthDay("2019", "2", "10"))
            println(getLunarMonthDay("2020", "03", "03"))
        }
    }
}
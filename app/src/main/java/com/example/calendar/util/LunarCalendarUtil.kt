package com.example.calendar.util

import java.util.Calendar

/**
 * 农历计算工具类
 * 实现离线农历日期转换
 */
object LunarCalendarUtil {
    
    // 农历数据表 (1900-2100年)
    private val lunarInfo = longArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520
    )
    
    // 农历月份名称
    private val lunarMonthName = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    
    // 农历日期名称
    private val lunarDayName = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    
    // 天干
    private val tianGan = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    
    // 地支
    private val diZhi = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    
    // 生肖
    private val shengXiao = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    
    /**
     * 获取农历年份的总天数
     */
    private fun getLunarYearDays(year: Int): Int {
        var sum = 348
        var i = 0x8000
        while (i > 0x8) {
            sum += if (lunarInfo[year - 1900] and i.toLong() != 0L) 1 else 0
            i = i shr 1
        }
        return sum + getLeapMonthDays(year)
    }
    
    /**
     * 获取农历年份闰月的天数
     */
    private fun getLeapMonthDays(year: Int): Int {
        return if (getLeapMonth(year) != 0) {
            if (lunarInfo[year - 1900] and 0x10000 != 0L) 30 else 29
        } else 0
    }
    
    /**
     * 获取农历年份的闰月月份 (0表示无闰月)
     */
    fun getLeapMonth(year: Int): Int {
        return (lunarInfo[year - 1900] and 0xf).toInt()
    }
    
    /**
     * 获取农历某月的天数
     */
    private fun getLunarMonthDays(year: Int, month: Int): Int {
        return if (lunarInfo[year - 1900] and (0x10000 shr month).toLong() != 0L) 30 else 29
    }
    
    /**
     * 公历转农历
     * @return LunarDate 农历日期对象
     */
    fun solarToLunar(year: Int, month: Int, day: Int): LunarDate {
        val baseDate = Calendar.getInstance().apply {
            set(1900, 0, 31)
        }
        val targetDate = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        
        var offset = ((targetDate.timeInMillis - baseDate.timeInMillis) / 86400000).toInt()
        
        var lunarYear = 1900
        var lunarMonth: Int
        var lunarDay: Int
        var isLeap = false
        
        var daysInYear: Int
        while (lunarYear < 2101 && offset > 0) {
            daysInYear = getLunarYearDays(lunarYear)
            if (offset < daysInYear) break
            offset -= daysInYear
            lunarYear++
        }
        
        val leapMonth = getLeapMonth(lunarYear)
        var daysInMonth: Int
        lunarMonth = 1
        while (lunarMonth <= 12) {
            if (leapMonth > 0 && lunarMonth == leapMonth + 1 && !isLeap) {
                lunarMonth--
                isLeap = true
                daysInMonth = getLeapMonthDays(lunarYear)
            } else {
                daysInMonth = getLunarMonthDays(lunarYear, lunarMonth)
            }
            
            if (offset < daysInMonth) break
            offset -= daysInMonth
            
            if (isLeap && lunarMonth == leapMonth + 1) isLeap = false
            lunarMonth++
        }
        
        lunarDay = offset + 1
        
        return LunarDate(
            year = lunarYear,
            month = lunarMonth,
            day = lunarDay,
            isLeapMonth = isLeap
        )
    }
    
    /**
     * 获取干支年
     */
    fun getGanZhiYear(year: Int): String {
        val ganIndex = (year - 4) % 10
        val zhiIndex = (year - 4) % 12
        return tianGan[ganIndex] + diZhi[zhiIndex]
    }
    
    /**
     * 获取生肖
     */
    fun getShengXiao(year: Int): String {
        return shengXiao[(year - 4) % 12]
    }
    
    /**
     * 获取农历日期的中文表示（简化版，只显示农历日）
     */
    fun getLunarDayText(lunarDate: LunarDate): String {
        return if (lunarDate.day == 1) {
            // 每月初一显示月份
            (if (lunarDate.isLeapMonth) "闰" else "") + lunarMonthName[lunarDate.month - 1] + "月"
        } else {
            lunarDayName[lunarDate.day - 1]
        }
    }
    
    /**
     * 获取农历日期文本（简化版）
     * 只返回农历日期，不包含节假日
     */
    fun getSimpleLunarText(year: Int, month: Int, day: Int): String {
        val lunarDate = solarToLunar(year, month, day)
        return getLunarDayText(lunarDate)
    }
    
    /**
     * 获取完整农历日期描述（年月日）
     * 格式：甲子年 鼠年 正月初一
     */
    fun getFullLunarText(year: Int, month: Int, day: Int): String {
        val lunarDate = solarToLunar(year, month, day)
        val ganZhi = getGanZhiYear(lunarDate.year)
        val sx = getShengXiao(lunarDate.year)
        val monthStr = (if (lunarDate.isLeapMonth) "闰" else "") + lunarMonthName[lunarDate.month - 1] + "月"
        val dayStr = lunarDayName[lunarDate.day - 1]
        return "${ganZhi}年 ${sx}年 $monthStr$dayStr"
    }
    
    /**
     * 获取农历月日描述
     * 格式：正月初一
     */
    fun getLunarMonthDayText(year: Int, month: Int, day: Int): String {
        val lunarDate = solarToLunar(year, month, day)
        val monthStr = (if (lunarDate.isLeapMonth) "闰" else "") + lunarMonthName[lunarDate.month - 1] + "月"
        val dayStr = lunarDayName[lunarDate.day - 1]
        return "$monthStr$dayStr"
    }
}

/**
 * 农历日期数据类
 */
data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false
)

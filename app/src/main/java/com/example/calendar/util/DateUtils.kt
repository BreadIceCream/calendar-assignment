package com.example.calendar.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期工具类
 */
object DateUtils {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy年M月", Locale.CHINESE)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.CHINESE)
    
    /**
     * 获取今天的开始时间戳 (00:00:00)
     */
    fun getStartOfDay(date: Date = Date()): Long {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * 获取某天的结束时间戳 (23:59:59.999)
     */
    fun getEndOfDay(date: Date = Date()): Long {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    
    /**
     * 获取某天开始时间戳
     */
    fun getStartOfDay(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * 获取某天结束时间戳
     */
    fun getEndOfDay(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    
    /**
     * 获取某月的开始时间戳
     */
    fun getStartOfMonth(year: Int, month: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * 获取某月的结束时间戳
     */
    fun getEndOfMonth(year: Int, month: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, 1, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    
    /**
     * 获取某周的开始时间戳 (周日)
     */
    fun getStartOfWeek(date: Date = Date()): Long {
        return Calendar.getInstance().apply {
            time = date
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * 获取某周的结束时间戳 (周六)
     */
    fun getEndOfWeek(date: Date = Date()): Long {
        return Calendar.getInstance().apply {
            time = date
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    
    /**
     * 获取某月有多少天
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    /**
     * 获取某月第一天是星期几 (0=周日, 1=周一, ..., 6=周六)
     * 用于以周日为起始日的日历
     */
    fun getFirstDayOfWeekInMonthSundayStart(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY = 1, 转为 0
        return dayOfWeek - 1
    }
    
    /**
     * 获取某月第一天是星期几 (1=周一, 7=周日)
     * 用于以周一为起始日的日历
     */
    fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // 将周日(1)转为7, 其他减1
        return if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
    }
    
    /**
     * 计算月视图需要的行数（周日为起始日）
     */
    fun getMonthViewRows(year: Int, month: Int): Int {
        val daysInMonth = getDaysInMonth(year, month)
        val firstDayOfWeek = getFirstDayOfWeekInMonthSundayStart(year, month)
        val totalCells = firstDayOfWeek + daysInMonth
        return (totalCells + 6) / 7 // 向上取整
    }
    
    /**
     * 格式化日期
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化日期时间
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化月份显示
     */
    fun formatMonth(year: Int, month: Int): String {
        return Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }.let { monthFormat.format(it.time) }
    }
    
    /**
     * 获取星期几文本
     */
    fun getDayOfWeekText(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    }
    
    /**
     * 获取星期几文本（周日起始）
     */
    fun getDayOfWeekTextSundayStart(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            0 -> "周日"
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            else -> ""
        }
    }
    
    /**
     * 判断是否是今天
     */
    fun isToday(year: Int, month: Int, day: Int): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == year &&
                today.get(Calendar.MONTH) == month - 1 &&
                today.get(Calendar.DAY_OF_MONTH) == day
    }
    
    /**
     * 判断是否是周末
     */
    fun isWeekend(year: Int, month: Int, day: Int): Boolean {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    /**
     * 判断是否是周末（周日起始模式，返回周日和周六）
     */
    fun isWeekendSundayStart(year: Int, month: Int, day: Int): Boolean {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    /**
     * 获取今天的年月日
     */
    fun getToday(): Triple<Int, Int, Int> {
        val calendar = Calendar.getInstance()
        return Triple(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    /**
     * 计算时间差描述
     */
    fun getTimeDiffDescription(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = timestamp - now
        
        return when {
            diff < 0 -> "已过期"
            diff < 60 * 1000 -> "即将开始"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟后"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时后"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天后"
            else -> formatDate(timestamp)
        }
    }
}

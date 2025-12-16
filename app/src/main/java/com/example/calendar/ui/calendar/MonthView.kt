package com.example.calendar.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.util.DateUtils

/**
 * 月视图组件
 * 动态计算行数，以周日为起始日
 */
@Composable
fun MonthView(
    year: Int,
    month: Int,
    selectedDate: Triple<Int, Int, Int>,
    events: List<CalendarEvent>,  // 直接传入事件列表，确保响应式更新
    onDateSelected: (Int, Int, Int) -> Unit,
    onDateClick: ((Int, Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 使用 remember 缓存日期计算结果
    val days = remember(year, month) {
        calculateMonthDays(year, month)
    }
    
    val numRows = remember(year, month) {
        DateUtils.getMonthViewRows(year, month)
    }
    
    Column(modifier = modifier) {
        // 星期标题 (周日 一 二 三 四 五 六)
        WeekdayHeader()
        
        // 日历网格 - 动态行数
        Column(modifier = Modifier.fillMaxWidth()) {
            for (rowIndex in 0 until numRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (colIndex in 0 until 7) {
                        val cellIndex = rowIndex * 7 + colIndex
                        if (cellIndex < days.size) {
                            val dayInfo = days[cellIndex]
                            val isSelected = dayInfo.year == selectedDate.first &&
                                    dayInfo.month == selectedDate.second &&
                                    dayInfo.day == selectedDate.third
                            
                            // 直接在这里计算 hasEvents，使用传入的 events 列表
                            val startTime = DateUtils.getStartOfDay(dayInfo.year, dayInfo.month, dayInfo.day)
                            val endTime = DateUtils.getEndOfDay(dayInfo.year, dayInfo.month, dayInfo.day)
                            val hasEvents = events.any { event ->
                                event.startTime <= endTime && event.endTime >= startTime
                            }
                            val eventCount = events.count { event ->
                                event.startTime <= endTime && event.endTime >= startTime
                            }
                            
                            DayCell(
                                year = dayInfo.year,
                                month = dayInfo.month,
                                day = dayInfo.day,
                                isCurrentMonth = dayInfo.isCurrentMonth,
                                isSelected = isSelected,
                                hasEvents = hasEvents,
                                eventCount = eventCount,
                                onClick = {
                                    // 如果有 onDateClick 回调且是当前月，则跳转日视图
                                    if (onDateClick != null && dayInfo.isCurrentMonth) {
                                        onDateClick(dayInfo.year, dayInfo.month, dayInfo.day)
                                    } else {
                                        onDateSelected(dayInfo.year, dayInfo.month, dayInfo.day)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // 空白占位
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 计算月视图需要显示的所有日期（周日起始）
 */
private fun calculateMonthDays(year: Int, month: Int): List<DayInfo> {
    val days = mutableListOf<DayInfo>()
    
    val daysInMonth = DateUtils.getDaysInMonth(year, month)
    // 获取当月1号是星期几 (0=周日, 1=周一, ..., 6=周六)
    val firstDayOfWeek = DateUtils.getFirstDayOfWeekInMonthSundayStart(year, month)
    
    // 计算上个月
    val prevMonth = if (month == 1) 12 else month - 1
    val prevYear = if (month == 1) year - 1 else year
    val daysInPrevMonth = DateUtils.getDaysInMonth(prevYear, prevMonth)
    
    // 计算下个月
    val nextMonth = if (month == 12) 1 else month + 1
    val nextYear = if (month == 12) year + 1 else year
    
    // 添加上个月的日期 (填充开头)
    for (i in 0 until firstDayOfWeek) {
        val day = daysInPrevMonth - (firstDayOfWeek - 1 - i)
        days.add(DayInfo(prevYear, prevMonth, day, isCurrentMonth = false))
    }
    
    // 添加当前月的所有日期
    for (day in 1..daysInMonth) {
        days.add(DayInfo(year, month, day, isCurrentMonth = true))
    }
    
    // 添加下个月的日期 (填充结尾，凑成完整的行)
    val numRows = DateUtils.getMonthViewRows(year, month)
    val totalCells = numRows * 7
    var nextDay = 1
    while (days.size < totalCells) {
        days.add(DayInfo(nextYear, nextMonth, nextDay++, isCurrentMonth = false))
    }
    
    return days
}

/**
 * 日期信息数据类
 */
private data class DayInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val isCurrentMonth: Boolean
)

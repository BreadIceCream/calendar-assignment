package com.example.calendar.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.ui.theme.TodayBackground
import com.example.calendar.ui.theme.WeekendText
import com.example.calendar.ui.theme.hexToColor
import com.example.calendar.util.DateUtils
import com.example.calendar.util.LunarCalendarUtil
import java.util.Calendar

/**
 * 周视图组件
 * 横向展示一周（周日起始），纵向展示时间轴
 */
@Composable
fun WeekView(
    year: Int,
    month: Int,
    day: Int,
    events: List<CalendarEvent>,
    onEventClick: (String) -> Unit,
    onDateClick: ((Int, Int, Int) -> Unit)? = null,  // 点击日期回调
    selectedDate: Triple<Int, Int, Int>? = null,     // 当前选中日期
    modifier: Modifier = Modifier
) {
    val hourHeight = 60.dp
    val timeColumnWidth = 50.dp
    val dayColumnWidth = 100.dp
    
    // 获取当前周的日期（周日起始）
    val weekDates = remember(year, month, day) {
        getWeekDatesSundayStart(year, month, day)
    }
    
    // 分离全天日程
    val allDayEvents = remember(events) {
        events.filter { it.isAllDay }
    }
    
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    Column(modifier = modifier.fillMaxSize()) {
        // 星期标题行 (固定) - 周日起始
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 空白角落
            Spacer(modifier = Modifier.width(timeColumnWidth))
            
            // 日期标题
            Row(
                modifier = Modifier.horizontalScroll(horizontalScrollState)
            ) {
                weekDates.forEachIndexed { index, date ->
                    val isToday = DateUtils.isToday(date.first, date.second, date.third)
                    val isSelected = selectedDate?.let { 
                        it.first == date.first && it.second == date.second && it.third == date.third 
                    } ?: (date.first == year && date.second == month && date.third == day)
                    // 周日(index=0)和周六(index=6)为周末
                    val isWeekend = index == 0 || index == 6
                    
                    // 获取农历
                    val lunarText = LunarCalendarUtil.getLunarMonthDayText(date.first, date.second, date.third)
                    
                    // 背景和文字颜色
                    val bgColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> TodayBackground
                        else -> Color.Transparent
                    }
                    val primaryTextColor = when {
                        isSelected -> Color.White
                        isToday -> MaterialTheme.colorScheme.primary
                        isWeekend -> WeekendText
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val secondaryTextColor = when {
                        isSelected -> Color.White.copy(alpha = 0.8f)
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        isWeekend -> WeekendText.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                    
                    // 使用圆角矩形背景而非圆形，以容纳所有文字
                    Column(
                        modifier = Modifier
                            .width(dayColumnWidth)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .clickable { 
                                onDateClick?.invoke(date.first, date.second, date.third)
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 星期几
                        Text(
                            text = DateUtils.getDayOfWeekTextSundayStart(index),
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                        
                        // 日期数字
                        Text(
                            text = "${date.third}",
                            fontSize = 18.sp,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = primaryTextColor
                        )
                        
                        // 农历日期
                        Text(
                            text = lunarText,
                            fontSize = 9.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
        
        
        // 全天日程横幅区域
        if (allDayEvents.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp)
            ) {
                Spacer(modifier = Modifier.width(timeColumnWidth))
                
                Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    weekDates.forEach { date ->
                        val dayAllDayEvents = allDayEvents.filter { event ->
                            val startOfDay = DateUtils.getStartOfDay(date.first, date.second, date.third)
                            val endOfDay = DateUtils.getEndOfDay(date.first, date.second, date.third)
                            event.startTime <= endOfDay && event.endTime >= startOfDay
                        }
                        
                        Column(
                            modifier = Modifier
                                .width(dayColumnWidth)
                                .padding(horizontal = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            dayAllDayEvents.take(2).forEach { event ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(hexToColor(event.color).copy(alpha = 0.8f))
                                        .clickable { onEventClick(event.id) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = event.title,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (dayAllDayEvents.size > 2) {
                                Text(
                                    text = "+${dayAllDayEvents.size - 2}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 时间轴和日程区域
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // 时间列 (固定)
            Column(
                modifier = Modifier.width(timeColumnWidth)
            ) {
                for (hour in 0..23) {
                    Box(
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = String.format("%02d:00", hour),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
            
            // 日程区域
            Row(
                modifier = Modifier.horizontalScroll(horizontalScrollState)
            ) {
                weekDates.forEach { date ->
                    DayColumn(
                        year = date.first,
                        month = date.second,
                        day = date.third,
                        events = events.filter { event ->
                            if (event.isAllDay) return@filter false // 全天日程已经在上方显示
                            val startOfDay = DateUtils.getStartOfDay(date.first, date.second, date.third)
                            val endOfDay = DateUtils.getEndOfDay(date.first, date.second, date.third)
                            (event.startTime >= startOfDay && event.startTime < endOfDay) ||
                            (event.endTime > startOfDay && event.endTime <= endOfDay) ||
                            (event.startTime < startOfDay && event.endTime > endOfDay)
                        },
                        hourHeight = hourHeight,
                        width = dayColumnWidth,
                        onEventClick = onEventClick
                    )
                }
            }
        }
    }
}

/**
 * 单日列 (用于周视图)
 */
@Composable
private fun DayColumn(
    year: Int,
    month: Int,
    day: Int,
    events: List<CalendarEvent>,
    hourHeight: Dp,
    width: Dp,
    onEventClick: (String) -> Unit
) {
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val totalHeight = hourHeight * 24
    
    val startOfDay = DateUtils.getStartOfDay(year, month, day)
    
    Box(
        modifier = Modifier
            .width(width)
            .height(totalHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 网格线
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 水平线 (小时分隔)
            for (hour in 0..24) {
                val y = hour * hourHeightPx
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            // 垂直边界线
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height),
                strokeWidth = 1f
            )
        }
        
        // 日程块
        events.forEach { event ->
            val eventStartY = ((event.startTime - startOfDay).coerceAtLeast(0) / (1000 * 60 * 60f)) * hourHeightPx
            val eventEndY = ((event.endTime - startOfDay).coerceAtMost(24 * 60 * 60 * 1000L) / (1000 * 60 * 60f)) * hourHeightPx
            val eventHeight = (eventEndY - eventStartY).coerceAtLeast(hourHeightPx / 2)
            
            EventBlock(
                event = event,
                modifier = Modifier
                    .offset(y = with(density) { eventStartY.toDp() })
                    .padding(horizontal = 2.dp, vertical = 1.dp)
                    .width(width - 4.dp)
                    .height(with(density) { eventHeight.toDp() }),
                onClick = { onEventClick(event.id) }
            )
        }
    }
}

/**
 * 日程块组件
 */
@Composable
private fun EventBlock(
    event: CalendarEvent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val eventColor = hexToColor(event.color)
    
    Box(
        modifier = modifier
            .background(
                color = eventColor.copy(alpha = 0.8f),
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(4.dp)
    ) {
        Column {
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!event.location.isNullOrBlank()) {
                Text(
                    text = event.location,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 获取某天所在周的所有日期（周日起始）
 */
private fun getWeekDatesSundayStart(year: Int, month: Int, day: Int): List<Triple<Int, Int, Int>> {
    val calendar = Calendar.getInstance().apply {
        set(year, month - 1, day)
        firstDayOfWeek = Calendar.SUNDAY
    }
    
    // 找到周日
    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    
    return (0..6).map {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        Triple(y, m, d)
    }
}

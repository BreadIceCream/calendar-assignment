package com.example.calendar.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.ui.theme.hexToColor
import com.example.calendar.util.DateUtils
import com.example.calendar.util.LunarCalendarUtil

/**
 * 日视图组件
 * 详细展示选中日期的完整时间轴
 */
@Composable
fun DayView(
    year: Int,
    month: Int,
    day: Int,
    events: List<CalendarEvent>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hourHeight = 60.dp
    val timeColumnWidth = 60.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    
    val scrollState = rememberScrollState()
    val startOfDay = DateUtils.getStartOfDay(year, month, day)
    
    // 判断是否是今天，用于显示当前时间红线
    val isToday = DateUtils.isToday(year, month, day)
    val currentTimePosition = if (isToday) {
        val calendar = java.util.Calendar.getInstance()
        val hours = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(java.util.Calendar.MINUTE)
        (hours + minutes / 60f) * hourHeightPx
    } else 0f
    
    // 获取完整农历信息
    val fullLunarText = remember(year, month, day) {
        LunarCalendarUtil.getFullLunarText(year, month, day)
    }
    
    // 分离全天日程和非全天日程
    val allDayEvents = remember(events) {
        events.filter { it.isAllDay }
    }
    val timedEvents = remember(events) {
        events.filter { !it.isAllDay }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 日期头部信息 (白色背景)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // 公历日期
                Text(
                    text = "${month}月${day}日",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 完整农历信息（年月日）
                Text(
                    text = fullLunarText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // 全天日程横幅区域
        if (allDayEvents.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "全天",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                allDayEvents.forEach { event ->
                    AllDayEventBanner(
                        event = event,
                        onClick = { onEventClick(event.id) }
                    )
                }
            }
        }
        
        // 时间轴和日程
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 时间列
            Column(modifier = Modifier.width(timeColumnWidth)) {
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
                            modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
            
            // 日程区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(hourHeight * 24)
            ) {
                // 网格线
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (hour in 0..24) {
                        val y = hour * hourHeightPx
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }
                    
                    // 当前时间红线（仅当天显示）
                    if (isToday && currentTimePosition > 0) {
                        // 红点
                        drawCircle(
                            color = Color.Red,
                            radius = 5f,
                            center = androidx.compose.ui.geometry.Offset(0f, currentTimePosition)
                        )
                        // 红线
                        drawLine(
                            color = Color.Red,
                            start = androidx.compose.ui.geometry.Offset(0f, currentTimePosition),
                            end = androidx.compose.ui.geometry.Offset(size.width, currentTimePosition),
                            strokeWidth = 2f
                        )
                    }
                }
                
                // 非全天日程块 (处理重叠)
                val eventLayouts = calculateEventLayouts(timedEvents, startOfDay)
                
                eventLayouts.forEach { layout ->
                    val event = layout.event
                    val eventStartY = layout.startY * hourHeightPx
                    val eventHeight = (layout.endY - layout.startY) * hourHeightPx
                    
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(density) { (layout.left * 100).dp },
                                y = with(density) { eventStartY.toDp() }
                            )
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .width(with(density) { (layout.width * 100).dp } - 4.dp)
                            .height(with(density) { eventHeight.toDp().coerceAtLeast(30.dp) })
                            .background(
                                color = hexToColor(event.color).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onEventClick(event.id) })
                            }
                            .padding(6.dp)
                    ) {
                        Column {
                            Text(
                                text = event.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Text(
                                text = "${DateUtils.formatTime(event.startTime)} - ${DateUtils.formatTime(event.endTime)}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            
                            if (!event.location.isNullOrBlank()) {
                                Text(
                                    text = event.location,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 全天日程横幅
 */
@Composable
private fun AllDayEventBanner(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(hexToColor(event.color).copy(alpha = 0.85f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (!event.location.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = event.location,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 日程布局信息
 */
private data class EventLayout(
    val event: CalendarEvent,
    val startY: Float,    // 开始位置 (小时)
    val endY: Float,      // 结束位置 (小时)
    val left: Float,      // 左偏移 (0-1)
    val width: Float      // 宽度 (0-1)
)

/**
 * 计算日程布局 (处理重叠)
 */
private fun calculateEventLayouts(
    events: List<CalendarEvent>,
    startOfDay: Long
): List<EventLayout> {
    if (events.isEmpty()) return emptyList()
    
    // 按开始时间排序
    val sortedEvents = events.sortedBy { it.startTime }
    
    val layouts = mutableListOf<EventLayout>()
    val columns = mutableListOf<MutableList<CalendarEvent>>()
    
    sortedEvents.forEach { event ->
        val startY = ((event.startTime - startOfDay).coerceAtLeast(0) / (1000f * 60 * 60))
        val endY = ((event.endTime - startOfDay).coerceAtMost(24 * 60 * 60 * 1000L) / (1000f * 60 * 60))
        
        // 找到可以放置的列
        var placed = false
        for (column in columns) {
            val lastEvent = column.lastOrNull()
            if (lastEvent == null || lastEvent.endTime <= event.startTime) {
                column.add(event)
                placed = true
                break
            }
        }
        
        if (!placed) {
            columns.add(mutableListOf(event))
        }
    }
    
    // 计算每个事件的布局
    val numColumns = columns.size
    columns.forEachIndexed { colIndex, column ->
        column.forEach { event ->
            val startY = ((event.startTime - startOfDay).coerceAtLeast(0) / (1000f * 60 * 60))
            val endY = ((event.endTime - startOfDay).coerceAtMost(24 * 60 * 60 * 1000L) / (1000f * 60 * 60))
            
            layouts.add(
                EventLayout(
                    event = event,
                    startY = startY,
                    endY = endY,
                    left = colIndex.toFloat() / numColumns,
                    width = 1f / numColumns
                )
            )
        }
    }
    
    return layouts
}

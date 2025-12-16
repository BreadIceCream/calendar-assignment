package com.example.calendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.ui.theme.*
import com.example.calendar.util.DateUtils
import com.example.calendar.util.LunarCalendarUtil

/**
 * 日期单元格组件
 * 用于月视图中的每一天
 */
@Composable
fun DayCell(
    year: Int,
    month: Int,
    day: Int,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    hasEvents: Boolean,
    eventCount: Int = 0,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isToday = DateUtils.isToday(year, month, day)
    val isWeekend = DateUtils.isWeekendSundayStart(year, month, day)
    
    // 获取农历日期文本（简化版，只显示农历）
    val lunarText = LunarCalendarUtil.getSimpleLunarText(year, month, day)
    
    // 背景颜色 (只用于日期和农历部分)
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> TodayBackground
        else -> Color.Transparent
    }
    
    // 主文本颜色
    val primaryTextColor = when {
        isSelected -> Color.White
        isToday -> TodayText
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isWeekend -> WeekendText
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    // 农历文本颜色
    val lunarTextColor = when {
        isSelected -> Color.White.copy(alpha = 0.8f)
        isToday -> TodayText.copy(alpha = 0.8f)
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        else -> LunarText
    }

    // 外层容器
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable(enabled = isCurrentMonth) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 日期和农历部分 - 带背景色，紧凑布局
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 4.dp, vertical = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 公历日期 - 紧凑行高
                Text(
                    text = day.toString(),
                    color = primaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                
                // 农历日期 - 紧凑行高，无额外间距
                Text(
                    text = lunarText,
                    color = lunarTextColor,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 10.sp
                )
            }
            
            // 日程灰色圆点提示
            Spacer(modifier = Modifier.height(1.dp))
            
            if (hasEvents) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (!isCurrentMonth) Color.Gray.copy(alpha = 0.4f)
                            else Color.Gray
                        )
                )
            } else {
                // 占位保持布局一致
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

/**
 * 星期标题行 - 周日开始
 */
@Composable
fun WeekdayHeader(
    modifier: Modifier = Modifier
) {
    // 从周日开始：日 一 二 三 四 五 六
    val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEachIndexed { index, weekday ->
            // 周日(index=0)和周六(index=6)为周末
            val isWeekend = index == 0 || index == 6
            Text(
                text = weekday,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = if (isWeekend) WeekendText else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 日程事件实体类
 * 对应数据库表 calendar_events
 */
@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    /** 日程标题 */
    val title: String,
    
    /** 备注描述 (可选) */
    val description: String? = null,
    
    /** 地点 (可选) */
    val location: String? = null,
    
    /** 开始时间戳 (毫秒) */
    val startTime: Long,
    
    /** 结束时间戳 (毫秒) */
    val endTime: Long,
    
    /** 是否全天事件 */
    val isAllDay: Boolean = false,
    
    /** 提醒时间偏移 (分钟), null表示不提醒 */
    val reminder: Int? = null,
    
    /** 分类颜色 (十六进制字符串) */
    val color: String? = null,
    
    /** 是否来自订阅日历 */
    val isFromSubscription: Boolean = false,
    
    /** 订阅来源URL (如果来自订阅) */
    val subscriptionUrl: String? = null
)

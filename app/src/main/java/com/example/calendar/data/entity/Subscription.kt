package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 日历订阅实体类
 * 用于保存 WebCal/ICS 订阅 URL
 */
@Entity(tableName = "calendar_subscriptions")
data class Subscription(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    /** 订阅名称 */
    val name: String,
    
    /** 订阅 URL */
    val url: String,
    
    /** 订阅颜色 */
    val color: String? = null,
    
    /** 是否启用 */
    val isEnabled: Boolean = true,
    
    /** 上次同步时间 */
    val lastSyncTime: Long? = null,
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.calendar

import android.app.Application
import com.example.calendar.notification.NotificationHelper

/**
 * 应用程序类
 * 初始化全局单例和通知渠道
 */
class CalendarApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化通知渠道
        NotificationHelper(this)
    }
}

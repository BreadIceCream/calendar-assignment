package com.example.calendar.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 闹钟广播接收器
 * 接收定时的提醒事件并显示通知
 */
class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(NotificationHelper.EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(NotificationHelper.EXTRA_EVENT_TITLE) ?: "日程提醒"
        val description = intent.getStringExtra(NotificationHelper.EXTRA_EVENT_DESCRIPTION) ?: ""
        
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(eventId, title, description)
    }
}

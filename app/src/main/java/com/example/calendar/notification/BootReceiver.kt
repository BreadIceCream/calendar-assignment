package com.example.calendar.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.calendar.data.database.AppDatabase
import com.example.calendar.data.repository.CalendarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机广播接收器
 * 设备重启后重新调度所有未来的提醒
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        // 在后台协程中重新调度提醒
        CoroutineScope(Dispatchers.IO).launch {
            rescheduleReminders(context)
        }
    }
    
    private suspend fun rescheduleReminders(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val repository = CalendarRepository(database.calendarEventDao())
        val notificationHelper = NotificationHelper(context)
        
        val currentTime = System.currentTimeMillis()
        val eventsWithReminders = repository.getEventsWithReminder(currentTime)
        
        eventsWithReminders.forEach { event ->
            notificationHelper.scheduleReminder(event)
        }
    }
}

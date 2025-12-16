package com.example.calendar.data.repository

import com.example.calendar.data.dao.CalendarEventDao
import com.example.calendar.data.entity.CalendarEvent
import kotlinx.coroutines.flow.Flow

/**
 * 日程数据仓库
 * 封装数据访问逻辑，提供给 ViewModel 使用
 */
class CalendarRepository(private val dao: CalendarEventDao) {
    
    /**
     * 获取所有日程
     */
    val allEvents: Flow<List<CalendarEvent>> = dao.getAllEvents()
    
    /**
     * 获取指定日期的日程
     */
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEvent>> {
        return dao.getEventsForDay(startOfDay, endOfDay)
    }
    
    /**
     * 获取指定时间范围内的日程
     */
    fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<CalendarEvent>> {
        return dao.getEventsInRange(startTime, endTime)
    }
    
    /**
     * 获取指定时间范围内的日程 (同步)
     */
    suspend fun getEventsInRangeSync(startTime: Long, endTime: Long): List<CalendarEvent> {
        return dao.getEventsInRangeSync(startTime, endTime)
    }
    
    /**
     * 根据ID获取日程
     */
    suspend fun getEventById(eventId: String): CalendarEvent? {
        return dao.getEventById(eventId)
    }
    
    /**
     * 插入日程
     */
    suspend fun insert(event: CalendarEvent) {
        dao.insert(event)
    }
    
    /**
     * 批量插入日程
     */
    suspend fun insertAll(events: List<CalendarEvent>) {
        dao.insertAll(events)
    }
    
    /**
     * 更新日程
     */
    suspend fun update(event: CalendarEvent) {
        dao.update(event)
    }
    
    /**
     * 删除日程
     */
    suspend fun delete(event: CalendarEvent) {
        dao.delete(event)
    }
    
    /**
     * 根据ID删除日程
     */
    suspend fun deleteById(eventId: String) {
        dao.deleteById(eventId)
    }
    
    /**
     * 获取有提醒的日程
     */
    suspend fun getEventsWithReminder(currentTime: Long): List<CalendarEvent> {
        return dao.getEventsWithReminder(currentTime)
    }
    
    /**
     * 删除订阅来源的所有日程
     */
    suspend fun deleteBySubscriptionUrl(url: String) {
        dao.deleteBySubscriptionUrl(url)
    }
    
    /**
     * 获取所有本地日程 (用于导出)
     */
    suspend fun getAllLocalEventsSync(): List<CalendarEvent> {
        return dao.getAllLocalEventsSync()
    }
    
    /**
     * 获取所有日程 (用于导出全部)
     */
    suspend fun getAllEventsSync(): List<CalendarEvent> {
        return dao.getAllEventsSync()
    }
    
    /**
     * 删除指定日期范围内的本地日程
     */
    suspend fun deleteEventsInRange(startTime: Long, endTime: Long): Int {
        return dao.deleteEventsInRange(startTime, endTime)
    }
    
    /**
     * 删除所有本地日程
     */
    suspend fun deleteAllLocalEvents(): Int {
        return dao.deleteAllLocalEvents()
    }
}

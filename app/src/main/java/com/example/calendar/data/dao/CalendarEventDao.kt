package com.example.calendar.data.dao

import androidx.room.*
import com.example.calendar.data.entity.CalendarEvent
import kotlinx.coroutines.flow.Flow

/**
 * 日程事件数据访问对象
 */
@Dao
interface CalendarEventDao {
    
    /**
     * 插入单个日程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CalendarEvent)
    
    /**
     * 批量插入日程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalendarEvent>)
    
    /**
     * 更新日程
     */
    @Update
    suspend fun update(event: CalendarEvent)
    
    /**
     * 删除日程
     */
    @Delete
    suspend fun delete(event: CalendarEvent)
    
    /**
     * 根据ID删除日程
     */
    @Query("DELETE FROM calendar_events WHERE id = :eventId")
    suspend fun deleteById(eventId: String)
    
    /**
     * 根据ID获取日程
     */
    @Query("SELECT * FROM calendar_events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): CalendarEvent?
    
    /**
     * 获取所有日程 (Flow)
     */
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>
    
    /**
     * 获取指定日期范围内的日程
     * @param startOfDay 当天开始时间戳
     * @param endOfDay 当天结束时间戳
     */
    @Query("""
        SELECT * FROM calendar_events 
        WHERE startTime <= :endOfDay AND endTime >= :startOfDay
        ORDER BY startTime ASC
    """)
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<CalendarEvent>>
    
    /**
     * 获取指定时间范围内的日程 (月视图/周视图)
     * 使用简化的重叠检测: 事件开始时间 <= 范围结束 AND 事件结束时间 >= 范围开始
     */
    @Query("""
        SELECT * FROM calendar_events 
        WHERE startTime <= :endTime AND endTime >= :startTime
        ORDER BY startTime ASC
    """)
    fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<CalendarEvent>>
    
    /**
     * 获取指定时间范围内的日程 (同步版本)
     */
    @Query("""
        SELECT * FROM calendar_events 
        WHERE startTime <= :endTime AND endTime >= :startTime
        ORDER BY startTime ASC
    """)
    suspend fun getEventsInRangeSync(startTime: Long, endTime: Long): List<CalendarEvent>
    
    /**
     * 获取有提醒的日程
     */
    @Query("SELECT * FROM calendar_events WHERE reminder IS NOT NULL AND startTime > :currentTime ORDER BY startTime ASC")
    suspend fun getEventsWithReminder(currentTime: Long): List<CalendarEvent>
    
    /**
     * 删除订阅来源的所有日程
     */
    @Query("DELETE FROM calendar_events WHERE subscriptionUrl = :url")
    suspend fun deleteBySubscriptionUrl(url: String)
    
    /**
     * 获取所有本地日程 (同步版本，用于导出)
     */
    @Query("SELECT * FROM calendar_events WHERE isFromSubscription = 0 ORDER BY startTime ASC")
    suspend fun getAllLocalEventsSync(): List<CalendarEvent>
    
    /**
     * 获取所有日程 (同步版本，用于导出全部日程)
     */
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    suspend fun getAllEventsSync(): List<CalendarEvent>
    
    /**
     * 删除指定日期范围内的日程（本地日程）
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 删除的日程数量
     */
    @Query("""
        DELETE FROM calendar_events 
        WHERE isFromSubscription = 0 
        AND startTime >= :startTime 
        AND startTime < :endTime
    """)
    suspend fun deleteEventsInRange(startTime: Long, endTime: Long): Int
    
    /**
     * 删除所有本地日程
     * @return 删除的日程数量
     */
    @Query("DELETE FROM calendar_events WHERE isFromSubscription = 0")
    suspend fun deleteAllLocalEvents(): Int
}

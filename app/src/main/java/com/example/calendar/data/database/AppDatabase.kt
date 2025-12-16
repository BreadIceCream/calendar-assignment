package com.example.calendar.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.calendar.data.dao.CalendarEventDao
import com.example.calendar.data.dao.SubscriptionDao
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.data.entity.Subscription

/**
 * Room 数据库类
 */
@Database(
    entities = [CalendarEvent::class, Subscription::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun subscriptionDao(): SubscriptionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendar_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

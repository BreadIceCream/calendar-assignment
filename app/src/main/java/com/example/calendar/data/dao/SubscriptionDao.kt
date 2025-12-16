package com.example.calendar.data.dao

import androidx.room.*
import com.example.calendar.data.entity.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * 订阅数据访问对象
 */
@Dao
interface SubscriptionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription)
    
    @Update
    suspend fun update(subscription: Subscription)
    
    @Delete
    suspend fun delete(subscription: Subscription)
    
    @Query("DELETE FROM calendar_subscriptions WHERE id = :subscriptionId")
    suspend fun deleteById(subscriptionId: String)
    
    @Query("SELECT * FROM calendar_subscriptions WHERE id = :subscriptionId")
    suspend fun getById(subscriptionId: String): Subscription?
    
    @Query("SELECT * FROM calendar_subscriptions ORDER BY createdAt DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM calendar_subscriptions WHERE isEnabled = 1")
    suspend fun getEnabledSubscriptions(): List<Subscription>
    
    @Query("UPDATE calendar_subscriptions SET lastSyncTime = :syncTime WHERE id = :subscriptionId")
    suspend fun updateSyncTime(subscriptionId: String, syncTime: Long)
}

package com.example.calendar.data.repository

import com.example.calendar.data.dao.SubscriptionDao
import com.example.calendar.data.entity.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * 订阅数据仓库
 */
class SubscriptionRepository(private val dao: SubscriptionDao) {
    
    val allSubscriptions: Flow<List<Subscription>> = dao.getAllSubscriptions()
    
    suspend fun insert(subscription: Subscription) {
        dao.insert(subscription)
    }
    
    suspend fun update(subscription: Subscription) {
        dao.update(subscription)
    }
    
    suspend fun delete(subscription: Subscription) {
        dao.delete(subscription)
    }
    
    suspend fun deleteById(subscriptionId: String) {
        dao.deleteById(subscriptionId)
    }
    
    suspend fun getById(subscriptionId: String): Subscription? {
        return dao.getById(subscriptionId)
    }
    
    suspend fun getEnabledSubscriptions(): List<Subscription> {
        return dao.getEnabledSubscriptions()
    }
    
    suspend fun updateSyncTime(subscriptionId: String, syncTime: Long) {
        dao.updateSyncTime(subscriptionId, syncTime)
    }
}

package com.example.calendar.sync

import android.content.Context
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.data.entity.Subscription
import com.example.calendar.data.repository.CalendarRepository
import com.example.calendar.data.repository.SubscriptionRepository
import com.example.calendar.util.IcsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 日历订阅同步服务
 * 负责从网络获取 ICS 数据并同步到本地数据库
 */
class SubscriptionSyncService(
    private val context: Context,
    private val calendarRepository: CalendarRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    /**
     * 同步所有启用的订阅
     * @return 成功同步的订阅数量
     */
    suspend fun syncAllSubscriptions(): SyncResult {
        val subscriptions = subscriptionRepository.getEnabledSubscriptions()
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()
        
        subscriptions.forEach { subscription ->
            val result = syncSubscription(subscription)
            if (result.success) {
                successCount++
            } else {
                failedCount++
                result.error?.let { errors.add("${subscription.name}: $it") }
            }
        }
        
        return SyncResult(
            success = failedCount == 0,
            syncedCount = successCount,
            failedCount = failedCount,
            errors = errors
        )
    }
    
    /**
     * 同步单个订阅
     */
    suspend fun syncSubscription(subscription: Subscription): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                // 转换 webcal:// 为 https://
                val url = subscription.url
                    .replace("webcal://", "https://")
                    .replace("webcals://", "https://")
                
                // 发起网络请求
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SmartCalendar/1.0")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext SyncResult(
                        success = false,
                        error = "HTTP ${response.code}: ${response.message}"
                    )
                }
                
                val icsContent = response.body?.string() ?: ""
                
                if (!IcsUtils.isValidIcs(icsContent)) {
                    return@withContext SyncResult(
                        success = false,
                        error = "无效的 ICS 格式"
                    )
                }
                
                // 解析事件
                val events = IcsUtils.parseFromIcs(icsContent, subscription.url)
                
                // 删除该订阅的旧事件
                calendarRepository.deleteBySubscriptionUrl(subscription.url)
                
                // 插入新事件
                if (events.isNotEmpty()) {
                    calendarRepository.insertAll(events)
                }
                
                // 更新同步时间
                subscriptionRepository.updateSyncTime(subscription.id, System.currentTimeMillis())
                
                SyncResult(
                    success = true,
                    syncedCount = events.size
                )
            } catch (e: Exception) {
                e.printStackTrace()
                SyncResult(
                    success = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }
    
    /**
     * 从URL获取ICS内容并解析
     */
    suspend fun fetchAndParseIcs(url: String): Pair<List<CalendarEvent>?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val actualUrl = url
                    .replace("webcal://", "https://")
                    .replace("webcals://", "https://")
                
                val request = Request.Builder()
                    .url(actualUrl)
                    .header("User-Agent", "SmartCalendar/1.0")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Pair(null, "HTTP ${response.code}: ${response.message}")
                }
                
                val icsContent = response.body?.string() ?: ""
                
                if (!IcsUtils.isValidIcs(icsContent)) {
                    return@withContext Pair(null, "无效的 ICS 格式")
                }
                
                val events = IcsUtils.parseFromIcs(icsContent, url)
                Pair(events, null)
            } catch (e: Exception) {
                Pair(null, e.message ?: "网络错误")
            }
        }
    }
}

/**
 * 同步结果
 */
data class SyncResult(
    val success: Boolean,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val error: String? = null,
    val errors: List<String> = emptyList()
)

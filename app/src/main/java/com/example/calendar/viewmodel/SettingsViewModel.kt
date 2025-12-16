package com.example.calendar.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.database.AppDatabase
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.data.entity.Subscription
import com.example.calendar.data.repository.CalendarRepository
import com.example.calendar.data.repository.SubscriptionRepository
import com.example.calendar.sync.SubscriptionSyncService
import com.example.calendar.sync.SyncResult
import com.example.calendar.util.IcsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 设置页面 ViewModel
 * 负责 ICS 导入导出和订阅管理
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val calendarRepository = CalendarRepository(database.calendarEventDao())
    private val subscriptionRepository = SubscriptionRepository(database.subscriptionDao())
    private val syncService = SubscriptionSyncService(application, calendarRepository, subscriptionRepository)
    
    // 订阅列表
    val subscriptions = subscriptionRepository.allSubscriptions
    
    // 操作状态
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()
    
    // 同步状态
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    // 消息
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    /**
     * 导入 ICS 文件
     */
    fun importIcsFile(uri: Uri) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val contentResolver: ContentResolver = getApplication<Application>().contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw Exception("无法打开文件")
                
                val content = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                inputStream.close()
                
                if (!IcsUtils.isValidIcs(content)) {
                    _operationState.value = OperationState.Error("无效的 ICS 文件格式")
                    return@launch
                }
                
                val events = IcsUtils.parseFromIcs(content)
                
                if (events.isEmpty()) {
                    _operationState.value = OperationState.Error("未找到任何日程")
                    return@launch
                }
                
                calendarRepository.insertAll(events)
                
                _operationState.value = OperationState.Success("成功导入 ${events.size} 个日程")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("导入失败: ${e.message}")
            }
        }
    }
    
    /**
     * 导出所有日程为 ICS (包括本地和订阅)
     * @return ICS 内容字符串
     */
    suspend fun exportAllEventsToIcs(): String? {
        return try {
            val events = calendarRepository.getAllEventsSync()
            if (events.isEmpty()) {
                _message.value = "没有可导出的日程"
                return null
            }
            IcsUtils.exportToIcs(events)
        } catch (e: Exception) {
            _message.value = "导出失败: ${e.message}"
            null
        }
    }
    
    /**
     * 添加新订阅
     */
    fun addSubscription(name: String, url: String, color: String? = null) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val subscription = Subscription(
                    name = name,
                    url = url,
                    color = color
                )
                subscriptionRepository.insert(subscription)
                
                // 立即同步
                val result = syncService.syncSubscription(subscription)
                
                if (result.success) {
                    _operationState.value = OperationState.Success("订阅已添加，同步了 ${result.syncedCount} 个日程")
                } else {
                    _operationState.value = OperationState.Success("订阅已添加，但同步失败: ${result.error}")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("添加订阅失败: ${e.message}")
            }
        }
    }
    
    /**
     * 删除订阅
     */
    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            try {
                // 删除订阅相关的事件
                calendarRepository.deleteBySubscriptionUrl(subscription.url)
                // 删除订阅记录
                subscriptionRepository.delete(subscription)
                _message.value = "已删除订阅: ${subscription.name}"
            } catch (e: Exception) {
                _message.value = "删除失败: ${e.message}"
            }
        }
    }
    
    /**
     * 同步单个订阅
     */
    fun syncSubscription(subscription: Subscription) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncService.syncSubscription(subscription)
                if (result.success) {
                    _message.value = "同步完成，已更新 ${result.syncedCount} 个日程"
                } else {
                    _message.value = "同步失败: ${result.error}"
                }
            } catch (e: Exception) {
                _message.value = "同步失败: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * 同步所有订阅
     */
    fun syncAllSubscriptions() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncService.syncAllSubscriptions()
                if (result.success) {
                    _message.value = "全部同步完成，共更新 ${result.syncedCount} 个订阅"
                } else {
                    _message.value = "同步完成: ${result.syncedCount} 成功, ${result.failedCount} 失败"
                }
            } catch (e: Exception) {
                _message.value = "同步失败: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * 删除指定日期范围内的日程
     */
    fun deleteEventsInRange(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            try {
                val count = calendarRepository.deleteEventsInRange(startTime, endTime)
                _message.value = "已删除 $count 个日程"
            } catch (e: Exception) {
                _message.value = "删除失败: ${e.message}"
            }
        }
    }
    
    /**
     * 删除所有本地日程
     */
    fun deleteAllLocalEvents() {
        viewModelScope.launch {
            try {
                val count = calendarRepository.deleteAllLocalEvents()
                _message.value = "已删除全部 $count 个本地日程"
            } catch (e: Exception) {
                _message.value = "删除失败: ${e.message}"
            }
        }
    }
    
    /**
     * 重置操作状态
     */
    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
    
    /**
     * 操作状态
     */
    sealed class OperationState {
        object Idle : OperationState()
        object Loading : OperationState()
        data class Success(val message: String) : OperationState()
        data class Error(val message: String) : OperationState()
    }
}

package com.example.calendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.database.AppDatabase
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.data.repository.CalendarRepository
import com.example.calendar.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

/**
 * 日程详情/编辑 ViewModel
 */
class EventDetailViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: CalendarRepository
    private val notificationHelper: NotificationHelper
    
    // 当前编辑的日程
    private val _event = MutableStateFlow<CalendarEvent?>(null)
    val event: StateFlow<CalendarEvent?> = _event.asStateFlow()
    
    // 是否是新建模式
    private val _isNewEvent = MutableStateFlow(true)
    val isNewEvent: StateFlow<Boolean> = _isNewEvent.asStateFlow()
    
    // 表单字段
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()
    
    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()
    
    private val _startTime = MutableStateFlow(System.currentTimeMillis())
    val startTime: StateFlow<Long> = _startTime.asStateFlow()
    
    private val _endTime = MutableStateFlow(System.currentTimeMillis() + 3600000) // +1小时
    val endTime: StateFlow<Long> = _endTime.asStateFlow()
    
    private val _isAllDay = MutableStateFlow(false)
    val isAllDay: StateFlow<Boolean> = _isAllDay.asStateFlow()
    
    private val _reminder = MutableStateFlow<Int?>(null)
    val reminder: StateFlow<Int?> = _reminder.asStateFlow()
    
    private val _color = MutableStateFlow<String?>(null)
    val color: StateFlow<String?> = _color.asStateFlow()
    
    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()
    
    // 表单验证错误
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = CalendarRepository(database.calendarEventDao())
        notificationHelper = NotificationHelper(application)
    }
    
    /**
     * 加载日程详情 (编辑模式)
     */
    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val loadedEvent = repository.getEventById(eventId)
            if (loadedEvent != null) {
                _event.value = loadedEvent
                _isNewEvent.value = false
                
                // 填充表单
                _title.value = loadedEvent.title
                _description.value = loadedEvent.description ?: ""
                _location.value = loadedEvent.location ?: ""
                _startTime.value = loadedEvent.startTime
                _endTime.value = loadedEvent.endTime
                _isAllDay.value = loadedEvent.isAllDay
                _reminder.value = loadedEvent.reminder
                _color.value = loadedEvent.color
            }
        }
    }
    
    /**
     * 初始化为新建模式
     */
    fun initNewEvent(initialDate: Triple<Int, Int, Int>? = null) {
        _isNewEvent.value = true
        _event.value = null
        
        // 重置表单
        _title.value = ""
        _description.value = ""
        _location.value = ""
        _isAllDay.value = false
        _reminder.value = 10 // 默认提前10分钟
        _color.value = null
        
        // 设置初始时间
        val calendar = Calendar.getInstance()
        if (initialDate != null) {
            calendar.set(initialDate.first, initialDate.second - 1, initialDate.third)
        }
        // 设置为下一个整点
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        
        _startTime.value = calendar.timeInMillis
        _endTime.value = calendar.timeInMillis + 3600000 // +1小时
        
        _validationError.value = null
        _saveState.value = SaveState.Idle
    }
    
    // 表单更新方法
    fun setTitle(value: String) { _title.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setLocation(value: String) { _location.value = value }
    fun setStartTime(value: Long) { 
        _startTime.value = value 
        // 如果结束时间早于开始时间，自动调整
        if (_endTime.value <= value) {
            _endTime.value = value + 3600000
        }
    }
    fun setEndTime(value: Long) { _endTime.value = value }
    fun setIsAllDay(value: Boolean) { _isAllDay.value = value }
    fun setReminder(value: Int?) { _reminder.value = value }
    fun setColor(value: String?) { _color.value = value }
    
    /**
     * 验证表单
     */
    private fun validate(): Boolean {
        if (_title.value.isBlank()) {
            _validationError.value = "请输入日程标题"
            return false
        }
        if (_endTime.value <= _startTime.value) {
            _validationError.value = "结束时间不能早于开始时间"
            return false
        }
        _validationError.value = null
        return true
    }
    
    /**
     * 保存日程
     */
    fun saveEvent() {
        if (!validate()) return
        
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            
            try {
                val eventToSave = CalendarEvent(
                    id = _event.value?.id ?: UUID.randomUUID().toString(),
                    title = _title.value.trim(),
                    description = _description.value.takeIf { it.isNotBlank() },
                    location = _location.value.takeIf { it.isNotBlank() },
                    startTime = _startTime.value,
                    endTime = _endTime.value,
                    isAllDay = _isAllDay.value,
                    reminder = _reminder.value,
                    color = _color.value
                )
                
                if (_isNewEvent.value) {
                    repository.insert(eventToSave)
                } else {
                    repository.update(eventToSave)
                }
                
                // 设置提醒
                if (eventToSave.reminder != null) {
                    notificationHelper.scheduleReminder(eventToSave)
                } else {
                    notificationHelper.cancelReminder(eventToSave.id)
                }
                
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "保存失败")
            }
        }
    }
    
    /**
     * 删除日程
     */
    fun deleteEvent() {
        val currentEvent = _event.value ?: return
        
        viewModelScope.launch {
            try {
                repository.delete(currentEvent)
                notificationHelper.cancelReminder(currentEvent.id)
                _saveState.value = SaveState.Deleted
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "删除失败")
            }
        }
    }
    
    /**
     * 重置保存状态
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
    
    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        object Deleted : SaveState()
        data class Error(val message: String) : SaveState()
    }
}

package com.example.calendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.database.AppDatabase
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.data.repository.CalendarRepository
import com.example.calendar.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 日历视图 ViewModel
 * 管理日历视图状态、日期选择、日程查询
 */
class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: CalendarRepository
    
    // 当前视图类型
    enum class ViewType { MONTH, WEEK, DAY }
    
    private val _viewType = MutableStateFlow(ViewType.MONTH)
    val viewType: StateFlow<ViewType> = _viewType.asStateFlow()
    
    // 当前显示的年月
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()
    
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()
    
    // 选中的日期
    private val _selectedDate = MutableStateFlow(DateUtils.getToday())
    val selectedDate: StateFlow<Triple<Int, Int, Int>> = _selectedDate.asStateFlow()
    
    // 当前月份的日程 (用于月视图标记)
    private val _monthEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val monthEvents: StateFlow<List<CalendarEvent>> = _monthEvents.asStateFlow()
    
    // 选中日期的日程
    private val _selectedDateEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val selectedDateEvents: StateFlow<List<CalendarEvent>> = _selectedDateEvents.asStateFlow()
    
    // 用于取消之前的加载协程
    private var monthEventsJob: kotlinx.coroutines.Job? = null
    private var selectedDateEventsJob: kotlinx.coroutines.Job? = null
    
    // 所有日程
    val allEvents: Flow<List<CalendarEvent>>
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = CalendarRepository(database.calendarEventDao())
        allEvents = repository.allEvents
        
        // 加载当前月份日程
        loadMonthEvents()
        
        // 加载选中日期日程
        loadSelectedDateEvents()
    }
    
    /**
     * 切换视图类型
     */
    fun setViewType(type: ViewType) {
        _viewType.value = type
    }
    
    /**
     * 选择日期
     */
    fun selectDate(year: Int, month: Int, day: Int) {
        _selectedDate.value = Triple(year, month, day)
        
        // 如果选择了不同月份的日期，切换月份
        if (year != _currentYear.value || month != _currentMonth.value) {
            _currentYear.value = year
            _currentMonth.value = month
            loadMonthEvents()
        }
        
        loadSelectedDateEvents()
    }
    
    /**
     * 切换到上一个月
     */
    fun previousMonth() {
        if (_currentMonth.value == 1) {
            _currentYear.value -= 1
            _currentMonth.value = 12
        } else {
            _currentMonth.value -= 1
        }
        loadMonthEvents()
    }
    
    /**
     * 切换到下一个月
     */
    fun nextMonth() {
        if (_currentMonth.value == 12) {
            _currentYear.value += 1
            _currentMonth.value = 1
        } else {
            _currentMonth.value += 1
        }
        loadMonthEvents()
    }
    
    /**
     * 回到今天
     */
    fun goToToday() {
        val today = DateUtils.getToday()
        _currentYear.value = today.first
        _currentMonth.value = today.second
        _selectedDate.value = today
        loadMonthEvents()
        loadSelectedDateEvents()
    }
    
    /**
     * 切换到上一周
     */
    fun previousWeek() {
        val calendar = Calendar.getInstance().apply {
            val (y, m, d) = _selectedDate.value
            set(y, m - 1, d)
            add(Calendar.DAY_OF_MONTH, -7)
        }
        val newYear = calendar.get(Calendar.YEAR)
        val newMonth = calendar.get(Calendar.MONTH) + 1
        val newDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectDate(newYear, newMonth, newDay)
    }
    
    /**
     * 切换到下一周
     */
    fun nextWeek() {
        val calendar = Calendar.getInstance().apply {
            val (y, m, d) = _selectedDate.value
            set(y, m - 1, d)
            add(Calendar.DAY_OF_MONTH, 7)
        }
        val newYear = calendar.get(Calendar.YEAR)
        val newMonth = calendar.get(Calendar.MONTH) + 1
        val newDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectDate(newYear, newMonth, newDay)
    }
    
    /**
     * 切换到前一天
     */
    fun previousDay() {
        val calendar = Calendar.getInstance().apply {
            val (y, m, d) = _selectedDate.value
            set(y, m - 1, d)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val newYear = calendar.get(Calendar.YEAR)
        val newMonth = calendar.get(Calendar.MONTH) + 1
        val newDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectDate(newYear, newMonth, newDay)
    }
    
    /**
     * 切换到后一天
     */
    fun nextDay() {
        val calendar = Calendar.getInstance().apply {
            val (y, m, d) = _selectedDate.value
            set(y, m - 1, d)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val newYear = calendar.get(Calendar.YEAR)
        val newMonth = calendar.get(Calendar.MONTH) + 1
        val newDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectDate(newYear, newMonth, newDay)
    }
    
    /**
     * 加载当前月份的日程
     */
    private fun loadMonthEvents() {
        // 取消之前的加载任务
        monthEventsJob?.cancel()
        
        monthEventsJob = viewModelScope.launch {
            // 月视图会显示上个月末尾和下个月开头的日期
            // 因此需要加载更大范围的事件数据（前后各扩展一周）
            val year = _currentYear.value
            val month = _currentMonth.value
            
            // 获取当月1号是星期几，计算需要显示的上个月天数
            val firstDayOfWeek = DateUtils.getFirstDayOfWeekInMonthSundayStart(year, month)
            
            // 计算上个月的日期范围
            val prevMonth = if (month == 1) 12 else month - 1
            val prevYear = if (month == 1) year - 1 else year
            val startDay = DateUtils.getDaysInMonth(prevYear, prevMonth) - firstDayOfWeek + 1
            val startTime = if (firstDayOfWeek > 0) {
                DateUtils.getStartOfDay(prevYear, prevMonth, startDay)
            } else {
                DateUtils.getStartOfMonth(year, month)
            }
            
            // 计算下个月的日期范围（最多显示2周）
            val nextMonth = if (month == 12) 1 else month + 1
            val nextYear = if (month == 12) year + 1 else year
            val endTime = DateUtils.getEndOfDay(nextYear, nextMonth, 14) // 最多显示下个月14号
            
            repository.getEventsInRange(startTime, endTime).collect { events ->
                _monthEvents.value = events
            }
        }
    }
    
    /**
     * 加载选中日期的日程
     */
    private fun loadSelectedDateEvents() {
        // 取消之前的加载任务
        selectedDateEventsJob?.cancel()
        
        selectedDateEventsJob = viewModelScope.launch {
            val (year, month, day) = _selectedDate.value
            val startTime = DateUtils.getStartOfDay(year, month, day)
            val endTime = DateUtils.getEndOfDay(year, month, day)
            
            repository.getEventsForDay(startTime, endTime).collect { events ->
                _selectedDateEvents.value = events
            }
        }
    }
    
    /**
     * 添加日程
     */
    fun addEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.insert(event)
        }
    }
    
    /**
     * 删除日程
     */
    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.delete(event)
        }
    }
    
    /**
     * 删除日程 (按ID)
     */
    fun deleteEventById(eventId: String) {
        viewModelScope.launch {
            repository.deleteById(eventId)
        }
    }
    
    /**
     * 更新日程
     */
    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.update(event)
        }
    }
    
    /**
     * 获取日程详情
     */
    suspend fun getEventById(eventId: String): CalendarEvent? {
        return repository.getEventById(eventId)
    }
    
    /**
     * 检查某天是否有日程
     */
    fun hasEventsOnDay(year: Int, month: Int, day: Int): Boolean {
        val startTime = DateUtils.getStartOfDay(year, month, day)
        val endTime = DateUtils.getEndOfDay(year, month, day)
        
        return _monthEvents.value.any { event ->
            // 检查事件是否与当天有交集
            event.startTime <= endTime && event.endTime >= startTime
        }
    }
    
    /**
     * 获取某天的日程数量
     */
    fun getEventCountOnDay(year: Int, month: Int, day: Int): Int {
        val startTime = DateUtils.getStartOfDay(year, month, day)
        val endTime = DateUtils.getEndOfDay(year, month, day)
        
        return _monthEvents.value.count { event ->
            // 检查事件是否与当天有交集
            event.startTime <= endTime && event.endTime >= startTime
        }
    }
    
    /**
     * 获取所有本地日程 (用于导出)
     */
    suspend fun getAllLocalEvents(): List<CalendarEvent> {
        return repository.getAllLocalEventsSync()
    }
    
    /**
     * 批量导入日程
     */
    fun importEvents(events: List<CalendarEvent>) {
        viewModelScope.launch {
            repository.insertAll(events)
        }
    }
    
    /**
     * 删除订阅来源的日程
     */
    fun deleteSubscriptionEvents(url: String) {
        viewModelScope.launch {
            repository.deleteBySubscriptionUrl(url)
        }
    }
}

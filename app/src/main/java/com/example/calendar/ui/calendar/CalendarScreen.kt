package com.example.calendar.ui.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calendar.data.entity.CalendarEvent
import com.example.calendar.ui.theme.hexToColor
import com.example.calendar.util.DateUtils
import com.example.calendar.viewmodel.CalendarViewModel

/**
 * 日历主屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onEventClick: (String) -> Unit,
    onAddEventClick: (Int, Int, Int) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 状态收集
    val viewType by viewModel.viewType.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val monthEvents by viewModel.monthEvents.collectAsState()
    val selectedDateEvents by viewModel.selectedDateEvents.collectAsState()
    
    // 通知权限请求
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }
    
    // 请求通知权限
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CalendarTopBar(
                year = currentYear,
                month = currentMonth,
                selectedDate = selectedDate,
                viewType = viewType,
                onPrevious = {
                    when (viewType) {
                        CalendarViewModel.ViewType.MONTH -> viewModel.previousMonth()
                        CalendarViewModel.ViewType.WEEK -> viewModel.previousWeek()
                        CalendarViewModel.ViewType.DAY -> viewModel.previousDay()
                    }
                },
                onNext = {
                    when (viewType) {
                        CalendarViewModel.ViewType.MONTH -> viewModel.nextMonth()
                        CalendarViewModel.ViewType.WEEK -> viewModel.nextWeek()
                        CalendarViewModel.ViewType.DAY -> viewModel.nextDay()
                    }
                },
                onToday = { viewModel.goToToday() },
                onViewTypeChange = { viewModel.setViewType(it) },
                onSettingsClick = onSettingsClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val (year, month, day) = selectedDate
                    onAddEventClick(year, month, day)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加日程",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 视图内容
            AnimatedContent(
                targetState = viewType,
                label = "viewType"
            ) { type ->
                when (type) {
                    CalendarViewModel.ViewType.MONTH -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 月视图
                            MonthView(
                                year = currentYear,
                                month = currentMonth,
                                selectedDate = selectedDate,
                                events = monthEvents,  // 直接传入事件列表
                                onDateSelected = { y, m, d ->
                                    viewModel.selectDate(y, m, d)
                                },
                                onDateClick = { y, m, d ->
                                    // 点击日期切换到日视图
                                    viewModel.selectDate(y, m, d)
                                    viewModel.setViewType(CalendarViewModel.ViewType.DAY)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 选中日期的日程列表
                            SelectedDateEventList(
                                selectedDate = selectedDate,
                                events = selectedDateEvents,
                                onEventClick = onEventClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                    
                    CalendarViewModel.ViewType.WEEK -> {
                        WeekView(
                            year = selectedDate.first,
                            month = selectedDate.second,
                            day = selectedDate.third,
                            events = monthEvents,
                            onEventClick = onEventClick,
                            onDateClick = { y, m, d ->
                                // 点击日期切换到日视图
                                viewModel.selectDate(y, m, d)
                                viewModel.setViewType(CalendarViewModel.ViewType.DAY)
                            },
                            selectedDate = selectedDate
                        )
                    }
                    
                    CalendarViewModel.ViewType.DAY -> {
                        DayView(
                            year = selectedDate.first,
                            month = selectedDate.second,
                            day = selectedDate.third,
                            events = selectedDateEvents,
                            onEventClick = onEventClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * 日历顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    year: Int,
    month: Int,
    selectedDate: Triple<Int, Int, Int>,
    viewType: CalendarViewModel.ViewType,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onViewTypeChange: (CalendarViewModel.ViewType) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    // 根据视图类型显示不同标题
    val titleText = when (viewType) {
        CalendarViewModel.ViewType.MONTH -> DateUtils.formatMonth(year, month)
        CalendarViewModel.ViewType.WEEK -> "${selectedDate.first}年${selectedDate.second}月第${getWeekOfMonth(selectedDate)}周"
        CalendarViewModel.ViewType.DAY -> "${selectedDate.first}年${selectedDate.second}月${selectedDate.third}日"
    }
    
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上一个
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上一个")
                }
                
                // 标题
                Text(
                    text = titleText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // 下一个
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下一个")
                }
            }
        },
        actions = {
            // 今天按钮
            TextButton(onClick = onToday) {
                Text("今天")
            }
            
            // 视图切换
            IconButton(onClick = {
                val nextType = when (viewType) {
                    CalendarViewModel.ViewType.MONTH -> CalendarViewModel.ViewType.WEEK
                    CalendarViewModel.ViewType.WEEK -> CalendarViewModel.ViewType.DAY
                    CalendarViewModel.ViewType.DAY -> CalendarViewModel.ViewType.MONTH
                }
                onViewTypeChange(nextType)
            }) {
                Icon(
                    imageVector = when (viewType) {
                        CalendarViewModel.ViewType.MONTH -> Icons.Outlined.CalendarMonth
                        CalendarViewModel.ViewType.WEEK -> Icons.Outlined.CalendarViewWeek
                        CalendarViewModel.ViewType.DAY -> Icons.Outlined.CalendarViewDay
                    },
                    contentDescription = "切换视图"
                )
            }
            
            // 设置按钮
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "设置")
            }
        }
    )
}

/**
 * 获取当月的第几周
 */
private fun getWeekOfMonth(date: Triple<Int, Int, Int>): Int {
    val calendar = java.util.Calendar.getInstance().apply {
        set(date.first, date.second - 1, date.third)
        firstDayOfWeek = java.util.Calendar.SUNDAY
    }
    return calendar.get(java.util.Calendar.WEEK_OF_MONTH)
}

/**
 * 选中日期的日程列表
 */
@Composable
private fun SelectedDateEventList(
    selectedDate: Triple<Int, Int, Int>,
    events: List<CalendarEvent>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 标题
        Text(
            text = "${selectedDate.second}月${selectedDate.third}日 日程",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "暂无日程",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn {
                items(events) { event ->
                    EventListItem(
                        event = event,
                        onClick = { onEventClick(event.id) }
                    )
                }
            }
        }
    }
}

/**
 * 日程列表项
 */
@Composable
fun EventListItem(
    event: CalendarEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventColor = hexToColor(event.color)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 颜色条
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(eventColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = if (event.isAllDay) "全天" 
                       else "${DateUtils.formatTime(event.startTime)} - ${DateUtils.formatTime(event.endTime)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            if (!event.location.isNullOrBlank()) {
                Text(
                    text = event.location,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

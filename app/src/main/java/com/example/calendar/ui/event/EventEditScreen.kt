package com.example.calendar.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.ui.theme.EventColorHexList
import com.example.calendar.ui.theme.EventColors
import com.example.calendar.ui.theme.hexToColor
import com.example.calendar.util.DateUtils
import com.example.calendar.viewmodel.EventDetailViewModel
import java.util.Calendar

/**
 * 日程编辑/新建页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    eventId: String?,
    initialDate: Triple<Int, Int, Int>?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EventDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    
    val isNewEvent by viewModel.isNewEvent.collectAsState()
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val location by viewModel.location.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val isAllDay by viewModel.isAllDay.collectAsState()
    val reminder by viewModel.reminder.collectAsState()
    val color by viewModel.color.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    
    // 初始化
    LaunchedEffect(eventId) {
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        } else {
            viewModel.initNewEvent(initialDate)
        }
    }
    
    // 处理保存结果
    LaunchedEffect(saveState) {
        if (saveState is EventDetailViewModel.SaveState.Success) {
            onSaved()
        }
    }
    
    // 显示错误提示
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(validationError) {
        validationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isNewEvent) "新建日程" else "编辑日程") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveEvent() },
                        enabled = saveState !is EventDetailViewModel.SaveState.Saving
                    ) {
                        if (saveState is EventDetailViewModel.SaveState.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 标题
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text("标题 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = validationError?.contains("标题") == true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 全天开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("全天")
                Switch(
                    checked = isAllDay,
                    onCheckedChange = { viewModel.setIsAllDay(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 开始时间
            DateTimeSelector(
                label = "开始时间",
                timestamp = startTime,
                showTime = !isAllDay,
                onDateTimeSelected = { viewModel.setStartTime(it) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 结束时间
            DateTimeSelector(
                label = "结束时间",
                timestamp = endTime,
                showTime = !isAllDay,
                onDateTimeSelected = { viewModel.setEndTime(it) },
                isError = validationError?.contains("结束时间") == true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 地点
            OutlinedTextField(
                value = location,
                onValueChange = { viewModel.setLocation(it) },
                label = { Text("地点") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 提醒设置
            ReminderSelector(
                selectedReminder = reminder,
                onReminderSelected = { viewModel.setReminder(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 颜色选择
            ColorSelector(
                selectedColor = color,
                onColorSelected = { viewModel.setColor(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 备注
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.setDescription(it) },
                label = { Text("备注") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null)
                }
            )
        }
    }
}

/**
 * 日期时间选择器
 */
@Composable
private fun DateTimeSelector(
    label: String,
    timestamp: Long,
    showTime: Boolean,
    onDateTimeSelected: (Long) -> Unit,
    isError: Boolean = false
) {
    val context = LocalContext.current
    val calendar = remember(timestamp) {
        Calendar.getInstance().apply { timeInMillis = timestamp }
    }
    
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isError) MaterialTheme.colorScheme.error 
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 日期选择
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(year, month, day)
                                onDateTimeSelected(calendar.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(DateUtils.formatDate(timestamp))
                }
            }
            
            // 时间选择
            if (showTime) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(0.6f)
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    onDateTimeSelected(calendar.timeInMillis)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(DateUtils.formatTime(timestamp))
                    }
                }
            }
        }
    }
}

/**
 * 提醒选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSelector(
    selectedReminder: Int?,
    onReminderSelected: (Int?) -> Unit
) {
    val options = listOf(
        null to "不提醒",
        0 to "事件发生时",
        10 to "提前10分钟",
        30 to "提前30分钟",
        60 to "提前1小时",
        1440 to "提前1天"
    )
    
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "提醒",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = options.find { it.first == selectedReminder }?.second ?: "不提醒",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                leadingIcon = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                }
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onReminderSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 颜色选择器
 */
@Composable
private fun ColorSelector(
    selectedColor: String?,
    onColorSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "颜色",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EventColorHexList.zip(EventColors)) { (hex, color) ->
                val isSelected = selectedColor == hex
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else Modifier
                        )
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

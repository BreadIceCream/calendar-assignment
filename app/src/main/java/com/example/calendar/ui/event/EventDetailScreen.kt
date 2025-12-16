package com.example.calendar.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.ui.theme.hexToColor
import com.example.calendar.util.DateUtils
import com.example.calendar.viewmodel.EventDetailViewModel

/**
 * 日程详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EventDetailViewModel = viewModel()
) {
    val event by viewModel.event.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // 加载日程
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }
    
    // 处理删除结果
    LaunchedEffect(saveState) {
        if (saveState is EventDetailViewModel.SaveState.Deleted) {
            onDeleted()
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除日程") },
            text = { Text("确定要删除这个日程吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteEvent()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val currentEvent = event!!
            val eventColor = hexToColor(currentEvent.color)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(eventColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentEvent.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider()
                
                // 时间信息
                DetailRow(
                    icon = Icons.Default.Schedule,
                    title = "时间",
                    content = if (currentEvent.isAllDay) {
                        "${DateUtils.formatDate(currentEvent.startTime)} 全天"
                    } else {
                        "${DateUtils.formatDateTime(currentEvent.startTime)} - ${DateUtils.formatDateTime(currentEvent.endTime)}"
                    }
                )
                
                // 地点
                if (!currentEvent.location.isNullOrBlank()) {
                    DetailRow(
                        icon = Icons.Default.LocationOn,
                        title = "地点",
                        content = currentEvent.location
                    )
                }
                
                // 提醒
                DetailRow(
                    icon = Icons.Default.Notifications,
                    title = "提醒",
                    content = when (currentEvent.reminder) {
                        null -> "无提醒"
                        0 -> "事件发生时"
                        10 -> "提前10分钟"
                        30 -> "提前30分钟"
                        60 -> "提前1小时"
                        1440 -> "提前1天"
                        else -> "提前${currentEvent.reminder}分钟"
                    }
                )
                
                // 备注
                if (!currentEvent.description.isNullOrBlank()) {
                    DetailRow(
                        icon = Icons.Default.Notes,
                        title = "备注",
                        content = currentEvent.description
                    )
                }
                
                // 来源标记
                if (currentEvent.isFromSubscription) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "此日程来自订阅日历",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 详情行组件
 */
@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = content,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

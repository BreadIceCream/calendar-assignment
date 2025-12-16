package com.example.calendar.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.data.entity.Subscription
import com.example.calendar.util.DateUtils
import com.example.calendar.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * 设置页面
 * 包含 ICS 导入导出和订阅管理功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val operationState by viewModel.operationState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val message by viewModel.message.collectAsState()
    
    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Subscription?>(null) }
    var showDeleteEventsDialog by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    
    // 文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importIcsFile(it) }
    }
    
    // 显示消息
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }
    
    // 显示操作结果
    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is SettingsViewModel.OperationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetOperationState()
            }
            is SettingsViewModel.OperationState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetOperationState()
            }
            else -> {}
        }
    }
    
    // 添加订阅对话框
    if (showAddSubscriptionDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddSubscriptionDialog = false },
            onConfirm = { name, url ->
                viewModel.addSubscription(name, url)
                showAddSubscriptionDialog = false
            }
        )
    }
    
    // 删除确认对话框
    showDeleteConfirmDialog?.let { subscription ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("删除订阅") },
            text = { Text("确定要删除订阅「${subscription.name}」吗？相关日程也会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubscription(subscription)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 删除所有本地日程确认对话框
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = { Text("删除所有本地日程") },
            text = { Text("确定要删除所有手动创建的日程吗？此操作不可恢复。\n\n注：订阅来源的日程不受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllLocalEvents()
                        showDeleteAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除全部")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 按日期范围删除对话框
    if (showDeleteEventsDialog) {
        DeleteEventsByDateRangeDialog(
            onDismiss = { showDeleteEventsDialog = false },
            onConfirm = { startTime, endTime ->
                viewModel.deleteEventsInRange(startTime, endTime)
                showDeleteEventsDialog = false
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ICS 导入导出
            item {
                Text(
                    text = "导入/导出",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.FileDownload,
                        title = "导入 ICS 文件",
                        subtitle = "从文件导入日程",
                        onClick = {
                            importLauncher.launch(arrayOf("text/calendar", "*/*"))
                        }
                    )
                    
                    HorizontalDivider()
                    
                    SettingsItem(
                        icon = Icons.Default.FileUpload,
                        title = "导出日程",
                        subtitle = "导出为 ICS 文件并分享",
                        onClick = {
                            scope.launch {
                                val icsContent = viewModel.exportAllEventsToIcs()
                                if (icsContent != null) {
                                    try {
                                        // 保存到缓存目录
                                        val file = File(context.cacheDir, "calendar_export.ics")
                                        file.writeText(icsContent)
                                        
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/calendar"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "分享日程"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
            
            // 数据管理
            item {
                Text(
                    text = "数据管理",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.DateRange,
                        title = "按日期范围删除",
                        subtitle = "删除指定日期范围内的日程",
                        onClick = { showDeleteEventsDialog = true }
                    )
                    
                    HorizontalDivider()
                    
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "删除所有本地日程",
                        subtitle = "删除所有手动创建的日程",
                        onClick = { showDeleteAllConfirmDialog = true }
                    )
                }
            }
            
            // 日历订阅
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "日历订阅",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row {
                        if (subscriptions.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.syncAllSubscriptions() },
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = "全部同步")
                                }
                            }
                        }
                        
                        IconButton(onClick = { showAddSubscriptionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "添加订阅")
                        }
                    }
                }
            }
            
            if (subscriptions.isEmpty()) {
                item {
                    SettingsCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "暂无订阅",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "点击 + 添加网络日历订阅",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            } else {
                items(subscriptions) { subscription ->
                    SubscriptionItem(
                        subscription = subscription,
                        onSync = { viewModel.syncSubscription(subscription) },
                        onDelete = { showDeleteConfirmDialog = subscription },
                        isSyncing = isSyncing
                    )
                }
            }
            
            // 提示信息
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "提示：支持 webcal:// 或 https:// 格式的 ICS 订阅链接，如中国节假日日历等。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * 设置卡片容器
 */
@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(content = content)
    }
}

/**
 * 设置项
 */
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp)
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

/**
 * 订阅项
 */
@Composable
private fun SubscriptionItem(
    subscription: Subscription,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    isSyncing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subscription.url,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subscription.lastSyncTime?.let {
                    Text(
                        text = "上次同步: ${DateUtils.formatDateTime(it)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            
            IconButton(onClick = onSync, enabled = !isSyncing) {
                Icon(Icons.Default.Refresh, contentDescription = "同步")
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 添加订阅对话框
 */
@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加日历订阅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("订阅 URL") },
                    placeholder = { Text("webcal:// 或 https://") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), url.trim()) },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 按日期范围删除对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteEventsByDateRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (startTime: Long, endTime: Long) -> Unit
) {
    val calendar = java.util.Calendar.getInstance()
    
    // 默认开始日期为今天
    var startYear by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR)) }
    var startMonth by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH) + 1) }
    var startDay by remember { mutableStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH)) }
    
    // 默认结束日期为一个月后
    calendar.add(java.util.Calendar.MONTH, 1)
    var endYear by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR)) }
    var endMonth by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH) + 1) }
    var endDay by remember { mutableStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("按日期范围删除日程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "将删除指定范围内开始的所有本地日程。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // 开始日期
                Text(text = "开始日期", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startYear.toString(),
                        onValueChange = { startYear = it.toIntOrNull() ?: startYear },
                        label = { Text("年") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startMonth.toString(),
                        onValueChange = { startMonth = (it.toIntOrNull() ?: startMonth).coerceIn(1, 12) },
                        label = { Text("月") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startDay.toString(),
                        onValueChange = { startDay = (it.toIntOrNull() ?: startDay).coerceIn(1, 31) },
                        label = { Text("日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // 结束日期
                Text(text = "结束日期", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = endYear.toString(),
                        onValueChange = { endYear = it.toIntOrNull() ?: endYear },
                        label = { Text("年") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endMonth.toString(),
                        onValueChange = { endMonth = (it.toIntOrNull() ?: endMonth).coerceIn(1, 12) },
                        label = { Text("月") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endDay.toString(),
                        onValueChange = { endDay = (it.toIntOrNull() ?: endDay).coerceIn(1, 31) },
                        label = { Text("日") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Text(
                    text = "⚠️ 此操作不可恢复",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startTime = DateUtils.getStartOfDay(startYear, startMonth, startDay)
                    val endTime = DateUtils.getEndOfDay(endYear, endMonth, endDay)
                    onConfirm(startTime, endTime)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

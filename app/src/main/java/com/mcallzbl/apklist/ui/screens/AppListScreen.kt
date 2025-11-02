package com.mcallzbl.apklist.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.mcallzbl.apklist.model.AppInfo
import com.mcallzbl.apklist.ui.components.AppDetailDialog
import com.mcallzbl.apklist.ui.components.AppListItem
import com.mcallzbl.apklist.utils.RequestPermissions
import com.mcallzbl.apklist.utils.getRequiredPermissions
import com.mcallzbl.apklist.viewModel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    // 请求权限
    RequestPermissions(
        permissions = getRequiredPermissions(),
        onPermissionsResult = { permissions ->
            permissionsGranted = permissions.values.all { it }
            if (permissionsGranted) {
                viewModel.loadApps()
            }
        }
    )

    // 处理返回键
    BackHandler {
        if (showExportMenu) {
            showExportMenu = false
        } else if (selectedApp != null) {
            selectedApp = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "应用列表导出工具",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // 刷新按钮
                    IconButton(
                        onClick = { viewModel.loadApps() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }

                    // 导出按钮
                    IconButton(
                        onClick = { showExportMenu = true },
                        enabled = uiState.filteredApps.isNotEmpty() && !uiState.isExporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "导出"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.filteredApps.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showExportMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "导出列表"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 搜索框和设置
            SearchAndSettingsSection(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::searchApps,
                includeSystemApps = uiState.includeSystemApps,
                onToggleSystemApps = viewModel::toggleSystemApps
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 统计信息
            StatsSection(
                totalCount = uiState.apps.size,
                filteredCount = uiState.filteredApps.size,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 应用列表
            Box(modifier = Modifier.weight(1f)) {
                when {
                    !permissionsGranted -> {
                        PermissionRequiredView()
                    }
                    uiState.isLoading -> {
                        LoadingIndicator()
                    }
                    uiState.error != null -> {
                        ErrorView(
                            error = uiState.error!!,
                            onRetry = viewModel::loadApps
                        )
                    }
                    uiState.filteredApps.isEmpty() -> {
                        EmptyView(
                            searchQuery = uiState.searchQuery
                        )
                    }
                    else -> {
                        AppListContent(
                            apps = uiState.filteredApps,
                            onAppClick = { selectedApp = it }
                        )
                    }
                }
            }
        }
    }

    // 导出菜单
    if (showExportMenu) {
        ExportMenuDialog(
            onDismiss = { showExportMenu = false },
            onExportToJson = {
                viewModel.exportToJson()
                showExportMenu = false
            },
            onExportToCsv = {
                viewModel.exportToCsv()
                showExportMenu = false
            },
            onExportToTxt = {
                viewModel.exportToTxt()
                showExportMenu = false
            },
            onExportAndShareJson = {
                viewModel.exportAndShareJson()
                showExportMenu = false
            },
            onExportAndShareCsv = {
                viewModel.exportAndShareCsv()
                showExportMenu = false
            },
            onExportAndShareTxt = {
                viewModel.exportAndShareTxt()
                showExportMenu = false
            },
            isExporting = uiState.isExporting
        )
    }

    // 应用详情对话框
    selectedApp?.let { app ->
        AppDetailDialog(
            appInfo = app,
            onDismiss = { selectedApp = null }
        )
    }

    // 导出结果消息
    uiState.exportMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearExportMessage()
        }
    }

    // 错误消息
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
}

@Composable
private fun SearchAndSettingsSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    includeSystemApps: Boolean,
    onToggleSystemApps: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text("搜索应用名称或包名...")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // 系统应用开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "显示系统应用",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = includeSystemApps,
                onCheckedChange = { onToggleSystemApps() }
            )
        }
    }
}

@Composable
private fun StatsSection(
    totalCount: Int,
    filteredCount: Int,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = "总应用",
                value = if (isLoading) "..." else totalCount.toString()
            )

            Divider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StatItem(
                label = "显示",
                value = if (isLoading) "..." else filteredCount.toString()
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "正在加载应用列表...",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "错误",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = onRetry
            ) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun PermissionRequiredView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "权限需要",
                modifier = Modifier.size(48.dp),
//                tint = MaterialTheme.colorScheme.warning
            )
            Text(
                text = "需要权限才能继续",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "请授予应用必要的权限以获取已安装的应用列表和导出文件",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyView(
    searchQuery: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "无结果",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (searchQuery.isBlank()) "没有找到应用" else "没有找到匹配的应用",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (searchQuery.isNotBlank()) {
                Text(
                    text = "搜索关键词: $searchQuery",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppListContent(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = apps,
            key = { it.packageName }
        ) { app ->
            AppListItem(
                appInfo = app,
                onClick = onAppClick
            )
        }
    }
}

@Composable
private fun ExportMenuDialog(
    onDismiss: () -> Unit,
    onExportToJson: () -> Unit,
    onExportToCsv: () -> Unit,
    onExportToTxt: () -> Unit,
    onExportAndShareJson: () -> Unit,
    onExportAndShareCsv: () -> Unit,
    onExportAndShareTxt: () -> Unit,
    isExporting: Boolean
) {
    if (isExporting) {
        AlertDialog(
            onDismissRequest = { /* 防止在导出时关闭 */ },
            title = {
                Text(
                    text = "正在导出",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("正在导出应用列表，请稍候...")
                }
            },
            confirmButton = {
                // 导出时不显示确认按钮
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "选择导出格式",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "导出选项",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExportOption(
                        title = "JSON格式",
                        description = "结构化数据，包含所有信息",
                        icon = Icons.Default.Code,
                        onClick = {
                            onExportToJson()
                            onDismiss()
                        }
                    )

                    ExportOption(
                        title = "CSV格式",
                        description = "表格数据，可用Excel打开",
                        icon = Icons.Default.TableChart,
                        onClick = {
                            onExportToCsv()
                            onDismiss()
                        }
                    )

                    ExportOption(
                        title = "TXT格式",
                        description = "纯文本格式，易于阅读",
                        icon = Icons.Default.Description,
                        onClick = {
                            onExportToTxt()
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "导出并分享",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ExportOption(
                        title = "分享JSON",
                        description = "导出JSON格式并直接分享",
                        icon = Icons.Default.Share,
                        onClick = {
                            onExportAndShareJson()
                            onDismiss()
                        }
                    )

                    ExportOption(
                        title = "分享CSV",
                        description = "导出CSV格式并直接分享",
                        icon = Icons.Default.Share,
                        onClick = {
                            onExportAndShareCsv()
                            onDismiss()
                        }
                    )

                    ExportOption(
                        title = "分享TXT",
                        description = "导出TXT格式并直接分享",
                        icon = Icons.Default.Share,
                        onClick = {
                            onExportAndShareTxt()
                            onDismiss()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ExportOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
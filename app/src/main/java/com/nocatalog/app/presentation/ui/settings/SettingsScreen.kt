package com.nocatalog.app.presentation.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.model.HomeViewMode
import com.nocatalog.app.presentation.component.AppTopBar
import com.nocatalog.app.presentation.component.SectionCard
import com.nocatalog.app.presentation.component.ScreenTopContentPadding
import com.nocatalog.app.presentation.component.SettingGroupCard
import com.nocatalog.app.presentation.component.SettingRow

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenImportExport: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPasswordDialog by remember { mutableStateOf(false) }

    androidx.compose.material3.Scaffold(
        topBar = {
            AppTopBar(
                title = "设置",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = ScreenTopContentPadding, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingGroupCard(
                    title = "显示设置",
                    subtitle = "统一首页视图和默认排序，减少重复切换。",
                ) {
                    SettingRow(
                        title = "首页视图",
                        subtitle = "当前为 ${uiState.homeViewMode.displayName()}",
                        icon = Icons.Default.ViewModule,
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = uiState.homeViewMode == HomeViewMode.CARD,
                                    onClick = viewModel::setCardMode,
                                    label = { Text("卡片") },
                                )
                                FilterChip(
                                    selected = uiState.homeViewMode == HomeViewMode.TABLE,
                                    onClick = viewModel::setTableMode,
                                    label = { Text("表格") },
                                )
                            }
                        },
                    )
                    SettingRow(
                        title = "默认排序",
                        subtitle = sortLabel(uiState.defaultSort),
                        icon = Icons.AutoMirrored.Filled.Sort,
                        onClick = viewModel::cycleSort,
                    )
                }
            }
            item {
                SettingGroupCard(
                    title = "安全设置",
                    subtitle = "密码锁继续沿用原有逻辑，只重构交互和展示。",
                ) {
                    SettingRow(
                        title = "密码锁",
                        subtitle = if (uiState.isPasswordSet) "已开启，冷启动和超时后需要解锁" else "未开启，可为本地收藏加一道保护",
                        icon = Icons.Default.Lock,
                        trailing = {
                            Switch(
                                checked = uiState.isPasswordSet,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showPasswordDialog = true
                                    } else {
                                        viewModel.clearPassword()
                                    }
                                },
                            )
                        },
                    )
                    if (uiState.isPasswordSet) {
                        SettingRow(
                            title = "更改密码",
                            subtitle = "保留现有设置方式，重新输入后覆盖旧密码",
                            icon = Icons.Default.Lock,
                            onClick = { showPasswordDialog = true },
                        )
                    }
                }
            }
            item {
                SettingGroupCard(title = "数据与备份") {
                    SettingRow(
                        title = "导入导出",
                        subtitle = "打开 CSV / JSON 导入导出页",
                        icon = Icons.Default.ImportExport,
                        onClick = onOpenImportExport,
                    )
                    SettingRow(
                        title = "WebDAV 备份",
                        subtitle = "保存配置、测试连接、执行备份与恢复",
                        icon = Icons.Default.CloudUpload,
                        onClick = onOpenBackup,
                    )
                }
            }
            item {
                SectionCard(title = "关于", subtitle = "NoCatalog 当前保持 UTF-8 编码和本地优先的收藏管理体验。") {
                    Text(
                        text = "本页仅重构了视觉层级和交互分组，没有改动现有业务流程。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    uiState.message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            confirmButton = {
                FilterChip(
                    selected = true,
                    onClick = {
                        viewModel.savePassword()
                        showPasswordDialog = false
                    },
                    label = { Text("保存") },
                )
            },
            dismissButton = {
                FilterChip(
                    selected = false,
                    onClick = { showPasswordDialog = false },
                    label = { Text("取消") },
                )
            },
            title = { Text(if (uiState.isPasswordSet) "更改密码" else "开启密码锁") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "密码至少 4 位，保存后仍沿用现有解锁流程。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = viewModel::onPasswordInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("密码") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            },
        )
    }
}

private fun HomeViewMode.displayName(): String = when (this) {
    HomeViewMode.CARD -> "卡片"
    HomeViewMode.TABLE -> "表格"
}

private fun sortLabel(sort: EntrySort): String = when (sort) {
    EntrySort.UPDATED_DESC -> "最近更新"
    EntrySort.CREATED_DESC -> "最近新增"
    EntrySort.RATING_DESC -> "评分优先"
    EntrySort.TITLE_ASC -> "标题 A-Z"
    EntrySort.CODE_ASC -> "番号 A-Z"
    EntrySort.RELEASE_DATE_DESC -> "发布时间"
}

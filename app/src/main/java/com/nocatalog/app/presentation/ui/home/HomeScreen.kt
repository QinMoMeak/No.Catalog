package com.nocatalog.app.presentation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.model.HomeViewMode
import com.nocatalog.app.presentation.component.AppTopBar
import com.nocatalog.app.presentation.component.EntryCard
import com.nocatalog.app.presentation.component.EntryTableRow
import com.nocatalog.app.presentation.component.FilterChipGroup
import com.nocatalog.app.presentation.component.FilterChipItem
import com.nocatalog.app.presentation.component.NoCatalogSearchBar
import com.nocatalog.app.presentation.component.SectionCard
import com.nocatalog.app.presentation.component.ScreenTopContentPadding

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdd: () -> Unit,
    onImportPreview: () -> Unit,
    onBackup: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(uiState.query.isNotBlank()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "NoCatalog",
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onImportPreview) {
                        Icon(Icons.Default.ImportExport, contentDescription = "导入导出")
                    }
                    IconButton(onClick = onBackup) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "备份")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                text = { Text("新增") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = ScreenTopContentPadding, end = 16.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showSearch || uiState.query.isNotBlank()) {
                item {
                    NoCatalogSearchBar(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                    )
                }
            }
            item {
                SectionCard(
                    title = "筛选与排序",
                    subtitle = "把列表切换、排序和筛选控制集中到一个区域，避免功能散落。",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.viewMode == HomeViewMode.CARD,
                            onClick = {
                                if (uiState.viewMode != HomeViewMode.CARD) viewModel.onToggleViewMode()
                            },
                            label = { Text("卡片") },
                        )
                        FilterChip(
                            selected = uiState.viewMode == HomeViewMode.TABLE,
                            onClick = {
                                if (uiState.viewMode != HomeViewMode.TABLE) viewModel.onToggleViewMode()
                            },
                            label = { Text("表格") },
                        )
                        FilterChip(
                            selected = false,
                            onClick = viewModel::onCycleSort,
                            label = { Text(sortLabel(uiState.sort)) },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.favoriteOnly,
                            onClick = viewModel::onToggleFavoriteOnly,
                            label = { Text("星标") },
                        )
                        FilterChip(
                            selected = uiState.watchedOnly,
                            onClick = viewModel::onToggleWatchedOnly,
                            label = { Text("已看") },
                        )
                        FilterChip(
                            selected = uiState.statusFilter != null,
                            onClick = viewModel::onCycleStatusFilter,
                            label = { Text(uiState.statusFilter?.displayName() ?: "全部状态") },
                        )
                        FilterChip(
                            selected = uiState.selectedTagIds.isNotEmpty() || uiState.selectedPerformerIds.isNotEmpty(),
                            onClick = { showAdvancedFilters = true },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null)
                            },
                            label = { Text("标签 / 演员") },
                        )
                    }
                }
            }
            if (uiState.entries.isEmpty()) {
                item {
                    SectionCard(
                        title = "还没有收藏记录",
                        subtitle = "先新增一条内容，列表、筛选和统计都会自动更新。",
                    ) {
                        Text(
                            text = "点击右下角按钮开始录入。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(uiState.entries, key = { it.id }) { entry ->
                    if (uiState.viewMode == HomeViewMode.CARD) {
                        EntryCard(entry = entry, onClick = { onOpenDetail(entry.id) })
                    } else {
                        EntryTableRow(entry = entry, onClick = { onOpenDetail(entry.id) })
                    }
                }
            }
        }
    }

    if (showAdvancedFilters) {
        AlertDialog(
            onDismissRequest = { showAdvancedFilters = false },
            confirmButton = {
                FilterChip(
                    selected = true,
                    onClick = { showAdvancedFilters = false },
                    label = { Text("完成") },
                )
            },
            dismissButton = {
                FilterChip(
                    selected = false,
                    onClick = viewModel::onClearAdvancedFilters,
                    label = { Text("清空") },
                )
            },
            title = { Text("高级筛选") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterSection(
                        title = "标签",
                        options = uiState.availableTags.map { FilterChipItem(id = it.id, label = it.name) },
                        selectedIds = uiState.selectedTagIds,
                        onToggle = viewModel::onToggleTag,
                    )
                    FilterSection(
                        title = "演员",
                        options = uiState.availablePerformers.map { FilterChipItem(id = it.id, label = it.name) },
                        selectedIds = uiState.selectedPerformerIds,
                        onToggle = viewModel::onTogglePerformer,
                    )
                }
            },
        )
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<FilterChipItem>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FilterChipGroup(
            options = options,
            selectedIds = selectedIds,
            onToggle = onToggle,
        )
    }
}

private fun sortLabel(sort: EntrySort): String = when (sort) {
    EntrySort.UPDATED_DESC -> "最近更新"
    EntrySort.CREATED_DESC -> "最近新增"
    EntrySort.RATING_DESC -> "评分优先"
    EntrySort.TITLE_ASC -> "标题 A-Z"
    EntrySort.CODE_ASC -> "番号 A-Z"
    EntrySort.RELEASE_DATE_DESC -> "发布时间"
}

private fun com.nocatalog.app.domain.model.EntryStatus.displayName(): String = when (this) {
    com.nocatalog.app.domain.model.EntryStatus.WISH -> "想看"
    com.nocatalog.app.domain.model.EntryStatus.COLLECTED -> "已收录"
    com.nocatalog.app.domain.model.EntryStatus.WATCHED -> "已看"
    com.nocatalog.app.domain.model.EntryStatus.ARCHIVED -> "归档"
}

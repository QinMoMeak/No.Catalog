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
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.domain.model.HomeViewMode
import com.nocatalog.app.presentation.component.EntryCard
import com.nocatalog.app.presentation.component.EntryTableRow
import com.nocatalog.app.presentation.component.NoCatalogSearchBar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdd: () -> Unit,
    onImportPreview: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Text("+")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "NoCatalog", style = MaterialTheme.typography.headlineMedium)
            NoCatalogSearchBar(value = uiState.query, onValueChange = viewModel::onQueryChange)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = viewModel::onToggleViewMode) {
                    Text(if (uiState.viewMode == HomeViewMode.CARD) "表格视图" else "卡片视图")
                }
                Button(onClick = viewModel::onCycleSort) {
                    Text("排序:${uiState.sort.name}")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = viewModel::onCycleStatusFilter) {
                    Text("状态:${uiState.statusFilter?.name ?: "全部"}")
                }
                Button(onClick = viewModel::onToggleFavoriteOnly) {
                    Text(if (uiState.favoriteOnly) "只看星标" else "星标筛选")
                }
                Button(onClick = viewModel::onToggleWatchedOnly) {
                    Text(if (uiState.watchedOnly) "只看已看" else "已看筛选")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onImportPreview) { Text("导入预览") }
                Button(onClick = onBackup) { Text("备份") }
                Button(onClick = onSettings) { Text("设置") }
            }
            if (uiState.entries.isEmpty()) {
                Text("暂无记录，先新增一条收藏。")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
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
    }
}

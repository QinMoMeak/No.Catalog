package com.nocatalog.app.presentation.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EntryDetailScreen(
    viewModel: EntryDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) {
            onBack()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.isLoading) {
                Text("正在加载...", style = MaterialTheme.typography.headlineSmall)
            }
            val entry = uiState.entry
            if (entry != null) {
                Text(entry.title, style = MaterialTheme.typography.headlineSmall)
                Text("番号：${entry.code}")
                Text("状态：${entry.status.name}")
                Text("评分：${entry.rating}")
                Text("演员：${entry.performers.joinToString { it.name }.ifBlank { "未填写" }}")
                Text("标签：${entry.tags.joinToString { it.name }.ifBlank { "未填写" }}")
                Text("收藏时间：${entry.collectedAt}")
                Text("发布日期：${entry.releaseDate.orEmpty().ifBlank { "未填写" }}")
                Text("来源链接：${entry.sourceUrl.orEmpty().ifBlank { "未填写" }}")
                Text("备注：${entry.notes.orEmpty().ifBlank { "未填写" }}")
                Button(onClick = { onEdit(entry.id) }) {
                    Text("编辑")
                }
                Button(onClick = viewModel::toggleFavorite) {
                    Text(if (entry.favorite) "取消星标" else "设为星标")
                }
                Button(onClick = viewModel::toggleWatched) {
                    Text(if (entry.watched) "标记未看" else "标记已看")
                }
                Button(onClick = viewModel::deleteEntry) {
                    Text("删除")
                }
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

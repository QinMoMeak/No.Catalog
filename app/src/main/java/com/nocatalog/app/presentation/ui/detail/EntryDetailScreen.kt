package com.nocatalog.app.presentation.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.presentation.component.AppTopBar
import com.nocatalog.app.presentation.component.EntryCover
import com.nocatalog.app.presentation.component.SectionCard

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

    Scaffold(
        topBar = {
            AppTopBar(
                title = "详情",
                onBack = onBack,
                actions = {
                    val entry = uiState.entry
                    if (entry != null) {
                        IconButton(onClick = { onEdit(entry.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val entry = uiState.entry
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isLoading) {
                Text("正在加载...", style = MaterialTheme.typography.headlineSmall)
            }
            if (entry != null) {
                SectionCard(title = entry.title, subtitle = "${entry.code} · ${entry.status.displayName()}") {
                    EntryCover(entry = entry, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = entry.performers.joinToString(" / ") { it.name }.ifBlank { "演员未填写" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(onClick = viewModel::toggleFavorite) {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                            Text(if (entry.favorite) "已星标" else "设为星标")
                        }
                        FilledTonalButton(onClick = viewModel::toggleWatched) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Text(if (entry.watched) "标记未看" else "标记已看")
                        }
                        OutlinedButton(onClick = viewModel::deleteEntry) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text("删除")
                        }
                    }
                }
                SectionCard(title = "基础信息") {
                    DetailLine("状态", entry.status.displayName())
                    DetailLine("评分", entry.rating.toString())
                    DetailLine("收藏时间", entry.collectedAt)
                    DetailLine("发布日期", entry.releaseDate.orEmpty().ifBlank { "未填写" })
                }
                SectionCard(title = "标签与来源") {
                    DetailLine("标签", entry.tags.joinToString(" / ") { it.name }.ifBlank { "未填写" })
                    DetailLine("来源链接", entry.sourceUrl.orEmpty().ifBlank { "未填写" })
                }
                SectionCard(title = "备注") {
                    Text(
                        text = entry.notes.orEmpty().ifBlank { "暂无备注" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun com.nocatalog.app.domain.model.EntryStatus.displayName(): String = when (this) {
    com.nocatalog.app.domain.model.EntryStatus.WISH -> "想看"
    com.nocatalog.app.domain.model.EntryStatus.COLLECTED -> "已收录"
    com.nocatalog.app.domain.model.EntryStatus.WATCHED -> "已看"
    com.nocatalog.app.domain.model.EntryStatus.ARCHIVED -> "归档"
}

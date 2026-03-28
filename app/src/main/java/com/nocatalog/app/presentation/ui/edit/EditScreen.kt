package com.nocatalog.app.presentation.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.presentation.component.RatingBar

@Composable
fun EditScreen(
    viewModel: EditViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) onBack()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (uiState.isEditing) "编辑记录" else "新增记录", style = MaterialTheme.typography.headlineSmall)
            if (uiState.isLoading) {
                Text("正在加载记录...")
            }
            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("番号") },
            )
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("标题") },
            )
            OutlinedTextField(
                value = uiState.performersText,
                onValueChange = viewModel::onPerformersChange,
                label = { Text("演员（用逗号或 | 分隔）") },
            )
            OutlinedTextField(
                value = uiState.tagsText,
                onValueChange = viewModel::onTagsChange,
                label = { Text("标签（用逗号或 | 分隔）") },
            )
            RatingBar(rating = uiState.rating, onRatingChange = viewModel::onRatingChange)
            Button(onClick = viewModel::onCycleStatus) {
                Text("状态：${uiState.status.name}")
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = uiState.watched, onCheckedChange = { viewModel.onToggleWatched() })
                    Text("已看")
                }
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = uiState.favorite, onCheckedChange = { viewModel.onToggleFavorite() })
                    Text("星标")
                }
            }
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("备注") },
                minLines = 3,
            )
            OutlinedTextField(
                value = uiState.releaseDate,
                onValueChange = viewModel::onReleaseDateChange,
                label = { Text("发布日期") },
            )
            OutlinedTextField(
                value = uiState.sourceUrl,
                onValueChange = viewModel::onSourceUrlChange,
                label = { Text("来源链接") },
            )
            OutlinedTextField(
                value = uiState.coverRemoteUrl,
                onValueChange = viewModel::onCoverRemoteUrlChange,
                label = { Text("封面 URL") },
            )
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = viewModel::save) {
                Text("保存")
            }
        }
    }
}

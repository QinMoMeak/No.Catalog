package com.nocatalog.app.presentation.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.Performer
import com.nocatalog.app.domain.model.Tag
import com.nocatalog.app.presentation.component.AppTopBar
import com.nocatalog.app.presentation.component.EntryCover
import com.nocatalog.app.presentation.component.SectionCard
import com.nocatalog.app.presentation.component.SectionActions
import com.nocatalog.app.presentation.component.RatingBar

@Composable
fun EditScreen(
    viewModel: EditViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imagePicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importLocalCover(uri)
        }
    }

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) onBack()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (uiState.isEditing) "编辑记录" else "新增记录",
                onBack = onBack,
                actions = {
                    FilterChip(
                        selected = true,
                        onClick = viewModel::save,
                        label = { Text("保存") },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isLoading) {
                Text("正在加载记录...", style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = "基础信息") {
                AppTextField(value = uiState.code, onValueChange = viewModel::onCodeChange, label = "番号")
                AppTextField(value = uiState.title, onValueChange = viewModel::onTitleChange, label = "标题")
                AppTextField(
                    value = uiState.releaseDate,
                    onValueChange = viewModel::onReleaseDateChange,
                    label = "发布日期",
                )
            }
            SectionCard(title = "演员与标签") {
                AppTextField(
                    value = uiState.performersText,
                    onValueChange = viewModel::onPerformersChange,
                    label = "演员（用逗号或 | 分隔）",
                )
                AppTextField(
                    value = uiState.tagsText,
                    onValueChange = viewModel::onTagsChange,
                    label = "标签（用逗号或 | 分隔）",
                )
            }
            SectionCard(title = "状态与评分") {
                FilterChip(
                    selected = true,
                    onClick = viewModel::onCycleStatus,
                    label = { Text("当前状态：${uiState.status.displayName()}") },
                )
                RatingBar(rating = uiState.rating, onRatingChange = viewModel::onRatingChange)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SwitchLine("已看", uiState.watched, viewModel::onToggleWatched)
                    SwitchLine("星标", uiState.favorite, viewModel::onToggleFavorite)
                }
            }
            SectionCard(title = "封面与来源") {
                EntryCover(
                    entry = previewEntry(uiState),
                    modifier = Modifier.fillMaxWidth(),
                )
                SectionActions(
                    primaryLabel = "选择本地封面",
                    secondaryLabel = if (uiState.coverLocalPath.isNotBlank()) "重新选择" else null,
                    onPrimaryClick = { imagePicker.launch(arrayOf("image/*")) },
                    onSecondaryClick = { imagePicker.launch(arrayOf("image/*")) },
                )
                if (uiState.coverLocalPath.isNotBlank()) {
                    Text(
                        text = uiState.coverLocalPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppTextField(
                    value = uiState.sourceUrl,
                    onValueChange = viewModel::onSourceUrlChange,
                    label = "来源链接",
                )
                AppTextField(
                    value = uiState.coverRemoteUrl,
                    onValueChange = viewModel::onCoverRemoteUrlChange,
                    label = "封面 URL",
                )
            }
            SectionCard(title = "备注") {
                AppTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = "备注",
                    minLines = 4,
                )
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            FilledTonalButton(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存记录")
            }
        }
    }
}

@Composable
private fun SwitchLine(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = checked, onCheckedChange = { onToggle() })
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        singleLine = minLines == 1,
        shape = MaterialTheme.shapes.large,
    )
}

private fun previewEntry(uiState: EditUiState): Entry {
    return Entry(
        id = uiState.entryId.orEmpty(),
        code = uiState.code,
        title = uiState.title.ifBlank { "封面预览" },
        performers = emptyList<Performer>(),
        tags = emptyList<Tag>(),
        rating = uiState.rating,
        collectedAt = uiState.collectedAt.ifBlank { uiState.createdAt },
        createdAt = uiState.createdAt.ifBlank { uiState.collectedAt },
        updatedAt = uiState.createdAt.ifBlank { uiState.collectedAt },
        coverLocalPath = uiState.coverLocalPath.ifBlank { null },
        coverThumbPath = uiState.coverThumbPath.ifBlank { null },
        coverRemoteUrl = uiState.coverRemoteUrl.ifBlank { null },
    )
}

private fun com.nocatalog.app.domain.model.EntryStatus.displayName(): String = when (this) {
    com.nocatalog.app.domain.model.EntryStatus.WISH -> "想看"
    com.nocatalog.app.domain.model.EntryStatus.COLLECTED -> "已收录"
    com.nocatalog.app.domain.model.EntryStatus.WATCHED -> "已看"
    com.nocatalog.app.domain.model.EntryStatus.ARCHIVED -> "归档"
}

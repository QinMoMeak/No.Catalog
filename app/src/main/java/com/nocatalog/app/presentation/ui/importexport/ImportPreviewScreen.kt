package com.nocatalog.app.presentation.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.domain.model.ImportAction
import com.nocatalog.app.domain.model.ImportPreviewRow

@Composable
fun ImportPreviewScreen(
    viewModel: ImportPreviewViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preview = uiState.preview

    val exportCsvLauncher = rememberLauncherForActivityResult(CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri)
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportJson(uri)
    }
    val importCsvLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) viewModel.loadCsv(uri)
    }
    val importJsonLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) viewModel.loadJson(uri)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("导入导出", style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { exportCsvLauncher.launch("nocatalog-export.csv") }) { Text("导出 CSV") }
                Button(onClick = { exportJsonLauncher.launch("nocatalog-backup.json") }) { Text("导出 JSON") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { importCsvLauncher.launch(arrayOf("text/*")) }) { Text("导入 CSV") }
                Button(onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/json", "*/*")) }) { Text("导入 JSON") }
            }
            if (uiState.isBusy) {
                Text("处理中...")
            }
            uiState.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            if (preview != null) {
                Text("来源：${uiState.fileType.name}")
                Text("总行数：${preview.totalRows}  有效：${preview.validRows}  冲突：${preview.conflictRows}  无效：${preview.invalidRows}")
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(preview.rows, key = { it.index }) { row ->
                        ImportPreviewCard(
                            row = row,
                            action = uiState.rowActions[row.index],
                            onCycleAction = { viewModel.cycleRowAction(row) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::confirmImport) {
                        Text("确认导入")
                    }
                    Button(onClick = viewModel::clearPreview) {
                        Text("清空预览")
                    }
                }
            }

            Button(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

@Composable
private fun ImportPreviewCard(
    row: ImportPreviewRow,
    action: ImportAction?,
    onCycleAction: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("第 ${row.index + 1} 行")
            Text("状态：${row.status.name}")
            Text("番号：${row.raw["code"].orEmpty()}  标题：${row.raw["title"].orEmpty()}")
            row.message?.let { Text(it) }
            if (row.status != com.nocatalog.app.domain.model.ImportRowStatus.INVALID) {
                Button(onClick = onCycleAction) {
                    Text("导入动作：${action?.name ?: "SKIP"}")
                }
            }
        }
    }
}

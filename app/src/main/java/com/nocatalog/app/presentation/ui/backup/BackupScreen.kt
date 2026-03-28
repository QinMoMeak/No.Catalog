package com.nocatalog.app.presentation.ui.backup

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("WebDAV 备份恢复", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = viewModel::onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
            )
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("用户名") },
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码 / 应用专用密码") },
            )
            OutlinedTextField(
                value = uiState.remoteDir,
                onValueChange = viewModel::onRemoteDirChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("远端目录") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::saveConfig) { Text("保存配置") }
                Button(onClick = viewModel::testConnection) { Text("测试连接") }
                Button(onClick = viewModel::backupNow) { Text("立即备份") }
            }
            if (uiState.isBusy) {
                Text("处理中...")
            }
            uiState.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Text("远端备份列表", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(uiState.remoteFiles, key = { it.path }) { file ->
                    Card {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(file.name)
                            Text(file.updatedAt ?: "无时间信息")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.restore(file.path, overwriteAll = false) }) {
                                    Text("仅导入新增")
                                }
                                Button(onClick = { viewModel.restore(file.path, overwriteAll = true) }) {
                                    Text("全量恢复")
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

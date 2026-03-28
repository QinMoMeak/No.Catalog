package com.nocatalog.app.presentation.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
fun SettingsScreen(
    viewModel: SettingsViewModel,
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
            Text("设置", style = MaterialTheme.typography.headlineSmall)
            Text("首页视图：${uiState.homeViewMode.name}")
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::setCardMode) { Text("卡片") }
                Button(onClick = viewModel::setTableMode) { Text("表格") }
            }
            Button(onClick = viewModel::cycleSort) {
                Text("默认排序：${uiState.defaultSort.name}")
            }
            Text(if (uiState.isPasswordSet) "密码锁：已开启" else "密码锁：未开启")
            OutlinedTextField(
                value = uiState.passwordInput,
                onValueChange = viewModel::onPasswordInputChange,
                label = { Text("新密码") },
            )
            Button(onClick = viewModel::savePassword) {
                Text("保存密码")
            }
            if (uiState.isPasswordSet) {
                Button(onClick = viewModel::clearPassword) {
                    Text("关闭密码锁")
                }
            }
            uiState.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

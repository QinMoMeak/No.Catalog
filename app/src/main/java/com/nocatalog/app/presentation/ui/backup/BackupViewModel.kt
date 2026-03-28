package com.nocatalog.app.presentation.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.BuildConfig
import com.nocatalog.app.core.common.Constants
import com.nocatalog.app.core.util.DateTimeUtil
import com.nocatalog.app.domain.model.BackupPayload
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.RemoteBackupFile
import com.nocatalog.app.domain.model.WebDavConfig
import com.nocatalog.app.domain.repository.BackupRepository
import com.nocatalog.app.domain.repository.EntryRepository
import com.nocatalog.app.domain.repository.SettingsRepository
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remoteDir: String = Constants.DEFAULT_REMOTE_DIR,
    val remoteFiles: List<RemoteBackupFile> = emptyList(),
    val isBusy: Boolean = false,
    val message: String? = null,
) : UiState

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val entryRepository: EntryRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.observeSettings().first()
            val config = settings.webDavConfig
            if (config != null) {
                mutableState.update {
                    it.copy(
                        baseUrl = config.baseUrl,
                        username = config.username,
                        password = config.password,
                        remoteDir = config.remoteDir,
                    )
                }
                refreshRemoteBackups()
            }
        }
    }

    fun onBaseUrlChange(value: String) = mutableState.update { it.copy(baseUrl = value) }
    fun onUsernameChange(value: String) = mutableState.update { it.copy(username = value) }
    fun onPasswordChange(value: String) = mutableState.update { it.copy(password = value) }
    fun onRemoteDirChange(value: String) = mutableState.update { it.copy(remoteDir = value) }

    fun saveConfig() {
        viewModelScope.launch {
            val config = currentConfig() ?: run {
                mutableState.update { it.copy(message = "请完整填写 WebDAV 配置") }
                return@launch
            }
            settingsRepository.updateWebDavConfig(config)
            mutableState.update { it.copy(message = "WebDAV 配置已保存") }
        }
    }

    fun testConnection() {
        refreshRemoteBackups(successMessage = "连接测试完成")
    }

    fun backupNow() {
        viewModelScope.launch {
            val config = currentConfig() ?: run {
                mutableState.update { it.copy(message = "请完整填写 WebDAV 配置") }
                return@launch
            }
            settingsRepository.updateWebDavConfig(config)
            mutableState.update { it.copy(isBusy = true, message = null) }
            val payload = BackupPayload(
                schemaVersion = Constants.BACKUP_SCHEMA_VERSION,
                exportedAt = DateTimeUtil.nowUtcIso(),
                appVersion = BuildConfig.VERSION_NAME,
                entries = entryRepository.observeEntries().first(),
            )
            when (val result = backupRepository.backupToWebDav(config, payload)) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    mutableState.update { it.copy(isBusy = false, message = result.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    mutableState.update {
                        it.copy(
                            isBusy = false,
                            message = "备份成功：${result.data.remoteBaseName}（${result.data.recordCount} 条）",
                        )
                    }
                    refreshRemoteBackups()
                }
            }
        }
    }

    fun restore(remoteFileName: String, overwriteAll: Boolean) {
        viewModelScope.launch {
            val config = currentConfig() ?: run {
                mutableState.update { it.copy(message = "请完整填写 WebDAV 配置") }
                return@launch
            }
            mutableState.update { it.copy(isBusy = true, message = null) }
            when (val restored = backupRepository.restoreFromWebDav(config, remoteFileName)) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    mutableState.update { it.copy(isBusy = false, message = restored.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    applyRestore(restored.data.entries, overwriteAll)
                }
            }
        }
    }

    fun refreshRemoteBackups(successMessage: String? = null) {
        viewModelScope.launch {
            val config = currentConfig() ?: return@launch
            mutableState.update { it.copy(isBusy = true, message = null) }
            when (val result = backupRepository.listRemoteBackups(config)) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    mutableState.update { it.copy(isBusy = false, message = result.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    mutableState.update {
                        it.copy(
                            isBusy = false,
                            remoteFiles = result.data.sortedByDescending(RemoteBackupFile::updatedAt),
                            message = successMessage ?: "已刷新远端备份列表",
                        )
                    }
                }
            }
        }
    }

    private suspend fun applyRestore(entries: List<Entry>, overwriteAll: Boolean) {
        val existing = entryRepository.observeEntries().first()
        if (overwriteAll) {
            existing.forEach { entryRepository.deleteEntry(it.id) }
            entries.forEach { entryRepository.addEntry(it) }
            mutableState.update { it.copy(isBusy = false, message = "已全量恢复 ${entries.size} 条记录") }
        } else {
            val candidates = entries.filter { incoming ->
                existing.none { current ->
                    current.id == incoming.id ||
                        current.code.equals(incoming.code, true) ||
                        (
                            current.code.equals(incoming.code, true) &&
                                current.title.equals(incoming.title, true)
                            )
                }
            }
            candidates.forEach { entryRepository.addEntry(it) }
            mutableState.update { it.copy(isBusy = false, message = "已导入新增 ${candidates.size} 条记录") }
        }
        refreshRemoteBackups()
    }

    private fun currentConfig(): WebDavConfig? {
        val state = uiState.value
        if (
            state.baseUrl.isBlank() ||
            state.username.isBlank() ||
            state.password.isBlank() ||
            state.remoteDir.isBlank()
        ) {
            return null
        }
        return WebDavConfig(
            baseUrl = state.baseUrl.trim(),
            username = state.username.trim(),
            password = state.password,
            remoteDir = state.remoteDir.trim(),
        )
    }

    private fun com.nocatalog.app.core.common.AppError.toReadableMessage(): String {
        return when (this) {
            is com.nocatalog.app.core.common.AppError.Network -> message
            is com.nocatalog.app.core.common.AppError.Security -> message
            is com.nocatalog.app.core.common.AppError.Storage -> message
            is com.nocatalog.app.core.common.AppError.Validation -> message
            is com.nocatalog.app.core.common.AppError.Unknown -> message
        }
    }
}


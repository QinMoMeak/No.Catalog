package com.nocatalog.app.presentation.ui.importexport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.ImportAction
import com.nocatalog.app.domain.model.ImportDecision
import com.nocatalog.app.domain.model.ImportPreview
import com.nocatalog.app.domain.model.ImportPreviewRow
import com.nocatalog.app.domain.model.ImportResult
import com.nocatalog.app.domain.model.ImportRowStatus
import com.nocatalog.app.domain.repository.EntryRepository
import com.nocatalog.app.domain.repository.ImportExportRepository
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ImportFileType {
    CSV,
    JSON,
}

data class ImportPreviewUiState(
    val preview: ImportPreview? = null,
    val fileType: ImportFileType = ImportFileType.CSV,
    val rowActions: Map<Int, ImportAction> = emptyMap(),
    val lastImportResult: ImportResult? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
) : UiState

@HiltViewModel
class ImportPreviewViewModel @Inject constructor(
    private val entryRepository: EntryRepository,
    private val importExportRepository: ImportExportRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ImportPreviewUiState())
    val uiState: StateFlow<ImportPreviewUiState> = mutableState.asStateFlow()

    fun exportCsv(uri: Uri) {
        export(uri, ImportFileType.CSV)
    }

    fun exportJson(uri: Uri) {
        export(uri, ImportFileType.JSON)
    }

    fun loadCsv(uri: Uri) {
        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, message = null, fileType = ImportFileType.CSV) }
            val result = importExportRepository.parseCsv(uri)
            when (result) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    mutableState.update { it.copy(isBusy = false, message = result.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    when (val preview = importExportRepository.previewImport(result.data)) {
                        is com.nocatalog.app.core.common.AppResult.Error -> {
                            mutableState.update { it.copy(isBusy = false, message = preview.error.toReadableMessage()) }
                        }
                        is com.nocatalog.app.core.common.AppResult.Success -> {
                            mutableState.update {
                                it.copy(
                                    isBusy = false,
                                    preview = preview.data,
                                    rowActions = buildInitialActions(preview.data.rows),
                                    message = "CSV 预览已生成",
                                    lastImportResult = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadJson(uri: Uri) {
        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, message = null, fileType = ImportFileType.JSON) }
            when (val parsed = importExportRepository.parseJson(uri)) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    mutableState.update { it.copy(isBusy = false, message = parsed.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    when (val preview = importExportRepository.previewImportEntries(parsed.data)) {
                        is com.nocatalog.app.core.common.AppResult.Error -> {
                            mutableState.update { it.copy(isBusy = false, message = preview.error.toReadableMessage()) }
                        }
                        is com.nocatalog.app.core.common.AppResult.Success -> {
                            mutableState.update {
                                it.copy(
                                    isBusy = false,
                                    preview = preview.data,
                                    rowActions = buildInitialActions(preview.data.rows),
                                    message = "JSON 预览已生成",
                                    lastImportResult = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun cycleRowAction(row: ImportPreviewRow) {
        if (row.status == ImportRowStatus.INVALID) return
        mutableState.update { state ->
            val current = state.rowActions[row.index] ?: defaultActionFor(row.status)
            val next = when (row.status) {
                ImportRowStatus.NEW -> if (current == ImportAction.CREATE) ImportAction.SKIP else ImportAction.CREATE
                ImportRowStatus.CONFLICT -> when (current) {
                    ImportAction.SKIP -> ImportAction.OVERWRITE
                    ImportAction.OVERWRITE -> ImportAction.CREATE
                    ImportAction.CREATE -> ImportAction.SKIP
                }
                ImportRowStatus.INVALID -> ImportAction.SKIP
            }
            state.copy(rowActions = state.rowActions + (row.index to next))
        }
    }

    fun confirmImport() {
        val preview = uiState.value.preview ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, message = null) }
            val decision = ImportDecision(
                defaultAction = ImportAction.SKIP,
                actions = uiState.value.rowActions,
            )
            when (val result = importExportRepository.confirmImport(preview, decision)) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    mutableState.update { it.copy(isBusy = false, message = result.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    mutableState.update {
                        it.copy(
                            isBusy = false,
                            lastImportResult = result.data,
                            message = "导入完成：新增 ${result.data.importedCount}，覆盖 ${result.data.overwrittenCount}，跳过 ${result.data.skippedCount}",
                        )
                    }
                }
            }
        }
    }

    fun clearPreview() {
        mutableState.update { ImportPreviewUiState() }
    }

    private fun export(uri: Uri, fileType: ImportFileType) {
        viewModelScope.launch {
            mutableState.update { it.copy(isBusy = true, message = null) }
            val entries = entryRepository.observeEntries().first()
            val result = when (fileType) {
                ImportFileType.CSV -> importExportRepository.exportCsv(entries, uri)
                ImportFileType.JSON -> importExportRepository.exportJson(entries, uri)
            }
            when (result) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    mutableState.update { it.copy(isBusy = false, message = result.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    mutableState.update { it.copy(isBusy = false, message = "${fileType.name} 导出成功") }
                }
            }
        }
    }

    private fun buildInitialActions(rows: List<ImportPreviewRow>): Map<Int, ImportAction> {
        return rows.associate { row ->
            row.index to defaultActionFor(row.status)
        }
    }

    private fun defaultActionFor(status: ImportRowStatus): ImportAction {
        return when (status) {
            ImportRowStatus.NEW -> ImportAction.CREATE
            ImportRowStatus.CONFLICT -> ImportAction.SKIP
            ImportRowStatus.INVALID -> ImportAction.SKIP
        }
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

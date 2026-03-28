package com.nocatalog.app.domain.repository

import android.net.Uri
import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.domain.model.CsvRowRaw
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.ImportDecision
import com.nocatalog.app.domain.model.ImportPreview
import com.nocatalog.app.domain.model.ImportResult

interface ImportExportRepository {
    suspend fun exportCsv(entries: List<Entry>, uri: Uri): AppResult<Unit>
    suspend fun exportJson(entries: List<Entry>, uri: Uri): AppResult<Unit>
    suspend fun parseCsv(uri: Uri): AppResult<List<CsvRowRaw>>
    suspend fun parseJson(uri: Uri): AppResult<List<Entry>>
    suspend fun previewImport(rows: List<CsvRowRaw>): AppResult<ImportPreview>
    suspend fun previewImportEntries(entries: List<Entry>): AppResult<ImportPreview>
    suspend fun confirmImport(preview: ImportPreview, decision: ImportDecision): AppResult<ImportResult>
}

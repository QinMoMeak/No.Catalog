package com.nocatalog.app.domain.usecase.importexport

import android.net.Uri
import com.nocatalog.app.domain.model.CsvRowRaw
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.ImportDecision
import com.nocatalog.app.domain.model.ImportPreview
import com.nocatalog.app.domain.repository.ImportExportRepository
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    private val repository: ImportExportRepository,
) {
    suspend operator fun invoke(entries: List<Entry>, uri: Uri) = repository.exportCsv(entries, uri)
}

class ExportJsonUseCase @Inject constructor(
    private val repository: ImportExportRepository,
) {
    suspend operator fun invoke(entries: List<Entry>, uri: Uri) = repository.exportJson(entries, uri)
}

class ParseCsvUseCase @Inject constructor(
    private val repository: ImportExportRepository,
) {
    suspend operator fun invoke(uri: Uri) = repository.parseCsv(uri)
}

class ParseJsonUseCase @Inject constructor(
    private val repository: ImportExportRepository,
) {
    suspend operator fun invoke(uri: Uri) = repository.parseJson(uri)
}

class PreviewImportUseCase @Inject constructor(
    private val repository: ImportExportRepository,
) {
    suspend operator fun invoke(rows: List<CsvRowRaw>) = repository.previewImport(rows)
}

class PreviewImportEntriesUseCase @Inject constructor(
    private val repository: ImportExportRepository,
) {
    suspend operator fun invoke(entries: List<Entry>) = repository.previewImportEntries(entries)
}

class ConfirmImportUseCase @Inject constructor(
    private val repository: ImportExportRepository,
) {
    suspend operator fun invoke(preview: ImportPreview, decision: ImportDecision) =
        repository.confirmImport(preview, decision)
}

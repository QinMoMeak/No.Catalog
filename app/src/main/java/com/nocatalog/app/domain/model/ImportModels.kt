package com.nocatalog.app.domain.model

import kotlinx.serialization.Serializable

data class CsvRowRaw(
    val index: Int,
    val raw: Map<String, String>,
)

@Serializable
data class ImportPreview(
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val conflictRows: Int,
    val rows: List<ImportPreviewRow>,
)

@Serializable
data class ImportPreviewRow(
    val index: Int,
    val raw: Map<String, String>,
    val parsed: Entry?,
    val status: ImportRowStatus,
    val conflictTargetId: String? = null,
    val message: String? = null,
)

@Serializable
enum class ImportRowStatus {
    NEW,
    CONFLICT,
    INVALID,
}

enum class ImportAction {
    SKIP,
    CREATE,
    OVERWRITE,
}

data class ImportDecision(
    val defaultAction: ImportAction = ImportAction.SKIP,
    val actions: Map<Int, ImportAction> = emptyMap(),
)

data class ImportResult(
    val importedCount: Int,
    val skippedCount: Int,
    val overwrittenCount: Int,
)


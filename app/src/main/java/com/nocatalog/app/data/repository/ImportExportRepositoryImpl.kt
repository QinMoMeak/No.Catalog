package com.nocatalog.app.data.repository

import android.content.Context
import android.net.Uri
import com.nocatalog.app.BuildConfig
import com.nocatalog.app.core.common.AppError
import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.core.common.Constants
import com.nocatalog.app.core.csv.CsvReader
import com.nocatalog.app.core.csv.CsvWriter
import com.nocatalog.app.core.json.JsonBackupSerializer
import com.nocatalog.app.core.util.DateTimeUtil
import com.nocatalog.app.core.util.FileUtil
import com.nocatalog.app.domain.model.BackupPayload
import com.nocatalog.app.domain.model.CsvRowRaw
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryStatus
import com.nocatalog.app.domain.model.ImportAction
import com.nocatalog.app.domain.model.ImportDecision
import com.nocatalog.app.domain.model.ImportPreview
import com.nocatalog.app.domain.model.ImportPreviewRow
import com.nocatalog.app.domain.model.ImportResult
import com.nocatalog.app.domain.model.ImportRowStatus
import com.nocatalog.app.domain.model.Performer
import com.nocatalog.app.domain.model.Tag
import com.nocatalog.app.domain.repository.EntryRepository
import com.nocatalog.app.domain.repository.ImportExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ImportExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entryRepository: EntryRepository,
    private val csvReader: CsvReader,
    private val csvWriter: CsvWriter,
    private val jsonBackupSerializer: JsonBackupSerializer,
) : ImportExportRepository {

    override suspend fun exportCsv(entries: List<Entry>, uri: Uri): AppResult<Unit> {
        return FileUtil.writeBytes(context.contentResolver, uri, csvWriter.write(entries))
    }

    override suspend fun exportJson(entries: List<Entry>, uri: Uri): AppResult<Unit> {
        val payload = BackupPayload(
            schemaVersion = Constants.BACKUP_SCHEMA_VERSION,
            exportedAt = DateTimeUtil.nowUtcIso(),
            appVersion = BuildConfig.VERSION_NAME,
            entries = entries,
        )
        return FileUtil.writeBytes(context.contentResolver, uri, jsonBackupSerializer.encode(payload))
    }

    override suspend fun parseCsv(uri: Uri): AppResult<List<CsvRowRaw>> {
        return when (val read = FileUtil.readBytes(context.contentResolver, uri)) {
            is AppResult.Error -> read
            is AppResult.Success -> AppResult.Success(csvReader.read(read.data))
        }
    }

    override suspend fun parseJson(uri: Uri): AppResult<List<Entry>> {
        return when (val read = FileUtil.readBytes(context.contentResolver, uri)) {
            is AppResult.Error -> read
            is AppResult.Success -> {
                try {
                    AppResult.Success(jsonBackupSerializer.decode(read.data).entries)
                } catch (throwable: Throwable) {
                    AppResult.Error(AppError.Validation(throwable.message ?: "JSON 解析失败"))
                }
            }
        }
    }

    override suspend fun previewImport(rows: List<CsvRowRaw>): AppResult<ImportPreview> {
        val entries = rows.mapIndexed { index, row -> index to parseEntry(row) }
        return buildPreview(
            entries = entries.map { (_, entry) -> entry },
            rawMaps = rows.map { it.raw },
            indexes = rows.map { it.index },
        )
    }

    override suspend fun previewImportEntries(entries: List<Entry>): AppResult<ImportPreview> {
        return buildPreview(
            entries = entries,
            rawMaps = entries.map(::entryToRawMap),
            indexes = entries.indices.toList(),
        )
    }

    override suspend fun confirmImport(
        preview: ImportPreview,
        decision: ImportDecision,
    ): AppResult<ImportResult> {
        var imported = 0
        var skipped = 0
        var overwritten = 0

        preview.rows.forEach { row ->
            val entry = row.parsed
            if (entry == null || row.status == ImportRowStatus.INVALID) {
                skipped++
                return@forEach
            }

            val action = decision.actions[row.index] ?: defaultAction(row.status, decision.defaultAction)
            when (action) {
                ImportAction.SKIP -> skipped++
                ImportAction.CREATE -> {
                    entryRepository.addEntry(entry)
                    imported++
                }

                ImportAction.OVERWRITE -> {
                    val conflictId = row.conflictTargetId
                        ?: return AppResult.Error(AppError.Validation("覆盖导入缺少目标记录"))
                    entryRepository.updateEntry(
                        entry.copy(id = conflictId, updatedAt = DateTimeUtil.nowUtcIso()),
                    )
                    overwritten++
                }
            }
        }

        return AppResult.Success(
            ImportResult(
                importedCount = imported,
                skippedCount = skipped,
                overwrittenCount = overwritten,
            ),
        )
    }

    private fun defaultAction(status: ImportRowStatus, fallback: ImportAction): ImportAction {
        return when (status) {
            ImportRowStatus.NEW -> ImportAction.CREATE
            ImportRowStatus.CONFLICT -> fallback
            ImportRowStatus.INVALID -> ImportAction.SKIP
        }
    }

    private fun parseEntry(row: CsvRowRaw): Entry? {
        val code = row.raw["code"].orEmpty().trim().uppercase()
        val title = row.raw["title"].orEmpty().trim()
        val rating = row.raw["rating"].orEmpty().toFloatOrNull()
        if (code.isBlank() || title.isBlank() || rating == null || rating !in 0f..5f) {
            return null
        }

        val now = DateTimeUtil.nowUtcIso()
        return Entry(
            id = row.raw["id"].orEmpty().ifBlank { UUID.randomUUID().toString() },
            code = code,
            title = title,
            performers = row.raw["performers"].splitMultiValue().map {
                Performer(id = UUID.randomUUID().toString(), name = it)
            },
            tags = row.raw["tags"].splitMultiValue().map {
                Tag(id = UUID.randomUUID().toString(), name = it)
            },
            rating = rating,
            notes = row.raw["notes"]?.takeIf { it.isNotBlank() },
            status = row.raw["status"]?.takeIf { it.isNotBlank() }?.let {
                runCatching { EntryStatus.valueOf(it) }.getOrDefault(EntryStatus.COLLECTED)
            } ?: EntryStatus.COLLECTED,
            favorite = row.raw["favorite"]?.toBooleanStrictOrNull() ?: false,
            watched = row.raw["watched"]?.toBooleanStrictOrNull() ?: false,
            releaseDate = row.raw["release_date"]?.takeIf { it.isNotBlank() },
            collectedAt = row.raw["collected_at"]?.takeIf { it.isNotBlank() } ?: now,
            sourceUrl = row.raw["source_url"]?.takeIf { it.isNotBlank() },
            coverLocalPath = row.raw["cover_local_path"]?.takeIf { it.isNotBlank() },
            coverRemoteUrl = row.raw["cover_remote_url"]?.takeIf { it.isNotBlank() },
            createdAt = row.raw["created_at"]?.takeIf { it.isNotBlank() } ?: now,
            updatedAt = row.raw["updated_at"]?.takeIf { it.isNotBlank() } ?: now,
        )
    }

    private fun String?.splitMultiValue(): List<String> {
        return this.orEmpty()
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private suspend fun buildPreview(
        entries: List<Entry?>,
        rawMaps: List<Map<String, String>>,
        indexes: List<Int>,
    ): AppResult<ImportPreview> {
        val existing = entryRepository.observeEntries().first()
        val previewRows = entries.mapIndexed { listIndex, parsed ->
            val raw = rawMaps[listIndex]
            val rowIndex = indexes[listIndex]
            if (parsed == null) {
                ImportPreviewRow(
                    index = rowIndex,
                    raw = raw,
                    parsed = null,
                    status = ImportRowStatus.INVALID,
                    message = "缺少必填字段或评分非法",
                )
            } else {
                val conflictTarget = existing.firstOrNull { current ->
                    current.id == parsed.id ||
                        current.code.equals(parsed.code, true) ||
                        (
                            current.code.equals(parsed.code, true) &&
                                current.title.equals(parsed.title, true)
                            )
                }

                ImportPreviewRow(
                    index = rowIndex,
                    raw = raw,
                    parsed = parsed,
                    status = if (conflictTarget == null) ImportRowStatus.NEW else ImportRowStatus.CONFLICT,
                    conflictTargetId = conflictTarget?.id,
                    message = if (conflictTarget == null) null else "检测到疑似重复记录",
                )
            }
        }

        return AppResult.Success(
            ImportPreview(
                totalRows = previewRows.size,
                validRows = previewRows.count { it.status != ImportRowStatus.INVALID },
                invalidRows = previewRows.count { it.status == ImportRowStatus.INVALID },
                conflictRows = previewRows.count { it.status == ImportRowStatus.CONFLICT },
                rows = previewRows,
            ),
        )
    }

    private fun entryToRawMap(entry: Entry): Map<String, String> {
        return mapOf(
            "id" to entry.id,
            "code" to entry.code,
            "title" to entry.title,
            "performers" to entry.performers.joinToString("|") { it.name },
            "tags" to entry.tags.joinToString("|") { it.name },
            "rating" to entry.rating.toString(),
            "notes" to entry.notes.orEmpty(),
            "status" to entry.status.name,
            "favorite" to entry.favorite.toString(),
            "watched" to entry.watched.toString(),
            "release_date" to entry.releaseDate.orEmpty(),
            "collected_at" to entry.collectedAt,
            "source_url" to entry.sourceUrl.orEmpty(),
            "cover_local_path" to entry.coverLocalPath.orEmpty(),
            "cover_remote_url" to entry.coverRemoteUrl.orEmpty(),
            "created_at" to entry.createdAt,
            "updated_at" to entry.updatedAt,
        )
    }
}

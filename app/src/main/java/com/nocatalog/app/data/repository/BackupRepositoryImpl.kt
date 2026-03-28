package com.nocatalog.app.data.repository

import com.nocatalog.app.BuildConfig
import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.core.common.Constants
import com.nocatalog.app.core.csv.CsvReader
import com.nocatalog.app.core.csv.CsvWriter
import com.nocatalog.app.core.json.JsonBackupSerializer
import com.nocatalog.app.core.util.DateTimeUtil
import com.nocatalog.app.core.util.HashUtil
import com.nocatalog.app.data.remote.webdav.WebDavRemoteDataSource
import com.nocatalog.app.domain.model.BackupHashes
import com.nocatalog.app.domain.model.BackupManifest
import com.nocatalog.app.domain.model.BackupPayload
import com.nocatalog.app.domain.model.BackupResult
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryStatus
import com.nocatalog.app.domain.model.Performer
import com.nocatalog.app.domain.model.RemoteBackupFile
import com.nocatalog.app.domain.model.Tag
import com.nocatalog.app.domain.model.WebDavConfig
import com.nocatalog.app.domain.repository.BackupRepository
import com.nocatalog.app.domain.repository.EntryRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val entryRepository: EntryRepository,
    private val csvReader: CsvReader,
    private val csvWriter: CsvWriter,
    private val jsonBackupSerializer: JsonBackupSerializer,
    private val remoteDataSource: WebDavRemoteDataSource,
) : BackupRepository {

    private val json = Json { prettyPrint = true }

    override suspend fun backupToWebDav(
        config: WebDavConfig,
        payload: BackupPayload,
    ): AppResult<BackupResult> {
        val entries = payload.entries.ifEmpty { entryRepository.observeEntries().first() }
        val exportedAt = DateTimeUtil.nowUtcIso()
        val backupPayload = payload.copy(
            schemaVersion = Constants.BACKUP_SCHEMA_VERSION,
            exportedAt = exportedAt,
            appVersion = BuildConfig.VERSION_NAME,
            entries = entries,
        )
        val jsonBytes = jsonBackupSerializer.encode(backupPayload)
        val csvBytes = csvWriter.write(entries)
        val timestamp = DateTimeUtil.backupTimestamp()
        val baseDir = normalizeDir(config.remoteDir)
        val backupsDir = "$baseDir/backups"

        when (val ensureBase = remoteDataSource.ensureDir(config, baseDir)) {
            is AppResult.Error -> return ensureBase
            is AppResult.Success -> Unit
        }
        when (val ensureBackups = remoteDataSource.ensureDir(config, backupsDir)) {
            is AppResult.Error -> return ensureBackups
            is AppResult.Success -> Unit
        }

        val manifest = BackupManifest(
            schemaVersion = Constants.BACKUP_SCHEMA_VERSION,
            exportedAt = exportedAt,
            recordCount = entries.size,
            jsonFile = "backup-latest.json",
            csvFile = "backup-latest.csv",
            sha256 = BackupHashes(
                json = HashUtil.sha256(jsonBytes),
                csv = HashUtil.sha256(csvBytes),
            ),
        )
        val manifestBytes = json.encodeToString(manifest).encodeToByteArray()

        val versionedJson = "$backupsDir/backup-$timestamp.json"
        val versionedCsv = "$backupsDir/backup-$timestamp.csv"

        val uploads = listOf(
            Triple(versionedJson, jsonBytes, "application/json"),
            Triple(versionedCsv, csvBytes, "text/csv"),
            Triple("$baseDir/backup-latest.json", jsonBytes, "application/json"),
            Triple("$baseDir/backup-latest.csv", csvBytes, "text/csv"),
            Triple("$baseDir/manifest.json", manifestBytes, "application/json"),
        )
        uploads.forEach { (path, bytes, contentType) ->
            when (val result = remoteDataSource.upload(config, path, bytes, contentType)) {
                is AppResult.Error -> return result
                is AppResult.Success -> Unit
            }
        }

        return AppResult.Success(
            BackupResult(
                exportedAt = exportedAt,
                recordCount = entries.size,
                remoteBaseName = "backup-$timestamp",
            ),
        )
    }

    override suspend fun restoreFromWebDav(
        config: WebDavConfig,
        remoteFileName: String,
    ): AppResult<BackupPayload> {
        val baseDir = normalizeDir(config.remoteDir)
        val path = if (remoteFileName.startsWith("/")) remoteFileName else "$baseDir/$remoteFileName"
        return when (val download = remoteDataSource.download(config, path)) {
            is AppResult.Error -> download
            is AppResult.Success -> {
                if (remoteFileName.endsWith(".csv", ignoreCase = true)) {
                    AppResult.Success(
                        BackupPayload(
                            schemaVersion = Constants.BACKUP_SCHEMA_VERSION,
                            exportedAt = DateTimeUtil.nowUtcIso(),
                            appVersion = BuildConfig.VERSION_NAME,
                            entries = csvToEntries(download.data),
                        ),
                    )
                } else {
                    AppResult.Success(jsonBackupSerializer.decode(download.data))
                }
            }
        }
    }

    override suspend fun listRemoteBackups(config: WebDavConfig): AppResult<List<RemoteBackupFile>> {
        val backupsDir = "${normalizeDir(config.remoteDir)}/backups"
        return when (val list = remoteDataSource.list(config, backupsDir)) {
            is AppResult.Error -> list
            is AppResult.Success -> AppResult.Success(
                list.data
                    .filter { !it.isDirectory && it.name.endsWith(".json", ignoreCase = true) }
                    .map {
                        RemoteBackupFile(
                            name = it.name,
                            path = it.path,
                            updatedAt = it.lastModified,
                        )
                    },
            )
        }
    }

    private fun normalizeDir(remoteDir: String): String {
        return remoteDir.trim().trimEnd('/').ifBlank { Constants.DEFAULT_REMOTE_DIR.trimEnd('/') }
    }

    private fun csvToEntries(bytes: ByteArray): List<Entry> {
        return csvReader.read(bytes).mapNotNull { row ->
            val code = row.raw["code"].orEmpty()
            val title = row.raw["title"].orEmpty()
            val rating = row.raw["rating"]?.toFloatOrNull() ?: return@mapNotNull null
            val now = DateTimeUtil.nowUtcIso()
            Entry(
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
                status = row.raw["status"]?.let { runCatching { EntryStatus.valueOf(it) }.getOrDefault(EntryStatus.COLLECTED) }
                    ?: EntryStatus.COLLECTED,
                favorite = row.raw["favorite"]?.toBooleanStrictOrNull() ?: false,
                watched = row.raw["watched"]?.toBooleanStrictOrNull() ?: false,
                releaseDate = row.raw["release_date"]?.takeIf { it.isNotBlank() },
                collectedAt = row.raw["collected_at"] ?: now,
                sourceUrl = row.raw["source_url"]?.takeIf { it.isNotBlank() },
                coverLocalPath = row.raw["cover_local_path"]?.takeIf { it.isNotBlank() },
                coverThumbPath = row.raw["cover_thumb_path"]?.takeIf { it.isNotBlank() },
                coverRemoteUrl = row.raw["cover_remote_url"]?.takeIf { it.isNotBlank() },
                coverUpdatedAt = row.raw["cover_updated_at"]?.takeIf { it.isNotBlank() },
                createdAt = row.raw["created_at"] ?: now,
                updatedAt = row.raw["updated_at"] ?: now,
            )
        }
    }

    private fun String?.splitMultiValue(): List<String> {
        return this.orEmpty()
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}

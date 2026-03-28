package com.nocatalog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: String,
    val appVersion: String,
    val entries: List<Entry>,
)

@Serializable
data class BackupManifest(
    val schemaVersion: Int,
    val exportedAt: String,
    val recordCount: Int,
    val jsonFile: String,
    val csvFile: String,
    val sha256: BackupHashes,
)

@Serializable
data class BackupHashes(
    val json: String,
    val csv: String,
)

data class BackupResult(
    val exportedAt: String,
    val recordCount: Int,
    val remoteBaseName: String,
)

data class RemoteBackupFile(
    val name: String,
    val path: String,
    val updatedAt: String?,
)

data class WebDavConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
    val remoteDir: String,
)

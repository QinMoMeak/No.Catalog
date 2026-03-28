package com.nocatalog.app.domain.repository

import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.domain.model.BackupPayload
import com.nocatalog.app.domain.model.BackupResult
import com.nocatalog.app.domain.model.RemoteBackupFile
import com.nocatalog.app.domain.model.WebDavConfig

interface BackupRepository {
    suspend fun backupToWebDav(config: WebDavConfig, payload: BackupPayload): AppResult<BackupResult>
    suspend fun restoreFromWebDav(config: WebDavConfig, remoteFileName: String): AppResult<BackupPayload>
    suspend fun listRemoteBackups(config: WebDavConfig): AppResult<List<RemoteBackupFile>>
}


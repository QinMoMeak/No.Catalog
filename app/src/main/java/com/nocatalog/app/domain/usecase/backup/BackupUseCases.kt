package com.nocatalog.app.domain.usecase.backup

import com.nocatalog.app.domain.model.BackupPayload
import com.nocatalog.app.domain.model.WebDavConfig
import com.nocatalog.app.domain.repository.BackupRepository
import javax.inject.Inject

class BackupToWebDavUseCase @Inject constructor(
    private val repository: BackupRepository,
) {
    suspend operator fun invoke(config: WebDavConfig, payload: BackupPayload) =
        repository.backupToWebDav(config, payload)
}

class ListRemoteBackupsUseCase @Inject constructor(
    private val repository: BackupRepository,
) {
    suspend operator fun invoke(config: WebDavConfig) = repository.listRemoteBackups(config)
}

class RestoreFromWebDavUseCase @Inject constructor(
    private val repository: BackupRepository,
) {
    suspend operator fun invoke(config: WebDavConfig, remoteFileName: String) =
        repository.restoreFromWebDav(config, remoteFileName)
}

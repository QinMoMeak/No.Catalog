package com.nocatalog.app.core.webdav

import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.domain.model.WebDavConfig

interface WebDavClient {
    suspend fun ensureDir(config: WebDavConfig, path: String): AppResult<Unit>
    suspend fun list(config: WebDavConfig, path: String): AppResult<List<WebDavFileItem>>
    suspend fun upload(
        config: WebDavConfig,
        remotePath: String,
        bytes: ByteArray,
        contentType: String,
    ): AppResult<Unit>

    suspend fun download(config: WebDavConfig, remotePath: String): AppResult<ByteArray>
}


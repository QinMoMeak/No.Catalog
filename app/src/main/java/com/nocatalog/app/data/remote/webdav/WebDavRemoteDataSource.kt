package com.nocatalog.app.data.remote.webdav

import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.core.webdav.WebDavClient
import com.nocatalog.app.core.webdav.WebDavFileItem
import com.nocatalog.app.domain.model.WebDavConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远端数据源只负责与 WebDAV Client 交互，不承载业务判断。
 */
@Singleton
class WebDavRemoteDataSource @Inject constructor(
    private val client: WebDavClient,
) {
    suspend fun ensureDir(config: WebDavConfig, path: String): AppResult<Unit> =
        client.ensureDir(config, path)

    suspend fun list(config: WebDavConfig, path: String): AppResult<List<WebDavFileItem>> =
        client.list(config, path)

    suspend fun upload(
        config: WebDavConfig,
        remotePath: String,
        bytes: ByteArray,
        contentType: String,
    ): AppResult<Unit> = client.upload(config, remotePath, bytes, contentType)

    suspend fun download(config: WebDavConfig, remotePath: String): AppResult<ByteArray> =
        client.download(config, remotePath)
}

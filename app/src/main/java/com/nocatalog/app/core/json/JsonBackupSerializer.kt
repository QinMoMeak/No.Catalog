package com.nocatalog.app.core.json

import com.nocatalog.app.domain.model.BackupPayload
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * JSON 备份序列化器，保持结构稳定以便后续迁移网站端。
 */
class JsonBackupSerializer @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(payload: BackupPayload): ByteArray {
        return json.encodeToString(BackupPayload.serializer(), payload).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): BackupPayload {
        return json.decodeFromString(BackupPayload.serializer(), bytes.decodeToString())
    }
}

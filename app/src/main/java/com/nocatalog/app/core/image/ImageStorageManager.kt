package com.nocatalog.app.core.image

import android.net.Uri
import com.nocatalog.app.core.common.AppResult

/**
 * 管理封面图片在应用私有目录中的导入、压缩和删除。
 */
interface ImageStorageManager {
    suspend fun importCoverFromUri(sourceUri: Uri, entryId: String): AppResult<CoverImageResult>
    suspend fun deleteCoverFiles(entryId: String): AppResult<Unit>
}

data class CoverImageResult(
    val localPath: String,
    val thumbPath: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)

data class CompressedImageInfo(
    val filePath: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)


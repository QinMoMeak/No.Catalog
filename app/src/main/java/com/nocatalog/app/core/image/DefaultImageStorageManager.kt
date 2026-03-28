package com.nocatalog.app.core.image

import android.content.Context
import android.net.Uri
import com.nocatalog.app.core.common.AppDispatchers
import com.nocatalog.app.core.common.AppError
import com.nocatalog.app.core.common.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * 默认封面存储实现：
 * - 原图压缩版保存在 `files/covers/original`
 * - 列表缩略图保存在 `files/covers/thumbs`
 */
@Singleton
class DefaultImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: DefaultImageCompressor,
    private val dispatchers: AppDispatchers,
) : ImageStorageManager {

    override suspend fun importCoverFromUri(
        sourceUri: Uri,
        entryId: String,
    ): AppResult<CoverImageResult> = withContext(dispatchers.io) {
        try {
            if (!ImagePickerHandler.isImageUri(context, sourceUri)) {
                return@withContext AppResult.Error(AppError.Validation("所选文件不是图片"))
            }

            deleteCoverFiles(entryId)

            val originalFile = coverOriginalFile(entryId)
            val thumbFile = coverThumbFile(entryId)

            val original = when (
                val result = imageCompressor.compress(
                    sourceUri = sourceUri,
                    destinationFile = originalFile,
                    maxLongEdge = 1600,
                    quality = 86,
                )
            ) {
                is AppResult.Error -> return@withContext result
                is AppResult.Success -> result.data
            }

            when (
                val thumbResult = imageCompressor.compress(
                    sourceUri = sourceUri,
                    destinationFile = thumbFile,
                    maxLongEdge = 320,
                    quality = 82,
                )
            ) {
                is AppResult.Error -> return@withContext thumbResult
                is AppResult.Success -> Unit
            }

            AppResult.Success(
                CoverImageResult(
                    localPath = original.filePath,
                    thumbPath = thumbFile.absolutePath,
                    width = original.width,
                    height = original.height,
                    sizeBytes = original.sizeBytes,
                ),
            )
        } catch (throwable: Throwable) {
            AppResult.Error(AppError.Storage(throwable.message ?: "导入封面失败"))
        }
    }

    override suspend fun deleteCoverFiles(entryId: String): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            deleteMatchingFiles(coverOriginalDir(), entryId)
            deleteMatchingFiles(coverThumbDir(), entryId)
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Error(AppError.Storage(throwable.message ?: "删除封面文件失败"))
        }
    }

    private fun deleteMatchingFiles(directory: File, entryId: String) {
        if (!directory.exists()) return
        directory.listFiles()
            ?.filter { it.nameWithoutExtension == entryId }
            ?.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
    }

    private fun coverOriginalFile(entryId: String): File = File(coverOriginalDir(), "$entryId.jpg")

    private fun coverThumbFile(entryId: String): File = File(coverThumbDir(), "$entryId.jpg")

    private fun coverOriginalDir(): File = File(context.filesDir, "covers/original")

    private fun coverThumbDir(): File = File(context.filesDir, "covers/thumbs")
}


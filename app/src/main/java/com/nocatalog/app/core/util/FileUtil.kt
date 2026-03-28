package com.nocatalog.app.core.util

import android.content.ContentResolver
import android.net.Uri
import com.nocatalog.app.core.common.AppError
import com.nocatalog.app.core.common.AppResult

object FileUtil {

    fun writeBytes(
        contentResolver: ContentResolver,
        uri: Uri,
        bytes: ByteArray,
    ): AppResult<Unit> {
        return try {
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return AppResult.Error(AppError.Storage("无法打开输出流"))
            AppResult.Success(Unit)
        } catch (throwable: Throwable) {
            AppResult.Error(AppError.Storage(throwable.message ?: "文件写入失败"))
        }
    }

    fun readBytes(
        contentResolver: ContentResolver,
        uri: Uri,
    ): AppResult<ByteArray> {
        return try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return AppResult.Error(AppError.Storage("无法打开输入流"))
            AppResult.Success(bytes)
        } catch (throwable: Throwable) {
            AppResult.Error(AppError.Storage(throwable.message ?: "文件读取失败"))
        }
    }
}


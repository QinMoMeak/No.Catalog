package com.nocatalog.app.core.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.nocatalog.app.core.common.AppDispatchers
import com.nocatalog.app.core.common.AppError
import com.nocatalog.app.core.common.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * 负责将导入图片压缩为适合存储和列表展示的尺寸。
 */
interface ImageCompressor {
    suspend fun compress(
        sourceUri: Uri,
        destinationFile: File,
        maxLongEdge: Int,
        quality: Int = 86,
    ): AppResult<CompressedImageInfo>
}

@Singleton
class DefaultImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : ImageCompressor {

    override suspend fun compress(
        sourceUri: Uri,
        destinationFile: File,
        maxLongEdge: Int,
        quality: Int,
    ): AppResult<CompressedImageInfo> = withContext(dispatchers.io) {
        try {
            val contentResolver = context.contentResolver
            val bounds = decodeBounds(contentResolver, sourceUri)
                ?: return@withContext AppResult.Error(AppError.Validation("无法读取图片尺寸"))

            val decodedBitmap = decodeBitmap(contentResolver, sourceUri, bounds, maxLongEdge)
                ?: return@withContext AppResult.Error(AppError.Validation("无法解码图片"))

            val scaledBitmap = scaleBitmapIfNeeded(decodedBitmap, maxLongEdge)
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }

            if (scaledBitmap !== decodedBitmap) {
                decodedBitmap.recycle()
            }

            AppResult.Success(
                CompressedImageInfo(
                    filePath = destinationFile.absolutePath,
                    width = scaledBitmap.width,
                    height = scaledBitmap.height,
                    sizeBytes = destinationFile.length(),
                ),
            )
        } catch (throwable: Throwable) {
            AppResult.Error(AppError.Storage(throwable.message ?: "图片压缩失败"))
        }
    }

    private fun decodeBounds(contentResolver: ContentResolver, sourceUri: Uri): BitmapFactory.Options? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        return if (options.outWidth > 0 && options.outHeight > 0) options else null
    }

    private fun decodeBitmap(
        contentResolver: ContentResolver,
        sourceUri: Uri,
        bounds: BitmapFactory.Options,
        maxLongEdge: Int,
    ): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, sourceUri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val size = info.size
                val sampleSize = calculateSampleSize(size.width, size.height, maxLongEdge)
                decoder.setTargetSampleSize(sampleSize)
                decoder.isMutableRequired = false
            }
        } else {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdge)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
        val longEdge = maxOf(width, height)
        if (longEdge <= maxLongEdge) return 1
        var sampleSize = 1
        while (longEdge / sampleSize > maxLongEdge * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxLongEdge: Int): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= maxLongEdge) return bitmap

        val scale = maxLongEdge.toFloat() / longEdge.toFloat()
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}


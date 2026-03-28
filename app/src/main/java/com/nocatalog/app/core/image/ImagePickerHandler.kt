package com.nocatalog.app.core.image

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

/**
 * 图片选择辅助工具，统一判断 Uri 是否为图片资源。
 */
object ImagePickerHandler {

    fun isImageUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
            ?: guessMimeType(uri)
        return mimeType?.startsWith("image/") == true
    }

    private fun guessMimeType(uri: Uri): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return if (extension.isNullOrBlank()) {
            null
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        }
    }
}

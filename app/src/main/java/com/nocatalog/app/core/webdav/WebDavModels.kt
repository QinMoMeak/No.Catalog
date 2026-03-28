package com.nocatalog.app.core.webdav

data class WebDavFileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val contentLength: Long? = null,
    val lastModified: String? = null,
)


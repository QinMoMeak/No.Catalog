package com.nocatalog.app.core.util

import java.security.MessageDigest

object HashUtil {
    fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}


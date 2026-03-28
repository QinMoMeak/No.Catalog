package com.nocatalog.app.core.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateTimeUtil {
    private val formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    fun nowUtcIso(): String = formatter.format(Instant.now())

    fun backupTimestamp(): String {
        return nowUtcIso()
            .replace(":", "-")
            .replace(".", "-")
    }
}


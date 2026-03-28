package com.nocatalog.app.core.csv

import com.nocatalog.app.core.common.escapeCsvCell
import com.nocatalog.app.domain.model.Entry
import javax.inject.Inject

/**
 * CSV 导出遵循 UTF-8 with BOM，优先保证 Excel 兼容性。
 */
class CsvWriter @Inject constructor() {

    fun write(entries: List<Entry>): ByteArray {
        val builder = StringBuilder()
        builder.append(CsvSchema.headers.joinToString(",")).appendLine()
        entries.forEach { entry ->
            builder.append(
                listOf(
                    entry.id,
                    entry.code,
                    entry.title,
                    entry.performers.joinToString("|") { it.name },
                    entry.tags.joinToString("|") { it.name },
                    entry.rating.toString(),
                    entry.notes.orEmpty(),
                    entry.status.name,
                    entry.favorite.toString(),
                    entry.watched.toString(),
                    entry.releaseDate.orEmpty(),
                    entry.collectedAt,
                    entry.sourceUrl.orEmpty(),
                    entry.coverLocalPath.orEmpty(),
                    entry.coverThumbPath.orEmpty(),
                    entry.coverRemoteUrl.orEmpty(),
                    entry.coverUpdatedAt.orEmpty(),
                    entry.createdAt,
                    entry.updatedAt,
                ).joinToString(",") { it.escapeCsvCell() },
            ).appendLine()
        }
        return byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            builder.toString().encodeToByteArray()
    }
}

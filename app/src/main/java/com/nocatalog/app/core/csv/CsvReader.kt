package com.nocatalog.app.core.csv

import com.nocatalog.app.domain.model.CsvRowRaw
import javax.inject.Inject

/**
 * 解析标准 CSV 头，并兼容少量别名列。
 */
class CsvReader @Inject constructor() {

    fun read(bytes: ByteArray): List<CsvRowRaw> {
        val content = bytes.decodeToString()
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headers = parseLine(lines.first())
            .map { header -> CsvSchema.aliases[header] ?: header }

        return lines.drop(1).mapIndexed { index, line ->
            val cells = parseLine(line)
            val raw = headers.mapIndexed { cellIndex, header ->
                header to cells.getOrElse(cellIndex) { "" }
            }.toMap()
            CsvRowRaw(index = index, raw = raw)
        }
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (val char = line[i]) {
                '"' -> {
                    val next = line.getOrNull(i + 1)
                    if (inQuotes && next == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result += current.toString()
                        current.clear()
                    }
                }

                else -> current.append(char)
            }
            i++
        }
        result += current.toString()
        return result
    }
}


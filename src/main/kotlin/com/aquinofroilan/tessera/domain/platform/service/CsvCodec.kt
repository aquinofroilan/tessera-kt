package com.aquinofroilan.tessera.domain.platform.service

import com.aquinofroilan.tessera.exception.BusinessRuleException

/**
 * Minimal RFC 4180-ish CSV encoder/decoder. Fields containing comma, quote,
 * or newline are double-quoted; embedded quotes are doubled. Suitable for
 * Excel and Google Sheets round-trips for plain catalog data. For exotic
 * cases (multi-line headers, locale-dependent number formats, etc.) the
 * caller should pre-stringify values.
 */
object CsvCodec {
    fun encode(
        headers: List<String>,
        rows: List<Map<String, Any?>>,
    ): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { escape(it) })
        sb.append("\r\n")
        for (row in rows) {
            sb.append(headers.joinToString(",") { escape(row[it]?.toString() ?: "") })
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun decode(csv: String): List<Map<String, String>> {
        val all = parseRows(csv)
        if (all.isEmpty()) return emptyList()
        val headers = all.first().map { it.trim() }
        val seen = mutableSetOf<String>()
        headers.forEach {
            if (!seen.add(it)) throw BusinessRuleException("Duplicate header in CSV: '$it'")
        }
        return all.drop(1).filter { it.any { cell -> cell.isNotEmpty() } }.map { row ->
            headers.mapIndexed { idx, h -> h to (row.getOrNull(idx) ?: "") }.toMap()
        }
    }

    private fun escape(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun parseRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val current = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        while (i < csv.length) {
            val c = csv[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        current.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = false
                    i++
                    continue
                }
                current.append(c)
                i++
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                        i++
                    }
                    ',' -> {
                        row.add(current.toString())
                        current.setLength(0)
                        i++
                    }
                    '\r' -> {
                        if (i + 1 < csv.length && csv[i + 1] == '\n') i++
                        row.add(current.toString())
                        current.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                        i++
                    }
                    '\n' -> {
                        row.add(current.toString())
                        current.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                        i++
                    }
                    else -> {
                        current.append(c)
                        i++
                    }
                }
            }
        }
        if (current.isNotEmpty() || row.isNotEmpty()) {
            row.add(current.toString())
            rows.add(row)
        }
        return rows
    }
}

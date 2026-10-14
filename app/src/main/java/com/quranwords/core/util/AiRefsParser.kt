package com.quranwords.core.util

/**
 * Parses `root_ai_summary` reference columns.
 * Stored as JSON-array strings, e.g. `["مقاييس اللغة...", "لسان العرب..."]`.
 * Pure Kotlin, no Android deps — unit-testable.
 */
object AiRefsParser {
    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) {
            return trimmed.split("\n")
                .map { it.trim().trim('،', ',', ';', '؛', '.', ' ') }
                .filter { it.isNotBlank() }
        }
        return try {
            parseJsonArray(trimmed)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseJsonArray(raw: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        // Skip opening bracket.
        while (i < raw.length && raw[i] != '[') i++
        i++
        val current = StringBuilder()
        var inString = false
        var escaped = false
        while (i < raw.length) {
            val c = raw[i]
            if (inString) {
                if (escaped) {
                    when (c) {
                        '"', '\\', '/' -> current.append(c)
                        'n' -> current.append('\n')
                        't' -> current.append('\t')
                        'u' -> {
                            val hex = raw.substring((i + 1).coerceAtMost(raw.length), (i + 5).coerceAtMost(raw.length))
                            current.append(hex.toIntOrNull(16)?.toChar() ?: c)
                            i += 4
                        }
                        else -> current.append(c)
                    }
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                    val value = current.toString().trim()
                    if (value.isNotBlank()) out.add(value)
                    current.clear()
                } else {
                    current.append(c)
                }
            } else {
                if (c == '"') {
                    inString = true
                } else if (c == ']') {
                    break
                }
            }
            i++
        }
        return out
    }
}

package com.quranwords.core.util

enum class SizeUnit { KB, MB }

data class FileSize(val value: Long, val unit: SizeUnit)

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = BYTES_PER_KB * 1024

/** Pure byte-to-unit conversion; UI code maps the result to localized strings. */
fun formatFileSize(bytes: Long): FileSize? = when {
    bytes <= 0 -> null
    bytes >= BYTES_PER_MB -> FileSize(bytes / BYTES_PER_MB, SizeUnit.MB)
    else -> FileSize(bytes / BYTES_PER_KB, SizeUnit.KB)
}

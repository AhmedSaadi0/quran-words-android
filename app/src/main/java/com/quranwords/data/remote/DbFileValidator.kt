package com.quranwords.data.remote

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object DbFileValidator {
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private const val SQLITE_MAGIC = "SQLite format 3\u0000"

    fun isZip(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                if (input.read(header) != 4) return false
                header.contentEquals(ZIP_MAGIC)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun hasSqliteHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(16)
                if (input.read(header) != 16) return false
                String(header, Charsets.UTF_8) == SQLITE_MAGIC
            }
        } catch (_: Exception) {
            false
        }
    }

    fun sha256Hex(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    fun isValidDbSize(length: Long, expectedUncompressed: Long): Boolean {
        if (length <= 0) return false
        if (expectedUncompressed > 0) {
            return length in (expectedUncompressed * 0.9).toLong()..
                (expectedUncompressed * 1.1).toLong()
        }
        return length > 10_000_000L
    }
}

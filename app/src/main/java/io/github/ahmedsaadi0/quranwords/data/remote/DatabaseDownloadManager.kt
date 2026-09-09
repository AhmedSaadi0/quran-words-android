package io.github.ahmedsaadi0.quranwords.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.ahmedsaadi0.quranwords.core.util.DatabaseConstants
import io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

enum class DownloadPhase { DOWNLOAD, EXTRACT }

/**
 * Localizable download failure reason. The UI maps each code to a string
 * resource; [DownloadState.Error.detail] carries the raw technical message
 * for logs only and is never displayed.
 */
enum class DownloadError {
    NETWORK,
    NOT_ZIP,
    CHECKSUM_MISMATCH,
    EXTRACT_FAILED,
    INVALID_DB,
    INCOMPLETE_FILE,
    INSTALL_FAILED,
    BAD_PICK,
    IMPORT_FAILED,
    MANIFEST_FAILED,
    UNKNOWN
}

sealed interface DownloadState {
    object Idle : DownloadState
    data class Progress(
        val percentage: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedKbps: Long = 0,
        val phase: DownloadPhase = DownloadPhase.DOWNLOAD
    ) : DownloadState
    data class Extracting(val percentage: Int) : DownloadState
    data class Completed(val versionCode: Int = 0, val versionName: String = "") : DownloadState
    data class Error(val error: DownloadError, val detail: String? = null) : DownloadState
}

class DatabaseDownloadManager(
    private val context: Context,
    private val client: OkHttpClient
) {
    private val dbName = DatabaseConstants.DB_NAME

    fun isDatabaseReady(): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        return dbFile.exists() && dbFile.length() > DatabaseConstants.DB_MIN_READY_SIZE
    }

    fun getDatabaseFile(): File = context.getDatabasePath(dbName)

    fun downloadRelease(info: DbReleaseInfo): Flow<DownloadState> = flow {
        val targetFile = context.getDatabasePath(dbName)
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        val zipTmp = File(parent, "$dbName.zip.tmp")
        val dbTmp = File(parent, "$dbName.tmp")
        zipTmp.takeIf { it.exists() }?.delete()
        dbTmp.takeIf { it.exists() }?.delete()

        val totalEstimate = info.compressedSize.takeIf { it > 0 }
            ?: info.uncompressedSize.takeIf { it > 0 }
            ?: 30_000_000L
        emit(DownloadState.Progress(0, 0L, totalEstimate, 0))

        try {
            downloadUrlToFile(info.downloadUrl, zipTmp, totalEstimate) { percent, done, total, speed ->
                // Reserve 0..80 for network, 80..100 for unzip.
                val mapped = (percent * 0.8).toInt().coerceIn(0, 80)
                emit(DownloadState.Progress(mapped, done, total, speed, DownloadPhase.DOWNLOAD))
            }
        } catch (e: Exception) {
            zipTmp.takeIf { it.exists() }?.delete()
            emit(DownloadState.Error(DownloadError.NETWORK, e.message))
            return@flow
        }

        if (!DbFileValidator.isZip(zipTmp)) {
            zipTmp.takeIf { it.exists() }?.delete()
            emit(DownloadState.Error(DownloadError.NOT_ZIP))
            return@flow
        }

        if (!info.sha256.isNullOrBlank()) {
            val actual = DbFileValidator.sha256Hex(zipTmp)
            if (actual != null && !actual.equals(info.sha256, ignoreCase = true)) {
                zipTmp.takeIf { it.exists() }?.delete()
                emit(DownloadState.Error(DownloadError.CHECKSUM_MISMATCH))
                return@flow
            }
        }

        try {
            unzipSingleDb(zipTmp, dbTmp) { percent ->
                emit(DownloadState.Extracting(percent))
                val mapped = 80 + (percent * 0.2).toInt().coerceIn(0, 20)
                emit(
                    DownloadState.Progress(
                        mapped,
                        dbTmp.length(),
                        info.uncompressedSize.takeIf { it > 0 } ?: dbTmp.length().coerceAtLeast(1),
                        0,
                        DownloadPhase.EXTRACT
                    )
                )
            }
        } catch (e: Exception) {
            zipTmp.takeIf { it.exists() }?.delete()
            dbTmp.takeIf { it.exists() }?.delete()
            emit(DownloadState.Error(DownloadError.EXTRACT_FAILED, e.message))
            return@flow
        } finally {
            zipTmp.takeIf { it.exists() }?.delete()
        }

        if (!DbFileValidator.hasSqliteHeader(dbTmp)) {
            dbTmp.takeIf { it.exists() }?.delete()
            emit(DownloadState.Error(DownloadError.INVALID_DB))
            return@flow
        }
        if (!DbFileValidator.isValidDbSize(dbTmp.length(), info.uncompressedSize)) {
            dbTmp.takeIf { it.exists() }?.delete()
            emit(DownloadState.Error(DownloadError.INCOMPLETE_FILE))
            return@flow
        }

        if (installDbFile(dbTmp, targetFile)) {
            emit(DownloadState.Completed(info.versionCode, info.versionName))
        } else {
            emit(DownloadState.Error(DownloadError.INSTALL_FAILED))
        }
    }.flowOn(Dispatchers.IO)

    fun importDatabase(sourceUri: Uri): Flow<DownloadState> = flow {
        val targetFile = context.getDatabasePath(dbName)
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        val stagingTmp = File(parent, "$dbName.import.tmp")
        val dbTmp = File(parent, "$dbName.tmp")
        stagingTmp.takeIf { it.exists() }?.delete()
        dbTmp.takeIf { it.exists() }?.delete()

        emit(DownloadState.Progress(0, 0L, 0L, 0))
        try {
            val resolver = context.contentResolver
            var totalBytes = 0L
            try {
                resolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1 && cursor.moveToFirst()) {
                        totalBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L)
                    }
                }
            } catch (_: Exception) {
            }
            val sourceInput = resolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Cannot open selected file")
            sourceInput.use { input ->
                FileOutputStream(stagingTmp).use { output ->
                    copyWithProgress(input, output, totalBytes) { percent, done, total, speed ->
                        emit(DownloadState.Progress(percent, done, total, speed))
                    }
                }
            }

            val candidate: File = if (DbFileValidator.isZip(stagingTmp)) {
                try {
                    unzipSingleDb(stagingTmp, dbTmp) { percent ->
                        emit(DownloadState.Extracting(percent))
                    }
                    dbTmp
                } finally {
                    stagingTmp.takeIf { it.exists() }?.delete()
                }
            } else {
                stagingTmp.renameTo(dbTmp)
                dbTmp
            }

            if (!DbFileValidator.hasSqliteHeader(candidate) ||
                candidate.length() <= DatabaseConstants.DB_MIN_READY_SIZE
            ) {
                candidate.takeIf { it.exists() }?.delete()
                emit(DownloadState.Error(DownloadError.BAD_PICK))
                return@flow
            }
            if (installDbFile(candidate, targetFile)) {
                emit(DownloadState.Completed())
            } else {
                emit(DownloadState.Error(DownloadError.INSTALL_FAILED))
            }
        } catch (e: Exception) {
            stagingTmp.takeIf { it.exists() }?.delete()
            dbTmp.takeIf { it.exists() }?.delete()
            emit(DownloadState.Error(DownloadError.IMPORT_FAILED, e.localizedMessage))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadUrlToFile(
        url: String,
        dest: File,
        totalEstimate: Long,
        onProgress: suspend (percent: Int, done: Long, total: Long, speedKbps: Long) -> Unit
    ) {
        require(url.startsWith("https://")) { "Insecure URL" }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "QuranWordsApp/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Server error: code ${response.code}")
            val body = response.body ?: throw IllegalStateException("Empty server response")
            val totalBytes = if (body.contentLength() > 0) body.contentLength() else totalEstimate
            var downloaded = 0L
            val buffer = ByteArray(64 * 1024)
            var lastTime = System.currentTimeMillis()
            var bytesSince = 0L
            var speed = 0L
            var lastPercent = -1
            body.byteStream().use { input ->
                FileOutputStream(dest).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        bytesSince += read
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            speed = (bytesSince * 1000) / (elapsed * 1024)
                            lastTime = now
                            bytesSince = 0
                        }
                        val percent = ((downloaded * 100) / totalBytes.coerceAtLeast(1)).toInt().coerceIn(0, 100)
                        if (percent != lastPercent || elapsed >= 500) {
                            lastPercent = percent
                            onProgress(percent, downloaded, totalBytes, speed)
                        }
                    }
                }
            }
            if (dest.length() <= 0) throw IllegalStateException("Downloaded file is empty")
        }
    }

    private suspend fun copyWithProgress(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        totalBytes: Long,
        onProgress: suspend (percent: Int, done: Long, total: Long, speedKbps: Long) -> Unit
    ) {
        var done = 0L
        val buffer = ByteArray(64 * 1024)
        var lastTime = System.currentTimeMillis()
        var bytesSince = 0L
        var speed = 0L
        var lastPercent = -1
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            done += read
            bytesSince += read
            val now = System.currentTimeMillis()
            val elapsed = now - lastTime
            if (elapsed >= 500) {
                speed = (bytesSince * 1000) / (elapsed * 1024)
                lastTime = now
                bytesSince = 0
            }
            val denom = totalBytes.takeIf { it > 0 } ?: done.coerceAtLeast(1)
            val percent = ((done * 100) / denom).toInt().coerceIn(0, 100)
            if (percent != lastPercent || elapsed >= 500) {
                lastPercent = percent
                onProgress(percent, done, totalBytes, speed)
            }
        }
    }

    private suspend fun unzipSingleDb(zipFile: File, destDb: File, onProgress: suspend (Int) -> Unit) {
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var found = false
                var entry = zis.nextEntry
                var lastPercent = -1
                while (entry != null) {
                    val name = entry.name ?: ""
                    if (!entry.isDirectory && isSafeZipEntry(name)) {
                        if (found) throw IllegalStateException("Zip holds more than one database")
                        found = true
                        val totalEntry = entry.size.takeIf { it > 0 } ?: -1L
                        var written = 0L
                        FileOutputStream(destDb).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                written += read
                                if (totalEntry > 0) {
                                    val percent = ((written * 100) / totalEntry).toInt().coerceIn(0, 100)
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        onProgress(percent)
                                    }
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                if (!found) throw IllegalStateException("Database not found inside the zip")
            }
        }
    }

    private fun isSafeZipEntry(name: String): Boolean {
        if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) return false
        val base = name.substringAfterLast('/').substringAfterLast('\\')
        return base == DatabaseConstants.ZIP_ENTRY_NAME || base.endsWith(".db")
    }

    private fun installDbFile(staging: File, target: File): Boolean {
        try {
            io.github.ahmedsaadi0.quranwords.data.local.QuranDatabase.closeIfNeeded()
        } catch (_: Exception) {
        }
        return try {
            val parent = target.parentFile
            File(parent, "${DatabaseConstants.DB_NAME}-wal").takeIf { it.exists() }?.delete()
            File(parent, "${DatabaseConstants.DB_NAME}-shm").takeIf { it.exists() }?.delete()
            File(parent, "${DatabaseConstants.DB_NAME}-journal").takeIf { it.exists() }?.delete()
            if (target.exists()) target.delete()
            if (staging.renameTo(target)) {
                true
            } else {
                staging.copyTo(target, overwrite = true)
                staging.delete()
                target.exists() && target.length() > DatabaseConstants.DB_MIN_READY_SIZE
            }
        } catch (_: Exception) {
            false
        }
    }
}

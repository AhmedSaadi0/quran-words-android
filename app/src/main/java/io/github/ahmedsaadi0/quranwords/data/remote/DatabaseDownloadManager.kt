package io.github.ahmedsaadi0.quranwords.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed interface DownloadState {
    object Idle : DownloadState
    data class Progress(
        val percentage: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedKbps: Long = 0
    ) : DownloadState
    object Completed : DownloadState
    data class Error(val message: String) : DownloadState
}

class DatabaseDownloadManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    private val dbName = "quran_words.db"
    private val primaryUrl = "https://media.githubusercontent.com/media/AhmedSaadi0/quran-words/main/data/quran_words.db"
    private val fallbackUrl = "https://github.com/AhmedSaadi0/quran-words/raw/main/data/quran_words.db"

    fun isDatabaseReady(): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        return dbFile.exists() && dbFile.length() > 50_000_000L
    }

    fun getDatabaseFile(): File = context.getDatabasePath(dbName)

    fun downloadDatabase(): Flow<DownloadState> = flow {
        val targetFile = context.getDatabasePath(dbName)
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val tempFile = File(parent, "$dbName.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        emit(DownloadState.Progress(0, 0L, 118_534_144L, 0))

        val urls = listOf(primaryUrl, fallbackUrl)
        var succeeded = false
        var lastError = ""

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "QuranWordsApp/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    lastError = "فشل الاتصال بالخادم: كود ${response.code}"
                    continue
                }

                val body = response.body
                if (body == null) {
                    lastError = "استجابة الخادم فارغة"
                    continue
                }

                val totalBytes = if (body.contentLength() > 0) body.contentLength() else 118_534_144L

                // If GitHub returned the 134-byte git lfs pointer text, don't treat it as the DB!
                if (totalBytes < 10_000_000L) {
                    lastError = "الملف المستلم صغير جداً (مؤشر LFS)، المحاولة عبر الرابط التالي..."
                    continue
                }

                var downloadedBytes = 0L
                val buffer = ByteArray(32 * 1024)
                var lastTime = System.currentTimeMillis()
                var bytesSinceLastTime = 0L
                var currentSpeed = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        var bytesRead: Int
                        var lastPercent = -1
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            bytesSinceLastTime += bytesRead

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastTime
                            if (elapsed >= 500) {
                                currentSpeed = (bytesSinceLastTime * 1000) / (elapsed * 1024)
                                lastTime = now
                                bytesSinceLastTime = 0
                            }

                            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastPercent || elapsed >= 500) {
                                lastPercent = percent
                                emit(DownloadState.Progress(percent, downloadedBytes, totalBytes, currentSpeed))
                            }
                        }
                    }
                }

                if (tempFile.length() > 50_000_000L) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (tempFile.renameTo(targetFile)) {
                        succeeded = true
                        emit(DownloadState.Completed)
                        break
                    } else {
                        lastError = "تعذر تثبيت قاعدة البيانات في المسار المخصص"
                    }
                } else {
                    lastError = "الملف المحمّل غير مكتمل"
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "حدث خطأ أثناء تنزيل البيانات"
            }
        }

        if (!succeeded) {
            if (tempFile.exists()) tempFile.delete()
            emit(DownloadState.Error(if (lastError.isNotBlank()) lastError else "فشل تنزيل قاعدة البيانات"))
        }
    }.flowOn(Dispatchers.IO)

    fun importDatabase(sourceUri: Uri): Flow<DownloadState> = flow {
        val targetFile = context.getDatabasePath(dbName)
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val tempFile = File(parent, "$dbName.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        emit(DownloadState.Progress(0, 0L, 118_534_144L, 0))

        try {
            val resolver = context.contentResolver
            var totalBytes = 118_534_144L
            try {
                resolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1 && cursor.moveToFirst()) {
                        val size = cursor.getLong(sizeIdx)
                        if (size > 0) totalBytes = size
                    }
                }
            } catch (_: Exception) {
            }

            val inputStream = resolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("تعذر فتح الملف المختار")

            var downloadedBytes = 0L
            val buffer = ByteArray(32 * 1024)
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L
            var currentSpeed = 0L

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    var bytesRead: Int
                    var lastPercent = -1
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        bytesSinceLastTime += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            currentSpeed = (bytesSinceLastTime * 1000) / (elapsed * 1024)
                            lastTime = now
                            bytesSinceLastTime = 0
                        }

                        val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        if (percent != lastPercent || elapsed >= 500) {
                            lastPercent = percent
                            emit(DownloadState.Progress(percent, downloadedBytes, totalBytes, currentSpeed))
                        }
                    }
                }
            }

            if (tempFile.length() > 50_000_000L) {
                // Close any open DB handles before replacing file
                try {
                    io.github.ahmedsaadi0.quranwords.data.local.QuranDatabase.closeIfNeeded()
                } catch (_: Exception) {
                }
                try {
                    // Also close raw SQLite handle if cached in repository
                    // We do it via reflection-safe direct call if repository exists; otherwise just delete wal/shm
                    val wal = File(parent, "$dbName-wal")
                    val shm = File(parent, "$dbName-shm")
                    if (wal.exists()) wal.delete()
                    if (shm.exists()) shm.delete()
                } catch (_: Exception) {
                }
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (tempFile.renameTo(targetFile)) {
                    emit(DownloadState.Completed)
                } else {
                    // Fallback copy if rename fails (e.g., cross-filesystem)
                    try {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                        if (targetFile.length() > 50_000_000L) {
                            emit(DownloadState.Completed)
                        } else {
                            emit(DownloadState.Error("تعذر تثبيت قاعدة البيانات في المسار المخصص"))
                        }
                    } catch (e: Exception) {
                        emit(DownloadState.Error("تعذر تثبيت قاعدة البيانات في المسار المخصص"))
                    }
                }
            } else {
                if (tempFile.exists()) tempFile.delete()
                emit(DownloadState.Error("الملف المختار غير مكتمل أو ليس قاعدة البيانات الصحيحة (الحجم صغير جداً)"))
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            emit(DownloadState.Error(e.localizedMessage ?: "حدث خطأ أثناء استيراد البيانات"))
        }
    }.flowOn(Dispatchers.IO)
}

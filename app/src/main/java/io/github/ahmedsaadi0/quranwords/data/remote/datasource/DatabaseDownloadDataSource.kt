package io.github.ahmedsaadi0.quranwords.data.remote.datasource

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ahmedsaadi0.quranwords.core.util.DatabaseConstants
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadState
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

interface DatabaseDownloadDataSource {
    fun isDatabaseReady(): Boolean
    fun getDatabaseFile(): java.io.File
    fun downloadDatabase(): Flow<DownloadState>
    fun importDatabase(uri: Uri): Flow<DownloadState>
}

@Singleton
class DatabaseDownloadDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) : DatabaseDownloadDataSource {
    private val delegate by lazy { DatabaseDownloadManager(context, client) }

    override fun isDatabaseReady(): Boolean = delegate.isDatabaseReady()
    override fun getDatabaseFile(): java.io.File = delegate.getDatabaseFile()
    override fun downloadDatabase(): Flow<DownloadState> = delegate.downloadDatabase()
    override fun importDatabase(uri: Uri): Flow<DownloadState> = delegate.importDatabase(uri)
}

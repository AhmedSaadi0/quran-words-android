package io.github.ahmedsaadi0.quranwords.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * User preferences contract (AGENTS §12). UI and data layers depend on this
 * interface; the DataStore implementation lives in core/datastore.
 */
interface UserPreferencesRepository {

    // Display preferences
    val language: Flow<String>
    val fontSize: Flow<Float>
    val darkModeSetting: Flow<Int>
    val dynamicColorEnabled: Flow<Boolean>

    // Reading progress
    val lastReadSurah: Flow<Int>
    val lastReadAyah: Flow<Int>

    // Bookmarks (surah keys "id", ayah keys "id:ayah")
    val bookmarkedSurahs: Flow<Set<String>>
    val bookmarkedAyat: Flow<Set<String>>

    // Installed database version bookkeeping
    val dbVersionCode: Flow<Int>
    val dbVersionName: Flow<String>
    val dismissedDbVersionCode: Flow<Int>

    suspend fun setInstalledDbVersion(code: Int, name: String)

    suspend fun setDismissedDbVersion(code: Int)

    suspend fun setLastDbCheckAt(timestamp: Long = System.currentTimeMillis())

    suspend fun setFontSize(size: Float)

    suspend fun setDarkModeSetting(mode: Int)

    suspend fun setLanguage(tag: String)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setLastRead(surahId: Int, ayahNum: Int)

    suspend fun toggleSurahBookmark(surahId: Int)

    suspend fun toggleAyahBookmark(surahId: Int, ayahNum: Int)
}
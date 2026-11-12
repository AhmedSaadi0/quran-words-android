package com.quranwords.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * User preferences contract (AGENTS §12). UI and data layers depend on this
 * interface; the DataStore implementation lives in core/datastore.
 */
interface UserPreferencesRepository {

    // Display preferences
    val language: Flow<String>
    val fontSize: Flow<Float>
    val quranFontKey: Flow<String>
    val darkModeSetting: Flow<Int>
    val dynamicColorEnabled: Flow<Boolean>

    // Reading-chrome pill transparency (0 opaque .. 0.8), applied only while
    // the status bar is hidden (immersive reading).
    val pillTransparency: Flow<Float>

    // Reading layout mode (0 = vertical scroll, 1 = horizontal book pages).
    val readingMode: Flow<Int>

    // Reading progress
    val lastReadSurah: Flow<Int>
    val lastReadAyah: Flow<Int>

    // Bookmarks (surah keys "id", ayah keys "id:ayah")
    val bookmarkedSurahs: Flow<Set<String>>
    val bookmarkedAyat: Flow<Set<String>>

    // Recent searches — ordered MRU list (most-recent-first), capped at 10.
    // Order is part of the contract (see Decision #30 Fix #1): StringSet is
    // forbidden because it is unordered.
    val recentSearches: Flow<List<String>>

    // Installed database version bookkeeping
    val dbVersionCode: Flow<Int>
    val dbVersionName: Flow<String>
    val dismissedDbVersionCode: Flow<Int>

    suspend fun setInstalledDbVersion(code: Int, name: String)

    suspend fun setDismissedDbVersion(code: Int)

    suspend fun setLastDbCheckAt(timestamp: Long = System.currentTimeMillis())

    suspend fun setFontSize(size: Float)

    suspend fun setPillTransparency(transparency: Float)

    suspend fun setReadingMode(mode: Int)

    suspend fun setQuranFontKey(key: String)

    suspend fun setDarkModeSetting(mode: Int)

    suspend fun setLanguage(tag: String)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setLastRead(surahId: Int, ayahNum: Int)

    suspend fun toggleSurahBookmark(surahId: Int)

    suspend fun toggleAyahBookmark(surahId: Int, ayahNum: Int)

    suspend fun addRecentSearch(query: String)

    suspend fun removeRecentSearch(query: String)

    suspend fun clearRecentSearches()
}
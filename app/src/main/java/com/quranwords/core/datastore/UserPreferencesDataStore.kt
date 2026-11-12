package com.quranwords.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.quranwords.core.util.FontSizePreference
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.core.util.PillTransparencyPreference
import com.quranwords.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Private to this file — no global Context.dataStore extension (AGENTS §12).
private val Context.preferencesDataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(
    name = "quran_words_prefs"
)

/**
 * DataStore implementation of [UserPreferencesRepository]. Keys are private;
 * bookmark key formats ("id", "id:ayah") are parsed by core.util.BookmarkRef.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private companion object {
        val KEY_FONT_SIZE = floatPreferencesKey("quran_font_size")
        val KEY_PILL_TRANSPARENCY = floatPreferencesKey("reading_pill_transparency")
        val KEY_READING_MODE = intPreferencesKey("reading_mode")
        val KEY_DARK_MODE = intPreferencesKey("dark_mode_mode") // 0 = system, 1 = light, 2 = dark
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val KEY_LAST_SURAH = intPreferencesKey("last_read_surah")
        val KEY_LAST_AYAH = intPreferencesKey("last_read_ayah")
        val KEY_BOOKMARKED_SURAHS = stringSetPreferencesKey("bookmarked_surahs")
        val KEY_BOOKMARKED_AYAT = stringSetPreferencesKey("bookmarked_ayat")
        val KEY_DB_VERSION_CODE = intPreferencesKey("db_version_code")
        val KEY_DB_VERSION_NAME = stringPreferencesKey("db_version_name")
        val KEY_DB_INSTALLED_AT = longPreferencesKey("db_installed_at")
        val KEY_DISMISSED_DB_VERSION = intPreferencesKey("dismissed_db_version_code")
        val KEY_LAST_DB_CHECK_AT = longPreferencesKey("last_db_check_at")
        val KEY_LANGUAGE = stringPreferencesKey("app_language") // system|ar|en
        val KEY_QURAN_FONT = stringPreferencesKey("quran_font_key")
        // Fix #1: ordered MRU via string key, NOT StringSet (unordered).
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_search_queries")
        const val RECENT_SEARCH_LIMIT = 10
        const val RECENT_SEARCH_DELIM = "\n"
    }

    /** Mirrors ui.theme.QuranFont keys; unknown stored values fall back to default. */
    private val KNOWN_QURAN_FONT_KEYS = setOf(
        "kfgqpc_hafs_1441",
        "amiri_quran",
        "scheherazade_new",
        "noto_naskh_arabic"
    )
    private val DEFAULT_QURAN_FONT_KEY = "kfgqpc_hafs_1441"

    override val language: Flow<String> = context.preferencesDataStore.data.map { preferences ->
        (preferences[KEY_LANGUAGE] ?: "system").takeIf { it in setOf("system", "ar", "en") } ?: "system"
    }

    override val fontSize: Flow<Float> = context.preferencesDataStore.data.map { preferences ->
        FontSizePreference.clamp(preferences[KEY_FONT_SIZE] ?: FontSizePreference.DEFAULT)
    }

    override val pillTransparency: Flow<Float> = context.preferencesDataStore.data.map { preferences ->
        PillTransparencyPreference.clamp(preferences[KEY_PILL_TRANSPARENCY] ?: PillTransparencyPreference.DEFAULT)
    }

    override val readingMode: Flow<Int> = context.preferencesDataStore.data.map { preferences ->
        ReadingModePreference.fromValue(preferences[KEY_READING_MODE] ?: ReadingModePreference.DEFAULT)
    }

    override val quranFontKey: Flow<String> = context.preferencesDataStore.data.map { preferences ->
        (preferences[KEY_QURAN_FONT] ?: DEFAULT_QURAN_FONT_KEY)
            .takeIf { it in KNOWN_QURAN_FONT_KEYS } ?: DEFAULT_QURAN_FONT_KEY
    }

    override val darkModeSetting: Flow<Int> = context.preferencesDataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: 0
    }

    override val dynamicColorEnabled: Flow<Boolean> = context.preferencesDataStore.data.map { preferences ->
        preferences[KEY_DYNAMIC_COLOR] ?: false
    }

    override val lastReadSurah: Flow<Int> = context.preferencesDataStore.data.map { preferences ->
        preferences[KEY_LAST_SURAH] ?: 1
    }

    override val lastReadAyah: Flow<Int> = context.preferencesDataStore.data.map { preferences ->
        preferences[KEY_LAST_AYAH] ?: 1
    }

    override val bookmarkedSurahs: Flow<Set<String>> = context.preferencesDataStore.data.map { preferences ->
        preferences[KEY_BOOKMARKED_SURAHS] ?: emptySet()
    }

    override val bookmarkedAyat: Flow<Set<String>> = context.preferencesDataStore.data.map { preferences ->
        preferences[KEY_BOOKMARKED_AYAT] ?: emptySet()
    }

    override val recentSearches: Flow<List<String>> = context.preferencesDataStore.data.map { preferences ->
        val raw = preferences[KEY_RECENT_SEARCHES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(RECENT_SEARCH_DELIM).filter { it.isNotBlank() }.take(RECENT_SEARCH_LIMIT)
    }

    override val dbVersionCode: Flow<Int> = context.preferencesDataStore.data.map { it[KEY_DB_VERSION_CODE] ?: 0 }

    override val dbVersionName: Flow<String> = context.preferencesDataStore.data.map { it[KEY_DB_VERSION_NAME] ?: "" }

    override val dismissedDbVersionCode: Flow<Int> =
        context.preferencesDataStore.data.map { it[KEY_DISMISSED_DB_VERSION] ?: 0 }

    override suspend fun setInstalledDbVersion(code: Int, name: String) {
        context.preferencesDataStore.edit {
            it[KEY_DB_VERSION_CODE] = code
            it[KEY_DB_VERSION_NAME] = name
            it[KEY_DB_INSTALLED_AT] = System.currentTimeMillis()
        }
    }

    override suspend fun setDismissedDbVersion(code: Int) {
        context.preferencesDataStore.edit { it[KEY_DISMISSED_DB_VERSION] = code }
    }

    override suspend fun setLastDbCheckAt(timestamp: Long) {
        context.preferencesDataStore.edit { it[KEY_LAST_DB_CHECK_AT] = timestamp }
    }

    override suspend fun setFontSize(size: Float) {
        context.preferencesDataStore.edit { it[KEY_FONT_SIZE] = FontSizePreference.clamp(size) }
    }

    override suspend fun setPillTransparency(transparency: Float) {
        context.preferencesDataStore.edit {
            it[KEY_PILL_TRANSPARENCY] = PillTransparencyPreference.clamp(transparency)
        }
    }

    override suspend fun setReadingMode(mode: Int) {
        context.preferencesDataStore.edit { it[KEY_READING_MODE] = ReadingModePreference.fromValue(mode) }
    }

    override suspend fun setQuranFontKey(key: String) {
        val normalized = key.takeIf { it in KNOWN_QURAN_FONT_KEYS } ?: DEFAULT_QURAN_FONT_KEY
        context.preferencesDataStore.edit { it[KEY_QURAN_FONT] = normalized }
    }

    override suspend fun setDarkModeSetting(mode: Int) {
        context.preferencesDataStore.edit { it[KEY_DARK_MODE] = mode }
    }

    override suspend fun setLanguage(tag: String) {
        val normalized = tag.takeIf { it in setOf("system", "ar", "en") } ?: "system"
        context.preferencesDataStore.edit { it[KEY_LANGUAGE] = normalized }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setLastRead(surahId: Int, ayahNum: Int) {
        context.preferencesDataStore.edit {
            it[KEY_LAST_SURAH] = surahId
            it[KEY_LAST_AYAH] = ayahNum
        }
    }

    override suspend fun toggleSurahBookmark(surahId: Int) {
        context.preferencesDataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKED_SURAHS] ?: emptySet()
            val key = surahId.toString()
            prefs[KEY_BOOKMARKED_SURAHS] = if (current.contains(key)) current - key else current + key
        }
    }

    override suspend fun toggleAyahBookmark(surahId: Int, ayahNum: Int) {
        context.preferencesDataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKED_AYAT] ?: emptySet()
            val key = "$surahId:$ayahNum"
            prefs[KEY_BOOKMARKED_AYAT] = if (current.contains(key)) current - key else current + key
        }
    }

    override suspend fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isBlank() || q.contains('\n') || q.length > 80) return
        context.preferencesDataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
            val current = if (raw.isBlank()) emptyList() else raw.split(RECENT_SEARCH_DELIM).filter { it.isNotBlank() }
            // MRU: dedup + move to head, then cap.
            val next = (listOf(q) + current.filterNot { it == q }).take(RECENT_SEARCH_LIMIT)
            prefs[KEY_RECENT_SEARCHES] = next.joinToString(RECENT_SEARCH_DELIM)
        }
    }

    override suspend fun removeRecentSearch(query: String) {
        context.preferencesDataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_SEARCHES] ?: ""
            val current = if (raw.isBlank()) emptyList() else raw.split(RECENT_SEARCH_DELIM).filter { it.isNotBlank() }
            prefs[KEY_RECENT_SEARCHES] = current.filterNot { it == query }.joinToString(RECENT_SEARCH_DELIM)
        }
    }

    override suspend fun clearRecentSearches() {
        context.preferencesDataStore.edit { it[KEY_RECENT_SEARCHES] = "" }
    }
}
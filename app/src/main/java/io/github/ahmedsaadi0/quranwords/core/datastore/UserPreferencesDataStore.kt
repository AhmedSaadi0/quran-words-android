package io.github.ahmedsaadi0.quranwords.core.datastore

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
import io.github.ahmedsaadi0.quranwords.domain.repository.UserPreferencesRepository
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
    }

    override val language: Flow<String> = context.preferencesDataStore.data.map { preferences ->
        (preferences[KEY_LANGUAGE] ?: "system").takeIf { it in setOf("system", "ar", "en") } ?: "system"
    }

    override val fontSize: Flow<Float> = context.preferencesDataStore.data.map { preferences ->
        (preferences[KEY_FONT_SIZE] ?: 24f).coerceIn(1f, 48f)
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
        context.preferencesDataStore.edit { it[KEY_FONT_SIZE] = size.coerceIn(1f, 48f) }
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
}
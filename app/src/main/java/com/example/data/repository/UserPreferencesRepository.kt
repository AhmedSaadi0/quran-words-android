package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quran_words_prefs")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        private val KEY_FONT_SIZE = floatPreferencesKey("quran_font_size")
        private val KEY_DARK_MODE = intPreferencesKey("dark_mode_mode") // 0 = system, 1 = light, 2 = dark
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled") // false = Natural, true = Dynamic (harmonized)
        private val KEY_COLOR_MODE = intPreferencesKey("color_mode") // 0 = Natural, 1 = Dynamic, 2 = System (future)
        private val KEY_LAST_SURAH = intPreferencesKey("last_read_surah")
        private val KEY_LAST_AYAH = intPreferencesKey("last_read_ayah")
        private val KEY_DISMISSED_SETUP = booleanPreferencesKey("dismissed_setup")
        private val KEY_BOOKMARKED_SURAHS = stringSetPreferencesKey("bookmarked_surahs")
        private val KEY_BOOKMARKED_AYAT = stringSetPreferencesKey("bookmarked_ayat") // format "surahId:ayahNum"
    }

    val fontSize: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_FONT_SIZE] ?: 24f
    }

    val darkModeSetting: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: 0
    }

    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DYNAMIC_COLOR] ?: false
    }

    // Legacy alias for new color mode - maps to dynamic boolean for simplicity
    val colorMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_COLOR_MODE] ?: 0 // 0 Natural, 1 Dynamic
    }

    val lastReadSurah: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_SURAH] ?: 1
    }

    val lastReadAyah: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_AYAH] ?: 1
    }

    val isSetupDismissed: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DISMISSED_SETUP] ?: false
    }

    val bookmarkedSurahs: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_BOOKMARKED_SURAHS] ?: emptySet()
    }

    val bookmarkedAyat: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_BOOKMARKED_AYAT] ?: emptySet()
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setDarkModeSetting(mode: Int) {
        context.dataStore.edit { it[KEY_DARK_MODE] = mode }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_DYNAMIC_COLOR] = enabled
            it[KEY_COLOR_MODE] = if (enabled) 1 else 0
        }
    }

    suspend fun setColorMode(mode: Int) {
        context.dataStore.edit {
            it[KEY_COLOR_MODE] = mode
            it[KEY_DYNAMIC_COLOR] = mode == 1
        }
    }

    suspend fun setLastRead(surahId: Int, ayahNum: Int) {
        context.dataStore.edit {
            it[KEY_LAST_SURAH] = surahId
            it[KEY_LAST_AYAH] = ayahNum
        }
    }

    suspend fun setSetupDismissed(dismissed: Boolean) {
        context.dataStore.edit {
            it[KEY_DISMISSED_SETUP] = dismissed
        }
    }

    suspend fun toggleSurahBookmark(surahId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKED_SURAHS] ?: emptySet()
            val key = surahId.toString()
            prefs[KEY_BOOKMARKED_SURAHS] = if (current.contains(key)) current - key else current + key
        }
    }

    suspend fun toggleAyahBookmark(surahId: Int, ayahNum: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKED_AYAT] ?: emptySet()
            val key = "$surahId:$ayahNum"
            prefs[KEY_BOOKMARKED_AYAT] = if (current.contains(key)) current - key else current + key
        }
    }

    suspend fun isSurahBookmarked(surahId: Int): Boolean {
        val set = context.dataStore.data.map { it[KEY_BOOKMARKED_SURAHS] ?: emptySet() }
        // Use datastore snapshot via edit? We'll implement via flow first() in ViewModel; this helper not used directly
        return false
    }
}

package io.github.ahmedsaadi0.quranwords.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferencesKeys {
    val FONT_SIZE = floatPreferencesKey("quran_font_size")
    val DARK_MODE = intPreferencesKey("dark_mode_mode") // 0 system, 1 light, 2 dark
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
    val COLOR_MODE = intPreferencesKey("color_mode")
    val LAST_SURAH = intPreferencesKey("last_read_surah")
    val LAST_AYAH = intPreferencesKey("last_read_ayah")
    val DISMISSED_SETUP = booleanPreferencesKey("dismissed_setup")
    val BOOKMARKED_SURAHS = stringSetPreferencesKey("bookmarked_surahs")
    val BOOKMARKED_AYAT = stringSetPreferencesKey("bookmarked_ayat")
}

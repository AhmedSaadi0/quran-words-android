package io.github.ahmedsaadi0.quranwords.core.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App language tags persisted in DataStore ("system" is the default on fresh install).
 * "system" clears the per-app override so the app follows the OS locale dynamically.
 */
object AppLanguage {
    const val SYSTEM = "system"
    const val ARABIC = "ar"
    const val ENGLISH = "en"

    val ALL = setOf(SYSTEM, ARABIC, ENGLISH)
}

/**
 * Thin wrapper over [AppCompatDelegate.setApplicationLocales] (backports per-app
 * locales to Min SDK 24; framework handles API 33+).
 *
 * Pure mapping lives in [resolveTags]/[toLocaleList] so it stays unit-testable;
 * [apply] is the only Android side effect.
 */
@Singleton
class LanguageManager @Inject constructor() {

    /** Maps a stored tag to a BCP-47 tag, or null for system default. */
    fun resolveTags(tag: String): String? = when (tag) {
        AppLanguage.ARABIC -> AppLanguage.ARABIC
        AppLanguage.ENGLISH -> AppLanguage.ENGLISH
        else -> null
    }

    fun toLocaleList(tag: String): LocaleListCompat {
        val tags = resolveTags(tag)
        return if (tags == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tags)
        }
    }

    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(toLocaleList(tag))
    }
}

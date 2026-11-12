package com.quranwords.ui.settings

import com.quranwords.core.util.ReadingModePreference
import com.quranwords.ui.theme.QuranFont

data class SettingsUiState(
    val darkModeSetting: Int = 0,
    val dynamicColorEnabled: Boolean = false,
    val language: String = "",
    val quranFont: QuranFont = QuranFont.KFGQPC_HAFS_1441,
    /** Read-only here: the reading mode feeds the navigation transitions. */
    val readingMode: Int = ReadingModePreference.DEFAULT
)

sealed interface SettingsEvent {
    data class DarkModeChanged(val mode: Int) : SettingsEvent
    data class DynamicColorChanged(val enabled: Boolean) : SettingsEvent
    data class LanguageChanged(val tag: String) : SettingsEvent
    data class QuranFontChanged(val key: String) : SettingsEvent
}
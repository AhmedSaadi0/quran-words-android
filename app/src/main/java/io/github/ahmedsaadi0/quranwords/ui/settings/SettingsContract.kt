package io.github.ahmedsaadi0.quranwords.ui.settings

import io.github.ahmedsaadi0.quranwords.ui.theme.QuranFont

data class SettingsUiState(
    val darkModeSetting: Int = 0,
    val dynamicColorEnabled: Boolean = false,
    val language: String = "",
    val quranFont: QuranFont = QuranFont.KFGQPC_HAFS_1441
)

sealed interface SettingsEvent {
    data class DarkModeChanged(val mode: Int) : SettingsEvent
    data class DynamicColorChanged(val enabled: Boolean) : SettingsEvent
    data class LanguageChanged(val tag: String) : SettingsEvent
    data class QuranFontChanged(val key: String) : SettingsEvent
}
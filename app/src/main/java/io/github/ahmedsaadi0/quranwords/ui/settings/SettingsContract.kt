package io.github.ahmedsaadi0.quranwords.ui.settings

data class SettingsUiState(
    val darkModeSetting: Int = 0,
    val dynamicColorEnabled: Boolean = false,
    val language: String = ""
)

sealed interface SettingsEvent {
    data class DarkModeChanged(val mode: Int) : SettingsEvent
    data class DynamicColorChanged(val enabled: Boolean) : SettingsEvent
    data class LanguageChanged(val tag: String) : SettingsEvent
}
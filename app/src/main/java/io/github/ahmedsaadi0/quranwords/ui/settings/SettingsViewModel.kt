package io.github.ahmedsaadi0.quranwords.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ahmedsaadi0.quranwords.core.util.AppLanguage
import io.github.ahmedsaadi0.quranwords.core.util.LanguageManager
import io.github.ahmedsaadi0.quranwords.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns theme + language app preferences (Decision 14): MainActivity reads the
 * flows for theming; the settings dialog routes events here. `colorMode` was
 * dead UI state and is intentionally not exposed.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _darkModeSetting = MutableStateFlow(0)
    private val _dynamicColorEnabled = MutableStateFlow(false)
    private val _language = MutableStateFlow(AppLanguage.SYSTEM)

    val uiState: StateFlow<SettingsUiState> = combine(
        _darkModeSetting,
        _dynamicColorEnabled,
        _language
    ) { darkModeSetting, dynamicColorEnabled, language ->
        SettingsUiState(
            darkModeSetting = darkModeSetting,
            dynamicColorEnabled = dynamicColorEnabled,
            language = language
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        viewModelScope.launch {
            preferences.darkModeSetting.collect { _darkModeSetting.value = it }
        }
        viewModelScope.launch {
            preferences.dynamicColorEnabled.collect { _dynamicColorEnabled.value = it }
        }
        viewModelScope.launch {
            preferences.language.collect { tag ->
                _language.value = tag
                languageManager.apply(tag)
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.DarkModeChanged -> setDarkMode(event.mode)
            is SettingsEvent.DynamicColorChanged -> setDynamicColor(event.enabled)
            is SettingsEvent.LanguageChanged -> setLanguage(event.tag)
        }
    }

    /** Cycles system → dark → light (legacy toggle behavior). */
    fun toggleDarkMode() {
        viewModelScope.launch {
            val next = when (_darkModeSetting.value) {
                1 -> 2 // light → dark
                2 -> 1 // dark → light
                else -> 2 // system → dark
            }
            preferences.setDarkModeSetting(next)
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            preferences.setDynamicColorEnabled(!_dynamicColorEnabled.value)
        }
    }

    private fun setDarkMode(mode: Int) {
        viewModelScope.launch { preferences.setDarkModeSetting(mode) }
    }

    private fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColorEnabled(enabled) }
    }

    private fun setLanguage(tag: String) {
        viewModelScope.launch { preferences.setLanguage(tag) }
    }
}
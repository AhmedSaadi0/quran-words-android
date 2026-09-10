package io.github.ahmedsaadi0.quranwords.ui.viewmodel

import io.github.ahmedsaadi0.quranwords.core.util.AppLanguage

/**
 * Single app-level preferences snapshot (AGENTS §10.1). Consumers migrate to
 * this one state instead of collecting ten granular MainViewModel flows; the
 * granular flows remain as source-of-truth delegates until the last screen
 * migrates (UI_REFACTOR_PLAN Phase 10, Decision 14).
 */
data class AppPreferencesUiState(
    val fontSize: Float = 24f,
    val darkModeSetting: Int = 0,
    val dynamicColorEnabled: Boolean = false,
    val colorMode: Int = 0,
    val bookmarkedSurahs: Set<String> = emptySet(),
    val bookmarkedAyat: Set<String> = emptySet(),
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val language: String = AppLanguage.SYSTEM,
    val isDbReady: Boolean = false
)
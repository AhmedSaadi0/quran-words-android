package io.github.ahmedsaadi0.quranwords.ui.home

import io.github.ahmedsaadi0.quranwords.domain.model.RootItem

data class HomeUiState(
    val isLoading: Boolean = true,
    val isDbReady: Boolean = false,
    val featuredRoots: List<RootItem> = emptyList(),
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val lastSurahNameAr: String = "الفاتحة",
    val darkModeSetting: Int = 0,
    val dynamicColorEnabled: Boolean = false,
    val bookmarkedSurahsCount: Int = 0,
    val bookmarkedAyatCount: Int = 0,
    val hasBookmarks: Boolean = false,
    val error: String? = null
)

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data object ToggleDynamicColor : HomeEvent
    data object ToggleDarkMode : HomeEvent
    data class SetDarkMode(val mode: Int) : HomeEvent
    data class SetDynamicColor(val enabled: Boolean) : HomeEvent
    data object NavigateToSurahIndex : HomeEvent
    data object NavigateToRoots : HomeEvent
    data object NavigateToGuide : HomeEvent
    data object NavigateToSearch : HomeEvent
    data object NavigateToSetup : HomeEvent
    data object NavigateToBookmarks : HomeEvent
    data class NavigateToSurahDetail(val surahId: Int, val ayah: Int) : HomeEvent
    data class NavigateToRootDetail(val rootId: Int) : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToSurahDetail(val surahId: Int, val ayah: Int) : HomeEffect
    data class NavigateToRootDetail(val rootId: Int) : HomeEffect
    data object NavigateToSurahIndex : HomeEffect
    data object NavigateToRoots : HomeEffect
    data object NavigateToSearch : HomeEffect
    data object NavigateToGuide : HomeEffect
    data object NavigateToSetup : HomeEffect
    data object NavigateToBookmarks : HomeEffect
}

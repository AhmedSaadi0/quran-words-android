package com.quranwords.ui.home

import com.quranwords.domain.model.RootItem

data class HomeUiState(
    val featuredRoots: List<RootItem> = emptyList(),
    val isDbReady: Boolean = false,
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val bookmarkedSurahs: Set<String> = emptySet(),
    val bookmarkedAyat: Set<String> = emptySet()
)
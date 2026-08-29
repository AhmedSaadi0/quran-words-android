package io.github.ahmedsaadi0.quranwords.domain.usecase

import io.github.ahmedsaadi0.quranwords.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBookmarksUseCase @Inject constructor(
    private val prefs: UserPreferencesRepository
) {
    fun observeSurahs(): Flow<Set<String>> = prefs.bookmarkedSurahs
    fun observeAyat(): Flow<Set<String>> = prefs.bookmarkedAyat
}

class ToggleSurahBookmarkUseCase @Inject constructor(
    private val prefs: UserPreferencesRepository
) {
    suspend operator fun invoke(surahId: Int) {
        if (surahId in 1..114) prefs.toggleSurahBookmark(surahId)
    }
}

class ToggleAyahBookmarkUseCase @Inject constructor(
    private val prefs: UserPreferencesRepository
) {
    suspend operator fun invoke(surahId: Int, ayahNum: Int) {
        if (surahId in 1..114 && ayahNum >= 1) prefs.toggleAyahBookmark(surahId, ayahNum)
    }
}

class ObserveLastReadUseCase @Inject constructor(
    private val prefs: UserPreferencesRepository
) {
    fun observeSurah(): Flow<Int> = prefs.lastReadSurah
    fun observeAyah(): Flow<Int> = prefs.lastReadAyah
}

class UpdateLastReadUseCase @Inject constructor(
    private val prefs: UserPreferencesRepository
) {
    suspend operator fun invoke(surahId: Int, ayahNum: Int) {
        if (surahId in 1..114 && ayahNum >= 1) prefs.setLastRead(surahId, ayahNum)
    }
}

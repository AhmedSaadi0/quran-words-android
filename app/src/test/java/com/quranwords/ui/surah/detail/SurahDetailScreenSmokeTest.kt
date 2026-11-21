package com.quranwords.ui.surah.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.domain.model.Ayah
import com.quranwords.ui.navigation.SurahEnter
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SurahDetailScreenSmokeTest {

    @get:Rule val compose = createComposeRule()

    private val ayah = Ayah(
        id = 1, surah = 2, ayah = 1,
        textUthmani = "بِسْمِ اللَّهِ", textUthmaniPlain = "بسم الله", textImlaei = "بسم الله",
        wordCount = 0, pageNumber = 1
    )

    private fun render(state: SurahDetailUiState) {
        compose.setContent {
            val snackbar = remember { SnackbarHostState() }
            MyApplicationTheme {
                SurahDetailScreen(
                    uiState = state,
                    surahId = 2,
                    targetAyah = 1,
                    enter = SurahEnter.Normal,
                    animationsEnabled = false,
                    onEvent = {},
                    onNavigateBack = {},
                    onNavigateToAdjacentSurah = { _, _, _, _ -> },
                    onNavigateToRootDetail = {},
                    snackbarHostState = snackbar
                )
            }
        }
    }

    @Test
    fun scroll_mode_renders() {
        render(SurahDetailUiState(isLoading = false, ayat = listOf(ayah)))
        compose.onRoot().assertExists()
    }

    @Test
    fun pages_mode_renders() {
        render(
            SurahDetailUiState(
                isLoading = true,
                readingMode = ReadingModePreference.PAGES,
                totalPages = 1,
                entryPage = 1,
                currentPage = 1
            )
        )
        compose.onRoot().assertExists()
    }
}

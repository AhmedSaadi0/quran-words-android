package com.quranwords.ui.search

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.quranwords.ui.search.components.AyahSurahFilterBar
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en")
class SearchChromeLocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun english_locale_has_no_arabic_filter_labels() {
        compose.setContent {
            MyApplicationTheme {
                AyahSurahFilterBar(selectedSurahId = 1, onSelect = {})
            }
        }
        compose.onNodeWithText("Surah Al-Fatihah").assertExists()
        compose.onNodeWithText("سورة", substring = true).assertDoesNotExist()
    }
}

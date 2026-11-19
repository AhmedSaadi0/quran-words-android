package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import com.quranwords.domain.model.Ayah
import com.quranwords.ui.theme.MyApplicationTheme
import com.quranwords.ui.theme.QuranFont
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SurahAyatListUnpagedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun no_crash_for_null_page_number() {
        val ayat = listOf(
            Ayah(1, 1, 1, "نص", "نص", "نص", 1, pageNumber = null)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                SurahAyatList(
                    ayat = ayat,
                    fontSize = 24f,
                    quranFont = QuranFont.KFGQPC_HAFS_1441,
                    hasBasmalah = false,
                    isSelectionMode = false,
                    selectedAyahs = emptySet(),
                    listState = rememberLazyListState(),
                    wordInteraction = WordInteractionCallbacks(
                        onOpenWord = { _, _ -> },
                        onWordLongPressed = { },
                        onToggleChrome = { },
                        onDismissWordAction = { },
                        onCopyWord = { }
                    ),
                    onToggleSelection = { },
                    onEnterSelection = { }
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}

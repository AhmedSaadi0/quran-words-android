package com.quranwords.ui.surah.detail.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.theme.MyApplicationTheme
import com.quranwords.ui.theme.QuranFont
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WordActionPointerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun outside_tap_after_long_press_does_not_toggle_chrome() {
        var chromeToggles = 0
        var dismissals = 0
        val target = WordActionTarget(
            wordId = 1,
            word = WordToken(wordId = 1, wordAyahId = 10, position = 1, text = "كلمة", textClean = "كلمة", translation = ""),
            ayah = Ayah(1, 1, 1, "نص", "نص", "نص", 1),
            blockKey = "k_0",
            visualRect = Rect(0f, 0f, 10f, 10f)
        )
        val active = mutableStateOf<WordActionTarget?>(null)
        val group = AyahFlowGroup(key = "k", pageNumber = null, ayat = emptyList(), firstAyahIndex = 0)

        composeTestRule.setContent {
            MyApplicationTheme {
                MushafFlowBlock(
                    group = group,
                    fontSize = 24f,
                    quranFont = QuranFont.KFGQPC_HAFS_1441,
                    isSelectionMode = false,
                    selectedAyahs = emptySet(),
                    wordInteraction = WordInteractionCallbacks(
                        onOpenWord = { _, _ -> },
                        onWordLongPressed = { },
                        onToggleChrome = { chromeToggles++ },
                        onDismissWordAction = { dismissals++ },
                        onCopyWord = { }
                    ),
                    onToggleSelection = { },
                    onEnterSelection = { },
                    activeWordAction = active.value
                )
            }
        }

        // Simulate the recomposition that opens the toolbar after the pointer
        // node was installed with activeWordAction == null.
        composeTestRule.runOnIdle { active.value = target }
        composeTestRule.onNodeWithTag("mushaf_block_k_0").performClick()

        assertEquals(1, dismissals)
        assertEquals(0, chromeToggles)
    }
}

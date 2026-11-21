package com.quranwords.ui.surah.detail.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en")
class MushafAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun custom_actions_stable_across_recomposition() {
        val word = WordToken(
            wordId = 1, wordAyahId = 1, position = 1,
            text = "كلمة", textClean = "كلمة", translation = ""
        )
        val ayah = Ayah(
            id = 1, surah = 1, ayah = 1,
            textUthmani = "كلمة", textUthmaniPlain = "كلمة", textImlaei = "كلمة",
            wordCount = 1, words = listOf(word)
        )
        val group = AyahFlowGroup("1:1", 1, listOf(ayah), 0)
        val callbacks = WordInteractionCallbacks(
            onOpenWord = { _, _ -> }, onWordLongPressed = {},
            onToggleChrome = {}, onDismissWordAction = {}, onCopyWord = {}
        )
        val captured = mutableListOf<Any>()

        compose.setContent {
            var tick by mutableIntStateOf(0)
            MyApplicationTheme {
                val actions = rememberMushafAccessibilityActions(
                    group = group,
                    selectedAyahs = emptySet(),
                    bookmarkedAyahs = emptySet(),
                    isSelectionMode = false,
                    morphologyLabel = "Morphology",
                    wordInteraction = callbacks,
                    onToggleSelection = {},
                    onEnterSelection = {}
                )
                captured.add(actions)
                androidx.compose.material3.Button(onClick = { tick++ }) {
                    androidx.compose.material3.Text("tick $tick")
                }
            }
        }
        compose.onNodeWithText("tick 0", substring = true).performClick()
        compose.waitForIdle()
        assertSame(captured[0], captured[1])
    }
}

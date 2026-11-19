package com.quranwords.ui.search

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.quranwords.ui.search.components.HighlightedAyahText
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchResultCardsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderedText(): String = composeTestRule
        .onNodeWithTag("search_ayah_text")
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString("") { it.text }

    @Test
    fun real_ayah_marker_is_rendered_for_match_and_no_match() {
        val text = "مَثَلٌ يُضْرَبُ لِلنَّاسِ"

        composeTestRule.setContent {
            MyApplicationTheme {
                HighlightedAyahText(text = text, query = "مثل", ayahNumber = 35)
            }
        }
        composeTestRule.onNodeWithTag("search_ayah_text")
            .assertTextContains("﴿٣٥﴾", substring = true)
        assertFalse(renderedText().contains("﴿٠﴾"))

        composeTestRule.setContent {
            MyApplicationTheme {
                HighlightedAyahText(text = text, query = "لايوجد", ayahNumber = 35)
            }
        }
        composeTestRule.onNodeWithTag("search_ayah_text")
            .assertTextContains("﴿٣٥﴾", substring = true)
        assertFalse(renderedText().contains("﴿٠﴾"))
    }
}

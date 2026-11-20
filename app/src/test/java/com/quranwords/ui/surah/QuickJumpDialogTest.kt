package com.quranwords.ui.surah

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.quranwords.ui.surah.components.QuickJumpDialog
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuickJumpDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun invalid_page_and_invalid_ayah_show_error_and_clear_on_edit() {
        val jumps = mutableListOf<Pair<Int, Int>>()

        composeTestRule.setContent {
            MyApplicationTheme {
                QuickJumpDialog(
                    onJumpToPage = { },
                    onJumpToSurahAyah = { s, a -> jumps.add(s to a) },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("quick_jump_page").performTextReplacement("0")
        composeTestRule.onNodeWithTag("quick_jump_go_page").performClick()
        composeTestRule.onNodeWithText("Check the range").assertIsDisplayed()

        composeTestRule.onNodeWithTag("quick_jump_page").performTextReplacement("1")
        composeTestRule.onNodeWithText("Check the range").assertDoesNotExist()

        composeTestRule.onNodeWithTag("quick_jump_surah").performTextReplacement("2")
        composeTestRule.onNodeWithTag("quick_jump_ayah").performTextReplacement("999")
        composeTestRule.onNodeWithTag("quick_jump_go_ayah").performClick()
        composeTestRule.onNodeWithText("Check the range").assertIsDisplayed()

        composeTestRule.onNodeWithTag("quick_jump_ayah").performTextReplacement("1")
        composeTestRule.onNodeWithText("Check the range").assertDoesNotExist()
        composeTestRule.onNodeWithTag("quick_jump_go_ayah").performClick()
        assertEquals(listOf(2 to 1), jumps)
    }
}

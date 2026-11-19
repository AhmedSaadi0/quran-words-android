package com.quranwords.ui.surah.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SurahDetailErrorUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun error_message_and_retry_are_rendered() {
        var retried = false

        composeTestRule.setContent {
            MyApplicationTheme {
                SurahErrorState(message = "read failed", onRetry = { retried = true })
            }
        }

        composeTestRule.onNodeWithText("read failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }
}

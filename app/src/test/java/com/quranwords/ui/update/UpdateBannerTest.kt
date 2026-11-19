package com.quranwords.ui.update

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class UpdateBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun failed_banner_renders_message_and_dismiss_action() {
        var dismissed = false

        composeTestRule.setContent {
            MyApplicationTheme {
                UpdateFailedBanner(onDismiss = { dismissed = true })
            }
        }

        composeTestRule.onNodeWithTag(UpdateBannerTags.ROOT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(UpdateBannerTags.DISMISS_ACTION)
            .assertIsDisplayed()
            .performClick()
        assertTrue(dismissed)
    }
}

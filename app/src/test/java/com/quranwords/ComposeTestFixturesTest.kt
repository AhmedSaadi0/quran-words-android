package com.quranwords

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComposeTestFixturesTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun setThemedContent_renders_tagged_text() {
        compose.setThemedContent(rtl = true) {
            Text("مرحبا", modifier = Modifier.testTag("fixture_text"))
        }
        compose.onNodeWithTag("fixture_text").assertIsDisplayed()
    }
}

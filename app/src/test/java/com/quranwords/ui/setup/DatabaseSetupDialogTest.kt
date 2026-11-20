package com.quranwords.ui.setup

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
class DatabaseSetupDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_state_restored() {
        val events = mutableListOf<SetupEvent>()

        composeTestRule.setContent {
            MyApplicationTheme {
                DatabaseSetupScreen(
                    uiState = SetupUiState(importDialogVisible = true),
                    onEvent = { events.add(it) },
                    onPickImportFile = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("confirm_import_button").assertIsDisplayed().performClick()
        assertTrue(events.any { it is SetupEvent.SetImportDialogVisible && !it.visible })
    }
}

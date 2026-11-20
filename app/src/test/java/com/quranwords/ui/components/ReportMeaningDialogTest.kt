package com.quranwords.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.quranwords.ui.theme.MyApplicationTheme
import com.quranwords.util.MeaningReportType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReportMeaningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_renders_with_plain_state_and_emits_callbacks() {
        var dismissed = false
        var copied = false

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportMeaningDialog(
                    rootText = "كتب",
                    aiSummary = "ملخص",
                    state = ReportMeaningUiState(
                        reportType = MeaningReportType.INCORRECT,
                        description = "وصف",
                        canSubmit = true
                    ),
                    onReportTypeChange = {},
                    onDescriptionChange = {},
                    onSuggestionChange = {},
                    onCopyReport = { copied = true },
                    onShareReport = {},
                    onOpenUrl = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("copy_report_btn").assertIsEnabled().performClick()
        assertTrue(copied)

        composeTestRule.onNodeWithTag("open_github_report_btn").performClick()
        assertTrue(dismissed)
    }
}

package com.quranwords.ui.setup

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseSetupScreenTest {

    @get:Rule val compose = createComposeRule()

    private fun render(state: SetupDownload) {
        compose.setContent {
            MyApplicationTheme {
                SetupDownloadStates(
                    state = state,
                    latestRelease = null,
                    installedVersion = DbInstalledVersion(),
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }
    }

    @Test
    fun idle_shows_start_button() {
        render(SetupDownload.Idle)
        compose.onNodeWithTag("start_download_button").assertExists()
    }

    @Test
    fun error_shows_retry_button() {
        render(SetupDownload.Error(SetupError.NETWORK, "offline"))
        compose.onNodeWithTag("retry_download_button").assertExists()
    }

    @Test
    fun completed_shows_finish_button() {
        render(SetupDownload.Completed(1, "v1"))
        compose.onNodeWithTag("finish_setup_button").assertExists()
    }

    @Test
    fun progress_states_have_no_cta() {
        render(SetupDownload.Progress(40, 1L, 2L, 0L, false))
        compose.onNodeWithTag("start_download_button").assertDoesNotExist()
        compose.onNodeWithTag("retry_download_button").assertDoesNotExist()
        compose.onNodeWithTag("finish_setup_button").assertDoesNotExist()

        render(SetupDownload.Extracting(80))
        compose.onNodeWithTag("start_download_button").assertDoesNotExist()
        compose.onNodeWithTag("retry_download_button").assertDoesNotExist()
        compose.onNodeWithTag("finish_setup_button").assertDoesNotExist()
    }
}

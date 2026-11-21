package com.quranwords.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DbUpdateBannerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun shows_mb_size_and_vector_icon() {
        val info = DbReleaseInfo(
            versionCode = 2,
            versionName = "v2",
            downloadUrl = "https://example.test/db.zip",
            compressedSize = 2L * 1024 * 1024
        )
        compose.setContent {
            MyApplicationTheme {
                DbUpdateBanner(info = info, installedName = "", onUpdateClick = {}, onDismissClick = {})
            }
        }
        compose.onNodeWithTag("db_update_banner").assertIsDisplayed()
        compose.onNodeWithTag("db_update_icon").assertExists()
        compose.onNodeWithText("2 MB", substring = true).assertExists()
    }
}

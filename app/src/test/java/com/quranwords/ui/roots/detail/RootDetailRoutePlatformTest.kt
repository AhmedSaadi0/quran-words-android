package com.quranwords.ui.roots.detail

import android.app.Application
import android.content.Intent
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.quranwords.core.util.BuildInfo
import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.domain.model.DbUpdateState
import com.quranwords.domain.repository.DbCheckResult
import com.quranwords.domain.repository.DbUpdateRepository
import com.quranwords.fake.FakeQuranRepository
import com.quranwords.fake.FakeUserPreferences
import com.quranwords.ui.roots.detail.report.ReportMeaningViewModel
import com.quranwords.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RootDetailRoutePlatformTest {

    @get:Rule val compose = createComposeRule()

    private class FakeDbUpdate : DbUpdateRepository {
        override suspend fun getInstalledVersion() = DbInstalledVersion(1, "v1")
        override suspend fun setInstalledVersion(code: Int, name: String) = Unit
        override suspend fun getDismissedVersionCode() = 0
        override suspend fun setDismissedVersionCode(code: Int) = Unit
        override suspend fun getLatestRelease(): DbCheckResult<DbReleaseInfo> = DbCheckResult.Error("unused")
        override suspend fun checkForUpdate(): DbCheckResult<DbUpdateState> =
            DbCheckResult.Success(DbUpdateState.UpToDate)
    }

    @Test
    fun report_copy_and_share_route_to_platform_handlers() {
        val rootVm = RootDetailViewModel(FakeQuranRepository(), FakeUserPreferences(), SavedStateHandle())
        val reportVm = ReportMeaningViewModel(
            FakeDbUpdate(), BuildInfo("1.0", "14", "en")
        )
        reportVm.setDescription("وصف كافٍ يتجاوز الحد الأدنى المطلوب للبلاغ")
        rootVm.setReportVisible(true)
        lateinit var clipboard: ClipboardManager

        compose.setContent {
            clipboard = LocalClipboardManager.current
            MyApplicationTheme {
                RootDetailRoute(
                    rootId = 1,
                    onNavigateBack = {},
                    onNavigateToSurahDetail = { _, _ -> },
                    viewModel = rootVm,
                    reportViewModel = reportVm
                )
            }
        }

        compose.onNodeWithTag("copy_report_btn").performClick()
        compose.runOnIdle { assertTrue(clipboard.getText()?.text?.isNotBlank() == true) }

        val shadow = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
        shadow.startedActivities.clear()
        compose.onNodeWithTag("share_report_btn").performClick()
        compose.runOnIdle {
            val chooser = shadow.nextStartedActivity
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertTrue(send?.getStringExtra(Intent.EXTRA_TEXT)?.isNotBlank() == true)
        }
    }
}

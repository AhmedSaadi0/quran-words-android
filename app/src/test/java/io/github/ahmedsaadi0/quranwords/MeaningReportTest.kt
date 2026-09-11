package io.github.ahmedsaadi0.quranwords

import io.github.ahmedsaadi0.quranwords.util.MeaningReportContent
import io.github.ahmedsaadi0.quranwords.util.MeaningReportLimits
import io.github.ahmedsaadi0.quranwords.util.MeaningReportType
import io.github.ahmedsaadi0.quranwords.util.ReportAyahSample
import io.github.ahmedsaadi0.quranwords.util.buildMeaningReportIssueUrl
import io.github.ahmedsaadi0.quranwords.util.buildMeaningReportMarkdown
import io.github.ahmedsaadi0.quranwords.util.validateMeaningReport
import io.github.ahmedsaadi0.quranwords.domain.model.DbInstalledVersion
import io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo
import io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState
import io.github.ahmedsaadi0.quranwords.domain.repository.DbCheckResult
import io.github.ahmedsaadi0.quranwords.domain.repository.DbUpdateRepository
import io.github.ahmedsaadi0.quranwords.core.util.BuildInfo
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.report.ReportMeaningViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MeaningReportTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleContent() = MeaningReportContent(
        rootText = "كتب",
        rootId = 42,
        reportType = MeaningReportType.INCORRECT,
        targetSource = "لسان العرب",
        targetQuote = "الكِتابُ معروف",
        description = "هذا شرح كافٍ للمشكلة المبلغ عنها هنا",
        suggestion = "أقترح هذا التصحيح",
        samples = listOf(
            ReportAyahSample("البقرة", 282, "نص الآية الأولى للشاهد"),
            ReportAyahSample("النساء", 103, "نص الآية الثانية للشاهد")
        ),
        appVersion = "0.2.1 (3)",
        dbVersionName = "v2",
        dbVersionCode = 2,
        androidRelease = "14",
        locale = "ar"
    )

    @Test
    fun `validation requires minimum length`() {
        assertFalse(validateMeaningReport(""))
        assertFalse(validateMeaningReport("   "))
        assertFalse(validateMeaningReport("قصير"))
        assertTrue(validateMeaningReport("0123456789"))
        assertTrue(validateMeaningReport("  وصف كافٍ يتجاوز الحد الأدنى المطلوب  "))
    }

    @Test
    fun `markdown contains all sections`() {
        val md = buildMeaningReportMarkdown(sampleContent())
        assertTrue(md.contains("كتب"))
        assertTrue(md.contains(MeaningReportType.INCORRECT.ar))
        assertTrue(md.contains("لسان العرب"))
        assertTrue(md.contains("الكِتابُ معروف"))
        assertTrue(md.contains("هذا شرح كافٍ"))
        assertTrue(md.contains("أقترح هذا التصحيح"))
        assertTrue(md.contains("[البقرة: 282]"))
        assertTrue(md.contains("root_id: 42"))
        assertTrue(md.contains("app: 0.2.1 (3)"))
        assertTrue(md.contains("db: v2 (code 2)"))
        assertTrue(md.contains("android: 14"))
    }

    @Test
    fun `optional sections omitted when blank`() {
        val md = buildMeaningReportMarkdown(
            sampleContent().copy(
                suggestion = "  ",
                samples = emptyList(),
                targetSource = "",
                targetQuote = "",
                rootId = null,
                appVersion = "",
                dbVersionName = "",
                dbVersionCode = 0
            )
        )
        assertFalse(md.contains("التصحيح المقترح"))
        assertFalse(md.contains("شواهد"))
        assertFalse(md.contains("المعنى المقصود"))
        assertFalse(md.contains("root_id"))
        assertFalse(md.contains("- app:"))
        assertFalse(md.contains("- db:"))
        assertTrue(md.contains("كتب"))
    }

    @Test
    fun `markdown special chars are escaped`() {
        val md = buildMeaningReportMarkdown(
            sampleContent().copy(targetQuote = "نص *مهم* [رابط](x) #وسم")
        )
        assertTrue(md.contains("\\*مهم\\*"))
        assertTrue(md.contains("\\[رابط\\]"))
        assertTrue(md.contains("\\#وسم"))
    }

    @Test
    fun `long fields are trimmed with ellipsis`() {
        val long = "أ".repeat(2000)
        val md = buildMeaningReportMarkdown(
            sampleContent().copy(
                description = long,
                suggestion = long,
                targetQuote = long,
                samples = listOf(ReportAyahSample("س", 1, long))
            )
        )
        assertTrue(md.contains("…"))
        assertTrue(md.length < 3000)
    }

    @Test
    fun `at most two samples included`() {
        val md = buildMeaningReportMarkdown(
            sampleContent().copy(
                samples = listOf(
                    ReportAyahSample("أ", 1, "واحد"),
                    ReportAyahSample("ب", 2, "اثنان"),
                    ReportAyahSample("ج", 3, "ثلاثة")
                )
            )
        )
        assertTrue(md.contains("واحد"))
        assertTrue(md.contains("اثنان"))
        assertFalse(md.contains("ثلاثة"))
    }

    @Test
    fun `issue url carries title body and label`() {
        val url = buildMeaningReportIssueUrl(sampleContent())
        assertTrue(url.startsWith("https://github.com/AhmedSaadi0/quran-words/issues/new?"))
        assertTrue(url.contains("title="))
        assertTrue(url.contains("body="))
        assertTrue(url.contains("labels="))
        // Encoded Arabic root must be present (percent-encoded, no raw spaces as +)
        assertTrue(url.contains("%D9%83%D8%AA%D8%A8"))
        assertFalse(url.contains("title=.*\\+.*".toRegex()))
    }

    private class FakeDbUpdateRepo : DbUpdateRepository {
        override suspend fun getInstalledVersion() = DbInstalledVersion(2, "v2")
        override suspend fun setInstalledVersion(code: Int, name: String) = Unit
        override suspend fun getDismissedVersionCode() = 0
        override suspend fun setDismissedVersionCode(code: Int) = Unit
        override suspend fun getLatestRelease(): DbCheckResult<DbReleaseInfo> =
            DbCheckResult.Error("unused")
        override suspend fun checkForUpdate(): DbCheckResult<DbUpdateState> =
            DbCheckResult.Success(DbUpdateState.UpToDate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewmodel gates submit and builds content`() = runTest {
        val vm = ReportMeaningViewModel(
            FakeDbUpdateRepo(),
            BuildInfo(appVersion = "0.2.1 (3)", androidRelease = "14", locale = "ar")
        )
        advanceUntilIdle()

        assertFalse(vm.canSubmit.value)
        vm.setDescription("قصير")
        assertFalse(vm.canSubmit.value)
        vm.setDescription("وصف كافٍ يتجاوز الحد الأدنى المطلوب للبلاغ")
        assertTrue(vm.canSubmit.value)

        vm.setReportType(MeaningReportType.TYPO)
        vm.setSuggestion("اقتراح مختصر")
        val content = vm.buildContent(
            rootText = "كتب",
            rootId = 42,
            aiSummary = "ملخص تجريبي للذكاء الاصطناعي",
            samples = listOf(ReportAyahSample("البقرة", 282, "نص"))
        )
        assertEquals(MeaningReportType.TYPO, content.reportType)
        assertEquals("الملخص الذكي", content.targetSource)
        assertEquals("ملخص تجريبي للذكاء الاصطناعي", content.targetQuote)
        assertEquals("v2", content.dbVersionName)
        assertEquals(2, content.dbVersionCode)
        assertEquals(1, content.samples.size)

        vm.reset()
        assertFalse(vm.canSubmit.value)
        assertEquals("", vm.description.value)
        assertEquals(MeaningReportType.INCORRECT, vm.reportType.value)
    }
}

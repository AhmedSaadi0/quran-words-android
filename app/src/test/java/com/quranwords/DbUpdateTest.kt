package com.quranwords

import com.quranwords.data.remote.DbFileValidator
import com.quranwords.data.remote.ManifestRemoteDataSource
import com.quranwords.domain.model.DbInstalledVersion
import com.quranwords.domain.model.DbReleaseInfo
import com.quranwords.domain.model.DbUpdateState
import com.quranwords.domain.repository.DbCheckResult
import com.quranwords.domain.repository.DbUpdateRepository
import com.quranwords.domain.usecase.CheckDbUpdateUseCase
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

private class FakeDbUpdateRepository(
    private val installed: DbInstalledVersion = DbInstalledVersion(1, "v1"),
    private val latest: DbReleaseInfo? = DbReleaseInfo(
        versionCode = 2,
        versionName = "v2",
        downloadUrl = "https://github.com/x/releases/download/db-v2/quran_words.db.zip",
        compressedSize = 28_000_000L,
        uncompressedSize = 118_534_144L,
        releaseNotesAr = "تحسينات"
    ),
    private val dismissed: Int = 0,
    private val fail: Boolean = false
) : DbUpdateRepository {
    override suspend fun getInstalledVersion(): DbInstalledVersion = installed
    override suspend fun setInstalledVersion(code: Int, name: String) = Unit
    override suspend fun getDismissedVersionCode(): Int = dismissed
    override suspend fun setDismissedVersionCode(code: Int) = Unit
    override suspend fun getLatestRelease(): DbCheckResult<DbReleaseInfo> {
        if (fail || latest == null) return DbCheckResult.Error("network")
        return DbCheckResult.Success(latest)
    }

    override suspend fun checkForUpdate(): DbCheckResult<DbUpdateState> {
        if (fail || latest == null) return DbCheckResult.Success(DbUpdateState.Unknown)
        if (installed.versionCode <= 0) return DbCheckResult.Success(DbUpdateState.NoLocalDb)
        if (latest.versionCode <= installed.versionCode) {
            return DbCheckResult.Success(DbUpdateState.UpToDate)
        }
        if (dismissed >= latest.versionCode) {
            return DbCheckResult.Success(DbUpdateState.UpToDate)
        }
        return DbCheckResult.Success(DbUpdateState.UpdateAvailable(latest))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DbUpdateTest {

    private fun manifestSource(): ManifestRemoteDataSource {
        return ManifestRemoteDataSource(
            OkHttpClient(),
            kotlinx.coroutines.Dispatchers.Unconfined
        )
    }

    @Test
    fun `parse valid manifest`() {
        val json = """
            {
              "latestVersionCode": 2,
              "latestVersionName": "v2",
              "downloadUrl": "https://github.com/AhmedSaadi0/quran-words/releases/download/db-v2/quran_words.db.zip",
              "compressedSize": 28000000,
              "uncompressedSize": 118534144,
              "sha256": "${"a".repeat(64)}",
              "publishedAt": "2026-09-01T00:00:00Z",
              "minAppVersionCode": 3,
              "releaseNotesAr": "ملاحظات",
              "releasePageUrl": "https://github.com/x/releases/tag/db-v2"
            }
        """.trimIndent()
        val info = manifestSource().parseManifest(json)
        assertEquals(2, info.versionCode)
        assertEquals("v2", info.versionName)
        assertTrue(info.downloadUrl.startsWith("https://"))
        assertEquals(28_000_000L, info.compressedSize)
        assertEquals(64, info.sha256!!.length)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse rejects non-https url`() {
        manifestSource().parseManifest(
            """{"latestVersionCode":2,"downloadUrl":"http://evil/x.zip"}"""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse rejects bad version code`() {
        manifestSource().parseManifest(
            """{"latestVersionCode":0,"downloadUrl":"https://x/y.zip"}"""
        )
    }

    @Test
    fun `parse accepts notes array`() {
        val info = manifestSource().parseManifest(
            """{"latestVersionCode":2,"downloadUrl":"https://x/y.zip","releaseNotesAr":["إضافة أجزاء الآية","إصلاح المعاني"]""" +
                "}"
        )
        assertEquals("إضافة أجزاء الآية\nإصلاح المعاني", info.releaseNotesAr)
    }

    @Test
    fun `parse keeps legacy single string notes`() {
        val info = manifestSource().parseManifest(
            """{"latestVersionCode":2,"downloadUrl":"https://x/y.zip","releaseNotesAr":"سطر أول\nسطر ثان"}"""
        )
        assertEquals("سطر أول\nسطر ثان", info.releaseNotesAr)
    }

    @Test
    fun `parse ignores blank array entries`() {
        val info = manifestSource().parseManifest(
            """{"latestVersionCode":2,"downloadUrl":"https://x/y.zip","releaseNotesAr":["  ","نص",""]}"""
        )
        assertEquals("نص", info.releaseNotesAr)
    }

    @Test
    fun `usecase surfaces update available`() = runTest {
        val useCase = CheckDbUpdateUseCase(FakeDbUpdateRepository())
        val result = useCase()
        assertTrue(result is DbCheckResult.Success)
        val state = (result as DbCheckResult.Success).data
        assertTrue(state is DbUpdateState.UpdateAvailable)
        assertEquals(2, (state as DbUpdateState.UpdateAvailable).info.versionCode)
    }

    @Test
    fun `dismissed version is treated as up to date`() = runTest {
        val useCase = CheckDbUpdateUseCase(FakeDbUpdateRepository(dismissed = 2))
        val state = (useCase() as DbCheckResult.Success).data
        assertEquals(DbUpdateState.UpToDate, state)
    }

    @Test
    fun `same version is up to date`() = runTest {
        val repo = FakeDbUpdateRepository(
            installed = DbInstalledVersion(2, "v2")
        )
        val state = (CheckDbUpdateUseCase(repo)() as DbCheckResult.Success).data
        assertEquals(DbUpdateState.UpToDate, state)
    }

    @Test
    fun `no local db state`() = runTest {
        val repo = FakeDbUpdateRepository(installed = DbInstalledVersion(0, ""))
        val state = (CheckDbUpdateUseCase(repo)() as DbCheckResult.Success).data
        assertEquals(DbUpdateState.NoLocalDb, state)
    }

    @Test
    fun `network failure maps to unknown not crash`() = runTest {
        val repo = FakeDbUpdateRepository(fail = true)
        val state = (CheckDbUpdateUseCase(repo)() as DbCheckResult.Success).data
        assertEquals(DbUpdateState.Unknown, state)
    }

    @Test
    fun `zip magic detection`() {
        val zip = File.createTempFile("db", ".zip")
        val db = File.createTempFile("db", ".db")
        try {
            zip.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00))
            db.writeBytes("SQLite format 3\u0000rest".toByteArray(Charsets.UTF_8))
            assertTrue(DbFileValidator.isZip(zip))
            assertFalse(DbFileValidator.isZip(db))
            assertTrue(DbFileValidator.hasSqliteHeader(db))
            assertFalse(DbFileValidator.hasSqliteHeader(zip))
        } finally {
            zip.delete()
            db.delete()
        }
    }

    @Test
    fun `db size validation with tolerance`() {
        assertTrue(DbFileValidator.isValidDbSize(118_534_144L, 118_534_144L))
        assertTrue(DbFileValidator.isValidDbSize(110_000_000L, 118_534_144L))
        assertFalse(DbFileValidator.isValidDbSize(10_000L, 118_534_144L))
        assertFalse(DbFileValidator.isValidDbSize(0L, 0L))
    }
}

package io.github.ahmedsaadi0.quranwords.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import io.github.ahmedsaadi0.quranwords.core.util.DatabaseConstants
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.data.remote.DatabaseDownloadManager
import io.github.ahmedsaadi0.quranwords.data.repository.QuranRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P1 data-layer tests for Mushaf page reads. Builds a minimal fixture database
 * (two pages, cross-surah page, scrambled word positions, one word without a
 * morphology row) at the Robolectric database path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MushafPageDataTest {

  private lateinit var context: Context
  private lateinit var repo: QuranRepositoryImpl

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    repo = QuranRepositoryImpl(
      context,
      DatabaseDownloadManager(context, OkHttpClient()),
      Dispatchers.Unconfined
    )
    repo.minDbFileSizeBytes = 0
  }

  private fun writeFixtureDb() {
    val file = context.getDatabasePath(DatabaseConstants.DB_NAME)
    file.parentFile?.mkdirs()
    if (file.exists()) file.delete()
    val db = SQLiteDatabase.openOrCreateDatabase(file, null)
    db.execSQL("CREATE TABLE ayat (id INTEGER PRIMARY KEY, surah INTEGER, ayah INTEGER, text_uthmani TEXT, text_uthmani_plain TEXT, text_imlaei TEXT, word_count INTEGER, juz INTEGER, hizb INTEGER, rub_el_hizb INTEGER, page_number INTEGER)")
    db.execSQL("CREATE TABLE words (id INTEGER PRIMARY KEY, text TEXT, text_clean TEXT, translation TEXT)")
    db.execSQL("CREATE TABLE word_ayah (id INTEGER PRIMARY KEY, word_id INTEGER, ayah_id INTEGER, position INTEGER)")
    db.execSQL("CREATE TABLE word_morphology (word_ayah_id INTEGER, root_id INTEGER, pos TEXT, form TEXT, aspect TEXT, mood TEXT, voice TEXT, person TEXT, gender TEXT, number TEXT, grammatical_case TEXT, state TEXT, derivation TEXT, special TEXT, segments TEXT)")
    db.execSQL("CREATE TABLE roots (id INTEGER PRIMARY KEY, root TEXT)")
    // Page 7: two ayat of surah 2 (words inserted in scrambled position order)
    db.execSQL("INSERT INTO ayat VALUES (10, 2, 5, 't5', 'p5', 'i5', 2, 1, 1, 1, 7)")
    db.execSQL("INSERT INTO ayat VALUES (11, 2, 6, 't6', 'p6', 'i6', 1, 1, 1, 1, 7)")
    // Page 8: first ayah of surah 3 (cross-surah page boundary case)
    db.execSQL("INSERT INTO ayat VALUES (12, 3, 1, 't1', 'p1', 'i1', 1, 2, 2, 2, 8)")
    db.execSQL("INSERT INTO words VALUES (1, 'w-pos2', 'c2', 'tr2')")
    db.execSQL("INSERT INTO words VALUES (2, 'w-pos1', 'c1', 'tr1')")
    db.execSQL("INSERT INTO words VALUES (3, 'w-single', 'c3', 'tr3')")
    db.execSQL("INSERT INTO words VALUES (4, 'w-s3', 'c4', 'tr4')")
    db.execSQL("INSERT INTO word_ayah VALUES (100, 1, 10, 2)")
    db.execSQL("INSERT INTO word_ayah VALUES (101, 2, 10, 1)")
    db.execSQL("INSERT INTO word_ayah VALUES (102, 3, 11, 1)")
    db.execSQL("INSERT INTO word_ayah VALUES (103, 4, 12, 1)")
    db.execSQL("INSERT INTO roots VALUES (9, 'كتب')")
    db.execSQL("INSERT INTO word_morphology VALUES (101, 9, 'N', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)")
    db.close()
  }

  @Test
  fun `getAyatByPage returns ayat ordered with words attached`() = runTest {
    writeFixtureDb()

    val ayat = repo.getAyatByPage(7)

    assertEquals(2, ayat.size)
    assertEquals(5, ayat[0].ayah)
    assertEquals(6, ayat[1].ayah)
    assertEquals(7, ayat[0].pageNumber)
    assertEquals(1, ayat[0].juz)
    // Words ordered by position despite scrambled inserts
    assertEquals(listOf("w-pos1", "w-pos2"), ayat[0].words.map { it.text })
    assertEquals(listOf(1, 2), ayat[0].words.map { it.position })
    // Morphology attached; word without morphology row has null root
    assertEquals(9, ayat[0].words[0].rootId)
    assertEquals("كتب", ayat[0].words[0].rootText)
    assertNull(ayat[0].words[1].rootId)
    assertNull(ayat[1].words[0].rootId)
  }

  @Test
  fun `getAyatByPage isolates pages across surah boundary`() = runTest {
    writeFixtureDb()

    val ayat = repo.getAyatByPage(8)

    assertEquals(1, ayat.size)
    assertEquals(3, ayat[0].surah)
    assertEquals(1, ayat[0].ayah)
    assertEquals(2, ayat[0].juz)
    assertEquals(listOf("w-s3"), ayat[0].words.map { it.text })
  }

  @Test
  fun `getAyatByPage returns empty for unknown and invalid pages`() = runTest {
    writeFixtureDb()

    assertTrue(repo.getAyatByPage(999).isEmpty())
    assertTrue(repo.getAyatByPage(0).isEmpty())
    assertTrue(repo.getAyatByPage(-1).isEmpty())
  }

  @Test
  fun `getMushafPageCount returns max page`() = runTest {
    writeFixtureDb()

    assertEquals(8, repo.getMushafPageCount())
  }

  @Test
  fun `getMushafPageCount falls back when database missing`() = runTest {
    val file = context.getDatabasePath(DatabaseConstants.DB_NAME)
    if (file.exists()) file.delete()

    assertEquals(MushafConstants.TOTAL_PAGES_FALLBACK, repo.getMushafPageCount())
    assertTrue(repo.getAyatByPage(7).isEmpty())
  }
}

package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.AppLanguage
import io.github.ahmedsaadi0.quranwords.core.util.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LanguageManagerTest {

  private val manager = LanguageManager()

  @Test
  fun `resolveTags maps known tags and nulls everything else`() {
    assertEquals("ar", manager.resolveTags(AppLanguage.ARABIC))
    assertEquals("en", manager.resolveTags(AppLanguage.ENGLISH))
    assertNull(manager.resolveTags(AppLanguage.SYSTEM))
    assertNull(manager.resolveTags("fr"))
    assertNull(manager.resolveTags(""))
  }

  @Test
  fun `system tag clears the per-app override`() {
    assertTrue(manager.toLocaleList(AppLanguage.SYSTEM).isEmpty)
    assertTrue(manager.toLocaleList("unknown").isEmpty)
  }

  @Test
  fun `ar and en tags pin the locale`() {
    assertEquals("ar", manager.toLocaleList(AppLanguage.ARABIC).toLanguageTags())
    assertEquals("en", manager.toLocaleList(AppLanguage.ENGLISH).toLanguageTags())
  }
}

package io.github.ahmedsaadi0.quranwords.core

import io.github.ahmedsaadi0.quranwords.core.util.isMeccan
import io.github.ahmedsaadi0.quranwords.core.util.SurahMeta
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevelationTypeTest {

  @Test
  fun `surah meccan detection uses exact match`() {
    assertTrue(Surah(1, "الفاتحة", "Al-Fatihah", 7, "مكية", 1).isMeccan)
    assertFalse(Surah(2, "البقرة", "Al-Baqarah", 286, "مدنية", 1).isMeccan)
    assertFalse(Surah(2, "البقرة", "Al-Baqarah", 286, "Meccan", 1).isMeccan)
  }

  @Test
  fun `surah meta meccan detection uses exact match`() {
    assertTrue(SurahMeta(1, "الفاتحة", "Al-Fatihah", 7, "مكية", 1).isMeccan)
    assertFalse(SurahMeta(2, "البقرة", "Al-Baqarah", 286, "مدنية", 1).isMeccan)
  }
}

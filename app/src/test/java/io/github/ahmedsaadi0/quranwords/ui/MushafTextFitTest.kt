package io.github.ahmedsaadi0.quranwords.ui

import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.FIT_MAX_SP
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.FIT_MIN_SP
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.binarySearchFontSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafTextFitTest {

  @Test
  fun `sparse page ceiling stays at 28sp`() {
    assertEquals(28f, FIT_MAX_SP, 0.001f)
  }

  @Test
  fun `hard floor stays at 12sp with scroll fallback below`() {
    assertEquals(12f, FIT_MIN_SP, 0.001f)
  }

  @Test
  fun `binary search converges to largest fitting size`() {
    val fitted = binarySearchFontSize(18f, 34f, 6, 200) { size ->
      if (size <= 24f) 100 else 500
    }

    assertTrue(fitted in 23.5f..24.5f)
  }

  @Test
  fun `budget below minimum capacity returns minimum`() {
    val fitted = binarySearchFontSize(18f, 34f, 6, 100) { 1000 }

    assertEquals(18f, fitted, 0.001f)
  }

  @Test
  fun `generous budget returns maximum`() {
    val fitted = binarySearchFontSize(18f, 34f, 6, 10_000) { 0 }

    assertEquals(34f, fitted, 0.5f)
  }

  @Test
  fun `linear measure resolves proportionally`() {
    val fitted = binarySearchFontSize(18f, 34f, 8, 255) { size -> (size * 10).toInt() }

    assertTrue(fitted in 25f..26f)
  }
}

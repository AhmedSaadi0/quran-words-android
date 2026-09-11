package io.github.ahmedsaadi0.quranwords.ui

import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.ayahCountLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class AyahCountLabelTest {

  @Test
  fun `large counts use singular ayah`() {
    assertEquals("٢٨٦ آية", ayahCountLabel(286))
    assertEquals("١١ آية", ayahCountLabel(11))
  }

  @Test
  fun `counts three to ten use plural ayat`() {
    assertEquals("٧ آيات", ayahCountLabel(7))
    assertEquals("٣ آيات", ayahCountLabel(3))
    assertEquals("١٠ آيات", ayahCountLabel(10))
  }
}

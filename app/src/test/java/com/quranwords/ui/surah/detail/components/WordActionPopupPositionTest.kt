package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure placement math for the long-press word toolbar (no Compose fixtures). */
class WordActionPopupPositionTest {

    private val window = IntSize(1000, 800)
    private val popup = IntSize(160, 48)

    private fun position(anchor: IntRect, word: Rect, gap: Int = 8) =
        wordPopupPosition(anchor, word, window, popup, gap)

    @Test
    fun `prefers centered above the word`() {
        // Anchor window origin (100,500); word rect is anchor-local.
        val anchor = IntRect(100, 500, 600, 800)
        val result = position(anchor, Rect(100f, 100f, 200f, 140f))
        // Word absolute: x 200..300, top 600 -> y = 600 - 48 - 8 = 544.
        assertEquals(IntOffset(170, 544), result)
    }

    @Test
    fun `flips below when too close to the top edge`() {
        val anchor = IntRect(0, 0, 1000, 800)
        val result = position(anchor, Rect(400f, 10f, 500f, 30f))
        // Above would be negative: use word bottom (30) + gap = 38.
        assertEquals(IntOffset(370, 38), result)
    }

    @Test
    fun `clamps horizontally inside the window in both directions`() {
        val anchor = IntRect(0, 100, 1000, 700)
        val left = position(anchor, Rect(2f, 200f, 40f, 230f))
        assertEquals(0, left.x)
        val right = position(anchor, Rect(960f, 200f, 998f, 230f))
        assertEquals(window.width - popup.width, right.x)
    }

    @Test
    fun `clamps vertically after flipping when word is tall`() {
        val anchor = IntRect(0, 0, 1000, 200)
        val result = position(anchor, Rect(400f, 0f, 500f, 180f))
        assertEquals(window.height - popup.height, result.y)
    }

    @Test
    fun `zero sized popup is a safe no-op`() {
        assertEquals(
            IntOffset.Zero,
            wordPopupPosition(IntRect(0, 0, 1000, 800), Rect(100f, 100f, 200f, 140f), window, IntSize.Zero, 8)
        )
    }

    @Test
    fun `scrolled anchor shifts the word position`() {
        val word = Rect(100f, 100f, 200f, 140f)
        val before = position(IntRect(0, 500, 1000, 800), word)
        val after = position(IntRect(0, 300, 1000, 600), word)
        assertEquals(-200, after.y - before.y)
    }
}

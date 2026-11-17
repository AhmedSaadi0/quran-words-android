package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val SLOP = 24f

class MushafWordHitTestTest {

    @Test
    fun emptyRectsReturnNull() {
        assertNull(closestWordIndex(0f, emptyList(), SLOP))
    }

    @Test
    fun directHitInsideRectWins() {
        val rects = listOf(Rect(0f, 0f, 100f, 10f), Rect(200f, 0f, 300f, 10f))
        assertEquals(0, closestWordIndex(50f, rects, SLOP))
        assertEquals(1, closestWordIndex(250f, rects, SLOP))
    }

    @Test
    fun containmentWinsEvenWhenAnotherRectIsEquallyNear() {
        // 95 is inside both; the earlier rect in input order wins containment.
        val rects = listOf(Rect(0f, 0f, 100f, 10f), Rect(90f, 0f, 200f, 10f))
        assertEquals(0, closestWordIndex(95f, rects, 0f))
    }

    @Test
    fun gapTapPicksNearestWordLtrOrder() {
        val rects = listOf(Rect(100f, 0f, 150f, 10f), Rect(200f, 0f, 250f, 10f))
        // 170: 20px from first, 30px from second -> first.
        assertEquals(0, closestWordIndex(170f, rects, 0f))
        // 185: 35px from first, 15px from second -> second.
        assertEquals(1, closestWordIndex(185f, rects, 0f))
    }

    @Test
    fun gapTapPicksNearestWordRtlOrder() {
        // RTL input order: the word logically first (rightmost visually).
        val rects = listOf(Rect(200f, 0f, 250f, 10f), Rect(100f, 0f, 150f, 10f))
        // 185: 15px from first rect, 35px from second -> index 0.
        assertEquals(0, closestWordIndex(185f, rects, 0f))
        // 170: 30px from first rect, 20px from second -> index 1.
        assertEquals(1, closestWordIndex(170f, rects, 0f))
    }

    @Test
    fun exactMidGapTieReturnsEarlierRectDeterministically() {
        // Both rects exactly 50px away; the earlier rect in the list wins.
        val ltr = listOf(Rect(0f, 0f, 100f, 10f), Rect(200f, 0f, 300f, 10f))
        assertEquals(0, closestWordIndex(150f, ltr, 0f))
        val reversed = listOf(Rect(200f, 0f, 300f, 10f), Rect(0f, 0f, 100f, 10f))
        assertEquals(0, closestWordIndex(150f, reversed, 0f))
    }

    @Test
    fun marginWithinSlopSnapsToEdgeWord() {
        val rects = listOf(Rect(100f, 0f, 150f, 10f), Rect(400f, 0f, 450f, 10f))
        // Left boundary (inclusive): exactly slop pixels outside the first rect.
        assertEquals(0, closestWordIndex(76f, rects, SLOP))
        // Right boundary (inclusive): exactly slop pixels outside the last rect.
        assertEquals(1, closestWordIndex(474f, rects, SLOP))
    }

    @Test
    fun marginBeyondSlopReturnsNull() {
        val rects = listOf(Rect(100f, 0f, 150f, 10f), Rect(400f, 0f, 450f, 10f))
        assertNull(closestWordIndex(75.9f, rects, SLOP))
        assertNull(closestWordIndex(474.1f, rects, SLOP))
    }

    @Test
    fun wideGapGapBetweenDistantWordsUsesLineBoundsNotSlop() {
        // Far-apart edge words: interior points still resolve to a word.
        val rects = listOf(Rect(0f, 0f, 10f, 10f), Rect(500f, 0f, 510f, 10f))
        // 500f? No: 500f is inside rect 2; use a mid point inside the wide gap.
        assertEquals(0, closestWordIndex(200f, rects, SLOP))
        assertEquals(1, closestWordIndex(400f, rects, SLOP))
    }

    @Test
    fun markerContainmentWinsOverOverlappingWord() {
        val words = listOf(Rect(0f, 0f, 100f, 10f))
        val markers = listOf(Rect(90f, 0f, 110f, 10f))
        assertEquals(MushafHitKind.Marker(0), resolveHitKind(100f, words, markers, 0f))
    }

    @Test
    fun wordHitWhenOutsideTheMarker() {
        val words = listOf(Rect(0f, 0f, 100f, 10f))
        val markers = listOf(Rect(120f, 0f, 140f, 10f))
        assertEquals(MushafHitKind.Word(0), resolveHitKind(30f, words, markers, 0f))
    }

    @Test
    fun gluingNbspFallsBackToNearestWord() {
        // Between the word's ink end (100) and the marker start (105).
        val words = listOf(Rect(0f, 0f, 100f, 10f))
        val markers = listOf(Rect(105f, 0f, 125f, 10f))
        assertEquals(MushafHitKind.Word(0), resolveHitKind(103f, words, markers, SLOP))
    }

    @Test
    fun trueWhitespaceAndMarginsMiss() {
        val words = listOf(Rect(0f, 0f, 100f, 10f))
        val markers = listOf(Rect(120f, 0f, 140f, 10f))
        assertEquals(MushafHitKind.Miss, resolveHitKind(300f, words, markers, SLOP))
    }
}

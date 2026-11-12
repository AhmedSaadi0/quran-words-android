package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the reading screen's scroll connection — in particular the phase order
 * that makes cancelling a live pull unconditionally first: the pull unwinds
 * before anything else may move.
 *
 * Rubber-band factor is 0.4f (mirrors `PullResistance`); px values below are
 * chosen so the clamps never hide the arithmetic under test.
 */
class SurahReadingScrollTest {

    private fun connection(
        atBottomEdge: Boolean = false,
        atTopEdge: Boolean = false,
        canDown: Boolean = true,
        canUp: Boolean = true,
        horizontal: Boolean = false,
        rtl: Boolean = true,
        onSwitch: (Boolean) -> Unit = {}
    ): SurahReadingScrollConnection = SurahReadingScrollConnection(
        canScrollForward = { !atBottomEdge },
        canScrollBackward = { !atTopEdge },
        canSwitchDown = { canDown },
        canSwitchUp = { canUp },
        onSwitch = onSwitch,
        thresholdPx = 100f,
        maxPullPx = 60f,
        hintMinPx = 10f,
        horizontal = { horizontal },
        rtl = { rtl }
    )

    private fun pullUp(delta: Float, connection: SurahReadingScrollConnection) =
        connection.onPostScroll(Offset.Zero, Offset(0f, delta), NestedScrollSource.UserInput)

    private fun drag(delta: Float, connection: SurahReadingScrollConnection) =
        connection.onPreScroll(Offset(0f, delta), NestedScrollSource.UserInput)

    private fun pullX(delta: Float, connection: SurahReadingScrollConnection) =
        connection.onPostScroll(Offset.Zero, Offset(delta, 0f), NestedScrollSource.UserInput)

    private fun dragX(delta: Float, connection: SurahReadingScrollConnection) =
        connection.onPreScroll(Offset(delta, 0f), NestedScrollSource.UserInput)

    @Test
    fun `pull accumulates at the bottom edge and consumes nothing`() {
        val connection = connection(atBottomEdge = true)
        val consumed = pullUp(-40f, connection)

        assertEquals(Offset.Zero, consumed)
        assertEquals(-16f, connection.pullPx, 0.01f) // 40 * 0.4
        assertEquals(-1, connection.hintDirection)
    }

    @Test
    fun `scrolling away from an edge never pulls or hints`() {
        val connection = connection()
        pullUp(-40f, connection)

        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
    }

    @Test
    fun `reverse drag unwinds the pull and consumes only the pull travel`() {
        val connection = connection(atBottomEdge = true)
        pullUp(-150f, connection)

        val consumed = drag(60f, connection)

        assertEquals(60f, consumed.y, 0.01f)
        assertEquals(-36f, connection.pullPx, 0.01f) // 90 left * 0.4
        assertEquals(-1, connection.hintDirection) // still pulled past hintMin
    }

    @Test
    fun `unwinding past zero hands the remainder back to the list`() {
        val connection = connection(atBottomEdge = true)
        pullUp(-40f, connection)

        val consumed = drag(100f, connection)

        assertEquals(40f, consumed.y, 0.01f) // only what the pull needed
        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
        // The very next frame is plain scrolling again.
        assertEquals(Offset.Zero, drag(100f, connection))
    }

    @Test
    fun `same direction drag extends the pull without consuming`() {
        val connection = connection(atBottomEdge = true)
        pullUp(-40f, connection)

        assertEquals(0f, drag(-30f, connection).y, 0f)
        assertEquals(-28f, connection.pullPx, 0.01f) // 70 * 0.4
    }

    @Test
    fun `previous pull cancels symmetrically at the top edge`() {
        val connection = connection(atTopEdge = true)
        pullUp(60f, connection) // dragging down pulls the previous surah
        assertEquals(1, connection.hintDirection)

        val consumed = drag(-25f, connection)

        assertEquals(-25f, consumed.y, 0.01f)
        assertEquals(14f, connection.pullPx, 0.01f) // 35 * 0.4
        assertEquals(1, connection.hintDirection)
    }

    @Test
    fun `gated direction never pulls`() {
        val connection = connection(atBottomEdge = true, canDown = false)

        pullUp(-40f, connection)

        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
        assertEquals(Offset.Zero, drag(20f, connection))
    }

    @Test
    fun `fling leftovers never arm or move a pull`() {
        val connection = connection(atBottomEdge = true)

        connection.onPostScroll(Offset.Zero, Offset(0f, -40f), NestedScrollSource.Fling)

        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, 20f), NestedScrollSource.Fling))
    }

    @Test
    fun `release past the threshold switches in the pull direction`() = runTest {
        val fired = mutableListOf<Boolean>()
        val connection = connection(atBottomEdge = true, onSwitch = { fired += it })
        pullUp(-120f, connection)

        connection.onPreFling(Velocity.Zero)

        assertEquals(listOf(true), fired)
    }

    @Test
    fun `release backwards switches to the previous surah`() = runTest {
        val fired = mutableListOf<Boolean>()
        val connection = connection(atTopEdge = true, onSwitch = { fired += it })
        pullUp(120f, connection)

        connection.onPreFling(Velocity.Zero)

        assertEquals(listOf(false), fired)
    }

    @Test
    fun `cancelling the pull before release prevents the switch`() = runTest {
        val fired = mutableListOf<Boolean>()
        val connection = connection(atBottomEdge = true, onSwitch = { fired += it })
        pullUp(-150f, connection)
        drag(200f, connection) // reader changes their mind and drags back

        connection.onPreFling(Velocity.Zero)

        assertTrue(fired.isEmpty())
        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
    }

    @Test
    fun `extreme pull clamps at the visual limit`() {
        val connection = connection(atBottomEdge = true)

        pullUp(-1000f, connection)

        // accumulated clamps at the raw cap (240px) and the visual at 60px.
        assertEquals(-60f, connection.pullPx, 0.01f)
        assertEquals(-1, connection.hintDirection)
    }

    @Test
    fun `extreme pull toward the previous surah clamps symmetrically`() {
        val connection = connection(atTopEdge = true)

        pullUp(1000f, connection)

        assertEquals(60f, connection.pullPx, 0.01f)
        assertEquals(1, connection.hintDirection)
    }

    @Test
    fun `hint badge clears when the pull decays below its threshold`() {
        val connection = connection(atBottomEdge = true)
        pullUp(-30f, connection) // -12px visual → hinted
        assertEquals(-1, connection.hintDirection)

        drag(15f, connection) // unwind halfway: 6px visual < hintMin
        assertEquals(-6f, connection.pullPx, 0.01f)
        assertEquals(0, connection.hintDirection)
    }

    @Test
    fun `horizontal RTL pull right at the bottom edge pulls the next surah`() {
        val connection = connection(atBottomEdge = true, horizontal = true, rtl = true)

        pullX(40f, connection)

        // Normalized: +x right in RTL = next → pullPx negative.
        assertEquals(-16f, connection.pullPx, 0.01f) // 40 * 0.4
        assertEquals(-1, connection.hintDirection)
    }

    @Test
    fun `horizontal LTR pull left at the bottom edge pulls the next surah`() {
        val connection = connection(atBottomEdge = true, horizontal = true, rtl = false)

        pullX(-40f, connection)

        assertEquals(-16f, connection.pullPx, 0.01f) // same normalized value
        assertEquals(-1, connection.hintDirection)
    }

    @Test
    fun `horizontal RTL pull left at the top edge pulls the previous surah`() {
        val connection = connection(atTopEdge = true, horizontal = true, rtl = true)

        pullX(-60f, connection)

        assertEquals(24f, connection.pullPx, 0.01f) // 60 * 0.4
        assertEquals(1, connection.hintDirection)
    }

    @Test
    fun `horizontal reverse drag unwinds on the x axis`() {
        val connection = connection(atBottomEdge = true, horizontal = true, rtl = true)
        // RTL: a rightward drag (+x) pulls the NEXT surah.
        pullX(150f, connection)

        // The reader changes their mind and drags left; the connection
        // consumes exactly that reverse travel, reported on the REAL axis.
        val consumed = dragX(-60f, connection)

        assertEquals(-60f, consumed.x, 0.01f)
        assertEquals(0f, consumed.y, 0f)
        assertEquals(-36f, connection.pullPx, 0.01f) // 90 left * 0.4
    }

    @Test
    fun `horizontal release past the threshold switches forward`() = runTest {
        val fired = mutableListOf<Boolean>()
        val connection = connection(
            atBottomEdge = true,
            horizontal = true,
            rtl = true,
            onSwitch = { fired += it }
        )
        pullX(120f, connection) // RTL: right = next

        connection.onPreFling(Velocity.Zero)

        assertEquals(listOf(true), fired)
    }

    @Test
    fun `horizontal pull never arms while scrolling mid-list`() {
        val connection = connection(horizontal = true, rtl = true)

        pullX(200f, connection)

        // Same edge gates as the vertical pull: nothing at a non-edge.
        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
    }

    // Note: the spring-back branch of onPreFling is deliberately not covered
    // here — it drives an `animate` call, which needs a MonotonicFrameClock that
    // plain runTest does not install. That path is verified manually instead.
}

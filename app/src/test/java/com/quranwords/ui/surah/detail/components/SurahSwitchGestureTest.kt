package com.quranwords.ui.surah.detail.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the switch gesture's pull lifecycle — in particular the cancellation
 * path: once a pull is live the child list can scroll back, so a reverse drag
 * has to be intercepted in `onPreScroll`, otherwise `onPostScroll` sees no
 * delta, the stale pull survives, and releasing still switches surah.
 *
 * Rubber-band factor is 0.4f (mirrors `PullResistance`); px values below are
 * chosen so the clamps never hide the arithmetic under test.
 */
class SurahSwitchGestureTest {

    private fun gesture(
        atBottomEdge: Boolean = false,
        atTopEdge: Boolean = false,
        canDown: Boolean = true,
        canUp: Boolean = true,
        onSwitch: (Boolean) -> Unit = {}
    ) = SurahSwitchGesture(
        canScrollForward = { !atBottomEdge },
        canScrollBackward = { !atTopEdge },
        canSwitchDown = { canDown },
        canSwitchUp = { canUp },
        onSwitch = onSwitch,
        thresholdPx = 100f,
        maxPullPx = 60f,
        hintMinPx = 10f
    )

    private fun pullUp(delta: Float, gesture: SurahSwitchGesture) =
        gesture.onPostScroll(Offset.Zero, Offset(0f, delta), NestedScrollSource.UserInput)

    private fun drag(delta: Float, gesture: SurahSwitchGesture) =
        gesture.onPreScroll(Offset(0f, delta), NestedScrollSource.UserInput)

    @Test
    fun `pull accumulates at the bottom edge and consumes nothing`() {
        val gesture = gesture(atBottomEdge = true)
        val consumed = pullUp(-40f, gesture)

        assertEquals(Offset.Zero, consumed)
        assertEquals(-16f, gesture.pullPx, 0.01f) // 40 * 0.4
        assertEquals(-1, gesture.hintDirection)
    }

    @Test
    fun `scrolling away from an edge never pulls or hints`() {
        val gesture = gesture()
        pullUp(-40f, gesture)

        assertEquals(0f, gesture.pullPx, 0f)
        assertEquals(0, gesture.hintDirection)
    }

    @Test
    fun `reverse drag unwinds the pull and consumes only the pull travel`() {
        val gesture = gesture(atBottomEdge = true)
        pullUp(-150f, gesture)

        val consumed = drag(60f, gesture)

        assertEquals(60f, consumed.y, 0.01f)
        assertEquals(-36f, gesture.pullPx, 0.01f) // 90 left * 0.4
        assertEquals(-1, gesture.hintDirection) // still pulled past hintMin
    }

    @Test
    fun `unwinding past zero hands the remainder back to the list`() {
        val gesture = gesture(atBottomEdge = true)
        pullUp(-40f, gesture)

        val consumed = drag(100f, gesture)

        assertEquals(40f, consumed.y, 0.01f) // only what the pull needed
        assertEquals(0f, gesture.pullPx, 0f)
        assertEquals(0, gesture.hintDirection)
        // The very next frame is plain scrolling again.
        assertEquals(Offset.Zero, drag(100f, gesture))
    }

    @Test
    fun `same direction drag extends the pull without consuming`() {
        val gesture = gesture(atBottomEdge = true)
        pullUp(-40f, gesture)

        assertEquals(0f, drag(-30f, gesture).y, 0f)
        assertEquals(-28f, gesture.pullPx, 0.01f) // 70 * 0.4
    }

    @Test
    fun `previous pull cancels symmetrically at the top edge`() {
        val gesture = gesture(atTopEdge = true)
        pullUp(60f, gesture) // dragging down pulls the previous surah
        assertEquals(1, gesture.hintDirection)

        val consumed = drag(-25f, gesture)

        assertEquals(-25f, consumed.y, 0.01f)
        assertEquals(14f, gesture.pullPx, 0.01f) // 35 * 0.4
        assertEquals(1, gesture.hintDirection)
    }

    @Test
    fun `gated direction never pulls`() {
        val gesture = gesture(atBottomEdge = true, canDown = false)

        pullUp(-40f, gesture)

        assertEquals(0f, gesture.pullPx, 0f)
        assertEquals(0, gesture.hintDirection)
        assertEquals(Offset.Zero, drag(20f, gesture))
    }

    @Test
    fun `fling leftovers never arm or move a pull`() {
        val gesture = gesture(atBottomEdge = true)

        gesture.onPostScroll(Offset.Zero, Offset(0f, -40f), NestedScrollSource.Fling)

        assertEquals(0f, gesture.pullPx, 0f)
        assertEquals(Offset.Zero, gesture.onPreScroll(Offset(0f, 20f), NestedScrollSource.Fling))
    }

    @Test
    fun `release past the threshold switches in the pull direction`() = runTest {
        val fired = mutableListOf<Boolean>()
        val gesture = gesture(atBottomEdge = true, onSwitch = { fired += it })
        pullUp(-120f, gesture)

        gesture.onPreFling(Velocity.Zero)

        assertEquals(listOf(true), fired)
    }

    @Test
    fun `release backwards switches to the previous surah`() = runTest {
        val fired = mutableListOf<Boolean>()
        val gesture = gesture(atTopEdge = true, onSwitch = { fired += it })
        pullUp(120f, gesture)

        gesture.onPreFling(Velocity.Zero)

        assertEquals(listOf(false), fired)
    }

    @Test
    fun `cancelling the pull before release prevents the switch`() = runTest {
        val fired = mutableListOf<Boolean>()
        val gesture = gesture(atBottomEdge = true, onSwitch = { fired += it })
        pullUp(-150f, gesture)
        drag(200f, gesture) // reader changes their mind and drags back

        gesture.onPreFling(Velocity.Zero)

        assertTrue(fired.isEmpty())
        assertEquals(0f, gesture.pullPx, 0f)
        assertEquals(0, gesture.hintDirection)
    }

    // Note: the spring-back branch of onPreFling is deliberately not covered
    // here — it drives an `animate` call, which needs a MonotonicFrameClock that
    // plain runTest does not install. That path is verified manually instead.
}

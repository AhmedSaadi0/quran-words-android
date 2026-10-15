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
 * that a split connection pair could never guarantee: a live pull unwinds
 * before the header or the list may move.
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
        headerOffsetPx: Float = 0f,
        collapsibleHeightPx: Int = 0,
        selectionActive: Boolean = false,
        onSwitch: (Boolean) -> Unit = {}
    ): Pair<SurahReadingScrollConnection, SurahCollapsingHeaderState> {
        val header = SurahCollapsingHeaderState(collapsibleHeightPx, headerOffsetPx)
        val connection = SurahReadingScrollConnection(
            canScrollForward = { !atBottomEdge },
            canScrollBackward = { !atTopEdge },
            collapseState = header,
            isSelectionActive = { selectionActive },
            canSwitchDown = { canDown },
            canSwitchUp = { canUp },
            onSwitch = onSwitch,
            thresholdPx = 100f,
            maxPullPx = 60f,
            hintMinPx = 10f
        )
        return connection to header
    }

    private fun pullUp(delta: Float, connection: SurahReadingScrollConnection) =
        connection.onPostScroll(Offset.Zero, Offset(0f, delta), NestedScrollSource.UserInput)

    private fun drag(delta: Float, connection: SurahReadingScrollConnection) =
        connection.onPreScroll(Offset(0f, delta), NestedScrollSource.UserInput)

    @Test
    fun `pull accumulates at the bottom edge and consumes nothing`() {
        val (connection, _) = connection(atBottomEdge = true)
        val consumed = pullUp(-40f, connection)

        assertEquals(Offset.Zero, consumed)
        assertEquals(-16f, connection.pullPx, 0.01f) // 40 * 0.4
        assertEquals(-1, connection.hintDirection)
    }

    @Test
    fun `scrolling away from an edge never pulls or hints`() {
        val (connection, _) = connection()
        pullUp(-40f, connection)

        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
    }

    @Test
    fun `reverse drag unwinds the pull and consumes only the pull travel`() {
        val (connection, _) = connection(atBottomEdge = true)
        pullUp(-150f, connection)

        val consumed = drag(60f, connection)

        assertEquals(60f, consumed.y, 0.01f)
        assertEquals(-36f, connection.pullPx, 0.01f) // 90 left * 0.4
        assertEquals(-1, connection.hintDirection) // still pulled past hintMin
    }

    @Test
    fun `unwinding past zero hands the remainder back to the list`() {
        val (connection, _) = connection(atBottomEdge = true)
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
        val (connection, _) = connection(atBottomEdge = true)
        pullUp(-40f, connection)

        assertEquals(0f, drag(-30f, connection).y, 0f)
        assertEquals(-28f, connection.pullPx, 0.01f) // 70 * 0.4
    }

    @Test
    fun `previous pull cancels symmetrically at the top edge`() {
        val (connection, _) = connection(atTopEdge = true)
        pullUp(60f, connection) // dragging down pulls the previous surah
        assertEquals(1, connection.hintDirection)

        val consumed = drag(-25f, connection)

        assertEquals(-25f, consumed.y, 0.01f)
        assertEquals(14f, connection.pullPx, 0.01f) // 35 * 0.4
        assertEquals(1, connection.hintDirection)
    }

    @Test
    fun `gated direction never pulls`() {
        val (connection, _) = connection(atBottomEdge = true, canDown = false)

        pullUp(-40f, connection)

        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
        assertEquals(Offset.Zero, drag(20f, connection))
    }

    @Test
    fun `fling leftovers never arm or move a pull`() {
        val (connection, _) = connection(atBottomEdge = true)

        connection.onPostScroll(Offset.Zero, Offset(0f, -40f), NestedScrollSource.Fling)

        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, 20f), NestedScrollSource.Fling))
    }

    @Test
    fun `release past the threshold switches in the pull direction`() = runTest {
        val fired = mutableListOf<Boolean>()
        val (connection, _) = connection(atBottomEdge = true, onSwitch = { fired += it })
        pullUp(-120f, connection)

        connection.onPreFling(Velocity.Zero)

        assertEquals(listOf(true), fired)
    }

    @Test
    fun `release backwards switches to the previous surah`() = runTest {
        val fired = mutableListOf<Boolean>()
        val (connection, _) = connection(atTopEdge = true, onSwitch = { fired += it })
        pullUp(120f, connection)

        connection.onPreFling(Velocity.Zero)

        assertEquals(listOf(false), fired)
    }

    @Test
    fun `cancelling the pull before release prevents the switch`() = runTest {
        val fired = mutableListOf<Boolean>()
        val (connection, _) = connection(atBottomEdge = true, onSwitch = { fired += it })
        pullUp(-150f, connection)
        drag(200f, connection) // reader changes their mind and drags back

        connection.onPreFling(Velocity.Zero)

        assertTrue(fired.isEmpty())
        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(0, connection.hintDirection)
    }

    @Test
    fun `cancel unwinds the pull before the header may move`() {
        // Collapsed header (-100 of 100px): the old split pair offered the
        // reverse drag to the header first, freezing the pull. The merged
        // connection must spend it on the pull instead.
        val (connection, header) = connection(
            atBottomEdge = true,
            headerOffsetPx = -100f,
            collapsibleHeightPx = 100
        )
        pullUp(-150f, connection)

        val consumed = drag(60f, connection)

        assertEquals(60f, consumed.y, 0.01f)
        assertEquals(-100f, header.headerOffsetPx, 0.01f) // header untouched
        assertEquals(-36f, connection.pullPx, 0.01f)
    }

    @Test
    fun `header resumes only after the pull is fully unwound`() {
        val (connection, header) = connection(
            atBottomEdge = true,
            headerOffsetPx = -100f,
            collapsibleHeightPx = 100
        )
        pullUp(-150f, connection)

        val consumed = drag(200f, connection)

        assertEquals(200f, consumed.y, 0.01f) // 150 pull + 50 header
        assertEquals(0f, connection.pullPx, 0f)
        assertEquals(-50f, header.headerOffsetPx, 0.01f)
    }

    @Test
    fun `selection freezes the header but never the cancel`() {
        val (connection, header) = connection(
            atBottomEdge = true,
            headerOffsetPx = -100f,
            collapsibleHeightPx = 100,
            selectionActive = true
        )
        pullUp(-150f, connection)

        val consumed = drag(60f, connection)

        assertEquals(60f, consumed.y, 0.01f)
        assertEquals(-100f, header.headerOffsetPx, 0.01f)
        assertEquals(-36f, connection.pullPx, 0.01f)
    }

    // Note: the spring-back branch of onPreFling is deliberately not covered
    // here — it drives an `animate` call, which needs a MonotonicFrameClock that
    // plain runTest does not install. That path is verified manually instead.
}

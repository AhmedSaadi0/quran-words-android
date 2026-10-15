package com.quranwords.ui.surah.detail.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.quranwords.ui.theme.AppMotion
import kotlin.math.abs

/** Raw finger travel needed to arm a switch (matches Telegram's feel). */
private val SwitchThreshold = 110.dp

/** Maximum visual displacement of the reading view. */
private val MaxPull = 60.dp

/** Visual pull at which the neighbour hint starts fading in. */
private val HintMin = 10.dp

/**
 * Rubber-band factor applied to finger travel. The threshold is measured on the
 * RAW travel (what the finger did) while the content follows a damped fraction
 * of it — that is what makes a pull feel elastic instead of glued.
 */
private const val PullResistance = 0.4f

/**
 * The single nested-scroll connection of the surah reading screen.
 *
 * Two connections used to share this job (one for the collapsing header, one
 * for the adjacent-surah pull), and the framework always offers a reverse drag
 * to the header first — so cancelling a pull expanded the header while the pull
 * stayed frozen. Merging them makes the priority explicit and order-proof:
 *
 * `onPreScroll` phases, in order:
 * 1. **Cancel a live pull** (touch only): unwind `accumulated` 1:1, taking only
 *    `min(|delta|, |accumulated|)`; the remainder keeps flowing so cancelling
 *    never locks the view.
 * 2. **Quick-return header**: collapse on scroll down, reappear on scroll up
 *    (frozen while selection is active) — same physics as before.
 * 3. Anything left reaches the list.
 *
 * `onPostScroll` only observes the *unconsumed* remainder at a real edge
 * (`!canScrollForward` → next surah, `!canScrollBackward` → previous) and never
 * consumes, so the reading position is never hijacked mid-list. The switch
 * itself fires on **release** in [onPreFling], never mid-drag; fling leftovers
 * are ignored, so momentum never auto-advances.
 *
 * Requires overscroll to be disabled for the list (see `SurahDetailScreen`):
 * the Android 12+ stretch effect sits in front of parent connections and eats
 * unconsumed edge deltas, which would leave the edge observer with nothing.
 */
class SurahReadingScrollConnection internal constructor(
    private val canScrollForward: () -> Boolean,
    private val canScrollBackward: () -> Boolean,
    private val collapseState: SurahCollapsingHeaderState,
    private val isSelectionActive: () -> Boolean,
    private val canSwitchDown: () -> Boolean,
    private val canSwitchUp: () -> Boolean,
    private val onSwitch: (forward: Boolean) -> Unit,
    private val thresholdPx: Float,
    private val maxPullPx: Float,
    private val hintMinPx: Float
) : NestedScrollConnection {

    /** Visual pull applied by the screen; read in the draw phase only. */
    var pullPx by mutableFloatStateOf(0f)
        private set

    /** 0 = hidden, -1 = next-surah badge at the bottom, +1 = previous at the top. */
    var hintDirection by mutableIntStateOf(0)
        private set

    /** Raw finger travel since the drag started (unclamped by the visual limit). */
    private var accumulated = 0f

    /** A fired switch stays fired: no second pull while the slide is running. */
    private var switched = false

    private val maxRawPx: Float = maxPullPx * 4f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (switched) return Offset.Zero
        var remaining = available.y
        if (remaining == 0f) return Offset.Zero
        var consumed = 0f

        // Phase 1 — a live pull unwinds before anything else may move.
        if (source == NestedScrollSource.UserInput && accumulated != 0f) {
            val cancelling = (accumulated < 0f && remaining > 0f) ||
                (accumulated > 0f && remaining < 0f)
            if (cancelling) {
                val take = minOf(abs(remaining), abs(accumulated))
                val unwound = if (remaining > 0f) take else -take
                accumulated += unwound
                consumed += unwound
                remaining -= unwound
                syncVisual()
                if (remaining == 0f) return Offset(0f, consumed)
            }
        }

        // Phase 2 — quick-return header (all sources, as before).
        if (!isSelectionActive()) {
            consumed += consumeCollapse(remaining)
        }
        return Offset(0f, consumed)
    }

    /**
     * Collapses the header on scroll down, re-expands it on scroll up.
     * Returns the consumed Y so the caller can keep forwarding the remainder.
     */
    private fun consumeCollapse(delta: Float): Float {
        val state = collapseState
        if (state.collapsibleHeightPx <= 0) return 0f
        if ((delta < 0f && state.headerOffsetPx > -state.collapsibleHeightPx) ||
            (delta > 0f && state.headerOffsetPx < 0f)
        ) {
            val prevOffset = state.headerOffsetPx
            state.headerOffsetPx = (state.headerOffsetPx + delta)
                .coerceIn(-state.collapsibleHeightPx.toFloat(), 0f)
            return state.headerOffsetPx - prevOffset
        }
        return 0f
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || switched) return Offset.Zero
        val delta = available.y
        if (delta == 0f) return Offset.Zero

        val pullNext = delta < 0f && !canScrollForward() && canSwitchDown()
        val pullPrev = delta > 0f && !canScrollBackward() && canSwitchUp()

        accumulated = if (pullNext || pullPrev) {
            (accumulated + delta).coerceIn(-maxRawPx, maxRawPx)
        } else {
            decay(accumulated, delta)
        }
        syncVisual()
        // Never consumed: scrolling, fling and list position stay untouched.
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val from = accumulated
        val forward = from < 0f
        val allowed = if (forward) canSwitchDown() else canSwitchUp()
        accumulated = 0f
        hintDirection = 0
        if (abs(from) >= thresholdPx && allowed) {
            switched = true
            // pullPx stays frozen: the outgoing screen keeps its offset while
            // the vertical slide takes over.
            onSwitch(forward)
        } else {
            val fromPx = pullPx
            if (abs(fromPx) > 0.5f) {
                animate(fromPx, 0f, animationSpec = AppMotion.SpringGentle) { value, _ ->
                    pullPx = value
                }
            } else {
                pullPx = 0f
            }
        }
        return Velocity.Zero
    }

    /**
     * Projects the raw pull onto the damped visual offset and the hint badge.
     * Shared by every path that moves the pull so the badge can never disagree
     * with what the reader actually sees.
     */
    private fun syncVisual() {
        pullPx = (accumulated * PullResistance).coerceIn(-maxPullPx, maxPullPx)
        hintDirection = when {
            pullPx <= -hintMinPx -> -1
            pullPx >= hintMinPx -> 1
            else -> 0
        }
    }

    /** Moves [current] toward 0 by [delta] but never past 0 and never grows. */
    private fun decay(current: Float, delta: Float): Float {
        if (current == 0f) return 0f
        val next = current + delta
        return if (current > 0f) next.coerceIn(0f, current) else next.coerceIn(current, 0f)
    }
}

/**
 * Remembers the screen's scroll connection.
 *
 * Only [listState] and [collapseState] are keys: the instance must survive
 * every gate flip (pagination, selection, sheet) so a drag in progress can
 * never lose its accumulated travel. Gates and the callback are read through
 * [rememberUpdatedState], so they are always the latest without recreation.
 */
@Composable
fun rememberSurahReadingScroll(
    listState: LazyListState,
    collapseState: SurahCollapsingHeaderState,
    isSelectionActive: Boolean,
    canSwitchDown: Boolean,
    canSwitchUp: Boolean,
    onSwitch: (forward: Boolean) -> Unit
): SurahReadingScrollConnection {
    val density: Density = LocalDensity.current
    val selectionState = rememberUpdatedState(isSelectionActive)
    val canSwitchDownState = rememberUpdatedState(canSwitchDown)
    val canSwitchUpState = rememberUpdatedState(canSwitchUp)
    val onSwitchState = rememberUpdatedState(onSwitch)
    return remember(listState, collapseState) {
        SurahReadingScrollConnection(
            canScrollForward = { listState.canScrollForward },
            canScrollBackward = { listState.canScrollBackward },
            collapseState = collapseState,
            isSelectionActive = { selectionState.value },
            canSwitchDown = { canSwitchDownState.value },
            canSwitchUp = { canSwitchUpState.value },
            onSwitch = { forward -> onSwitchState.value(forward) },
            thresholdPx = with(density) { SwitchThreshold.toPx() },
            maxPullPx = with(density) { MaxPull.toPx() },
            hintMinPx = with(density) { HintMin.toPx() }
        )
    }
}

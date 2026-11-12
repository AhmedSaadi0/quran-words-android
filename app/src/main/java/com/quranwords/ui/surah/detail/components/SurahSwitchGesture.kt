package com.quranwords.ui.surah.detail.components

import androidx.compose.animation.core.animate
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.quranwords.core.util.ReadingModePreference
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
 * Chrome show/hide no longer lives here — the reading top bar is a persistent
 * overlay driven by the screen's own scroll observer — so this connection has
 * one job: the adjacent-surah rubber-band pull with explicit priority.
 *
 * `onPreScroll` phase:
 * 1. **Cancel a live pull** (touch only): unwind `accumulated` 1:1, taking only
 *    `min(|delta|, |accumulated|)`; the remainder keeps flowing so cancelling
 *    never locks the view. Anything left reaches the list untouched.
 *
 * `onPostScroll` only observes the *unconsumed* remainder at a real edge
 * (`!canScrollForward` → next surah, `!canScrollBackward` → previous) and never
 * consumes, so the reading position is never hijacked mid-list. The switch
 * itself fires on **release** in [onPreFling], never mid-drag; fling leftovers
 * are ignored, so momentum never auto-advances.
 *
 * The pull AXIS follows [ReadingModePreference]: vertical (up/down past the
 * list edges, default) or horizontal book-style flip at the same edges, with
 * the raw direction mirrored by the layout direction. Internally every delta
 * is normalized to "negative = toward the next surah", so gates, thresholds
 * and the hint badge are axis-agnostic; only the visual mapping differs.
 *
 * Requires overscroll to be disabled for the list (see `SurahDetailScreen`):
 * the Android 12+ stretch effect sits in front of parent connections and eats
 * unconsumed edge deltas, which would leave the edge observer with nothing.
 */
class SurahReadingScrollConnection internal constructor(
    private val canScrollForward: () -> Boolean,
    private val canScrollBackward: () -> Boolean,
    private val canSwitchDown: () -> Boolean,
    private val canSwitchUp: () -> Boolean,
    private val onSwitch: (forward: Boolean) -> Unit,
    private val thresholdPx: Float,
    private val maxPullPx: Float,
    private val hintMinPx: Float,
    /** Active pull axis: false = vertical pull, true = horizontal book flip. */
    private val horizontal: () -> Boolean,
    /** Layout direction, which mirrors the horizontal swipe directions. */
    private val rtl: () -> Boolean
) : NestedScrollConnection {

    /** Visual pull applied by the screen; read in the draw phase only. */
    var pullPx by mutableFloatStateOf(0f)
        private set

    /**
     * 0 = hidden, -1 = next-surah badge, +1 = previous-surah badge. Sign only —
     * the badge's EDGE (bottom/top vs end/start) is the screen's choice per axis.
     */
    var hintDirection by mutableIntStateOf(0)
        private set

    /** Raw finger travel since the drag started (unclamped by the visual limit). */
    private var accumulated = 0f

    /** A fired switch stays fired: no second pull while the slide is running. */
    private var switched = false

    private val maxRawPx: Float = maxPullPx * 4f

    /**
     * Normalized axis delta from a raw [offset]: negative means "toward the
     * next surah" on the ACTIVE axis. Vertical: up is next. Horizontal RTL
     * (mushaf convention): right is next; LTR mirrors it.
     */
    private fun axisDelta(offset: Offset): Float = when {
        !horizontal() -> offset.y
        rtl() -> -offset.x
        else -> offset.x
    }

    /** Inverse of [axisDelta]: reports a normalized consumption on the real axis. */
    private fun consumeOnAxis(normalized: Float): Offset = when {
        !horizontal() -> Offset(0f, normalized)
        rtl() -> Offset(-normalized, 0f)
        else -> Offset(normalized, 0f)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (switched) return Offset.Zero
        val remaining = axisDelta(available)
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
                syncVisual()
                if (remaining - unwound == 0f) return consumeOnAxis(consumed)
            }
        }

        // No header phase remains: reading chrome is a persistent overlay,
        // its visibility driven by the screen's scroll observer — never here.
        return consumeOnAxis(consumed)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || switched) return Offset.Zero
        val delta = axisDelta(available)
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
            // the axis slide takes over.
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
 * No keys: the instance must survive every gate flip so a drag in progress can
 * never lose its accumulated travel. The scroll-edge gates are plain lambdas
 * (list state in SCROLL mode, pager state in PAGES mode) and everything else
 * is read through [rememberUpdatedState], so the connection is always fed the
 * latest values without recreation.
 */
@Composable
fun rememberSurahReadingScroll(
    canScrollForward: () -> Boolean,
    canScrollBackward: () -> Boolean,
    readingMode: Int,
    canSwitchDown: Boolean,
    canSwitchUp: Boolean,
    onSwitch: (forward: Boolean) -> Unit
): SurahReadingScrollConnection {
    val density: Density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val horizontalState = rememberUpdatedState(readingMode == ReadingModePreference.PAGES)
    val rtlState = rememberUpdatedState(isRtl)
    val canScrollForwardState = rememberUpdatedState(canScrollForward)
    val canScrollBackwardState = rememberUpdatedState(canScrollBackward)
    val canSwitchDownState = rememberUpdatedState(canSwitchDown)
    val canSwitchUpState = rememberUpdatedState(canSwitchUp)
    val onSwitchState = rememberUpdatedState(onSwitch)
    return remember {
        SurahReadingScrollConnection(
            canScrollForward = { canScrollForwardState.value() },
            canScrollBackward = { canScrollBackwardState.value() },
            canSwitchDown = { canSwitchDownState.value },
            canSwitchUp = { canSwitchUpState.value },
            onSwitch = { forward -> onSwitchState.value(forward) },
            thresholdPx = with(density) { SwitchThreshold.toPx() },
            maxPullPx = with(density) { MaxPull.toPx() },
            hintMinPx = with(density) { HintMin.toPx() },
            horizontal = { horizontalState.value },
            rtl = { rtlState.value }
        )
    }
}

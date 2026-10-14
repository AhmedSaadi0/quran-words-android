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
 * Telegram-style adjacent-surah switch gesture.
 *
 * Observational while scrolling: [onPostScroll] only observes the *unconsumed*
 * remainder at a real edge and returns `Offset.Zero`, so the list keeps every
 * pixel it can scroll and the reading position is never hijacked mid-list.
 *
 * - dragging up at the bottom edge (`!canScrollForward`) pulls toward the next
 *   surah, dragging down at the top edge (`!canScrollBackward`) toward the
 *   previous one;
 * - [pullPx] follows the finger with rubber-band damping and drives the
 *   hint badge; [hintDirection] picks which badge is shown;
 * - dragging back towards rest cancels the pull in [onPreScroll] — the only
 *   place this connection consumes: the list would otherwise swallow the reverse
 *   delta itself (it can scroll back), leaving [onPostScroll] blind and a stale
 *   pull that would still fire on release;
 * - the switch itself fires on **release** in [onPreFling], never mid-drag, so a
 *   cancelled or short pull simply springs back;
 * - fling leftovers ([NestedScrollSource.Fling]) are ignored — momentum never
 *   auto-advances to the next surah.
 *
 * Requires overscroll to be disabled for the list (see `SurahDetailScreen`):
 * the Android 12+ stretch effect sits in front of parent connections and eats
 * unconsumed edge deltas, which would leave this observer with nothing to see.
 */
class SurahSwitchGesture internal constructor(
    private val canScrollForward: () -> Boolean,
    private val canScrollBackward: () -> Boolean,
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

    /**
     * Cancels a live pull when the reader drags back towards the resting
     * position. Without this, the child list would consume the reverse delta
     * (it can scroll that way), [onPostScroll] would see nothing, and the stale
     * pull would still arm the switch on release.
     */
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || switched) return Offset.Zero
        val delta = available.y
        if (delta == 0f || accumulated == 0f) return Offset.Zero
        val cancelling = (accumulated < 0f && delta > 0f) || (accumulated > 0f && delta < 0f)
        if (!cancelling) return Offset.Zero

        // Take only what the pull needs to unwind — the remainder keeps scrolling
        // the list in the same frame, so cancelling never locks the view.
        val take = minOf(abs(delta), abs(accumulated))
        val consumed = if (delta > 0f) take else -take
        accumulated += consumed
        syncVisual()
        return Offset(0f, consumed)
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
 * Remembers the gesture for a list.
 *
 * Only [listState] is a key: the gesture instance must survive every gate flip
 * (pagination, selection, sheet) so a drag in progress can never lose its
 * accumulated travel. Gates and the callback are read through
 * [rememberUpdatedState], so they are always the latest without recreation.
 */
@Composable
fun rememberSurahSwitchGesture(
    listState: LazyListState,
    canSwitchDown: Boolean,
    canSwitchUp: Boolean,
    onSwitch: (forward: Boolean) -> Unit
): SurahSwitchGesture {
    val density: Density = LocalDensity.current
    val canSwitchDownState = rememberUpdatedState(canSwitchDown)
    val canSwitchUpState = rememberUpdatedState(canSwitchUp)
    val onSwitchState = rememberUpdatedState(onSwitch)
    return remember(listState) {
        SurahSwitchGesture(
            canScrollForward = { listState.canScrollForward },
            canScrollBackward = { listState.canScrollBackward },
            canSwitchDown = { canSwitchDownState.value },
            canSwitchUp = { canSwitchUpState.value },
            onSwitch = { forward -> onSwitchState.value(forward) },
            thresholdPx = with(density) { SwitchThreshold.toPx() },
            maxPullPx = with(density) { MaxPull.toPx() },
            hintMinPx = with(density) { HintMin.toPx() }
        )
    }
}

package com.quranwords.ui.roots.detail.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt

/**
 * Isolated collapse state for RootDetail header (reveal-only-at-top).
 *
 * - Finger drag up collapses the subtitle first (`onPreScroll`, `delta < 0`).
 * - Finger drag down re-expands it ONLY from leftover deltas in `onPostScroll`
 *   (i.e. the active LazyColumn has exhausted its scroll at index 0 / offset 0).
 *   Mid-list scroll-ups never reveal the header.
 * - Offset clamped to `[-subtitleHeight, 0]` (full natural AI-summary height,
 *   no viewport cap), progress = `1 + offset / height` (for alpha).
 * - While multi-selecting (words/meanings), offset consumption is disabled
 *   so the header can't abruptly expand under the user's taps.
 *
 * Perf fix vs legacy: [subtitleHeightPx] / [headerOffsetPx] previously lived
 * in the parent `RootDetailScreen`, so every drag pixel recomposed the entire
 * Scaffold (header + HorizontalPager + 5 LazyColumns) and wrote state inside
 * `Modifier.layout`. Now the state lives here; the parent only holds the
 * stable [nestedScrollConnection] and never reads offset/height, so drag
 * events recompose **only** the header. Measurement uses `onSizeChanged`
 * (not a state-write inside `layout`).
 */
@Stable
class CollapsingHeaderState(
    initialHeightPx: Int = 0,
    initialOffsetPx: Float = 0f,
) {
    var subtitleHeightPx by mutableIntStateOf(initialHeightPx)
        internal set
    var headerOffsetPx by mutableFloatStateOf(initialOffsetPx)
        internal set

    /**
     * Transient UI flag mirrored from [RootDetailUiState] by the Screen inside
     * a `SideEffect` each composition. Deliberately NOT part of [Saver] —
     * selection must reset to false after process death, never restore active.
     */
    var isSelectionMode: Boolean by mutableStateOf(false)
        internal set

    /**
     * Transient provider of the currently visible tab's [LazyListState], mirrored
     * from the Screen (same pattern as [isSelectionMode], deliberately NOT part
     * of [Saver]). Lets [HeaderNestedScrollConnection.onPostFling] continue a
     * header-originated fling into the list with standard decay physics, so a
     * swipe starting on the header glides exactly like one starting on the list.
     */
    var activeListProvider: (() -> LazyListState?)? = null
        internal set

    val collapseProgress: Float
        get() = if (subtitleHeightPx > 0) {
            (1f + (headerOffsetPx / subtitleHeightPx.toFloat())).coerceIn(0f, 1f)
        } else {
            1f
        }

    val currentHeightPx: Int
        get() = if (subtitleHeightPx > 0) {
            (subtitleHeightPx + headerOffsetPx.roundToInt()).coerceIn(0, subtitleHeightPx)
        } else {
            0
        }

    /**
     * The inner Column is measured unbounded (see `wrapContentHeight(...,
     * unbounded = true)` in `RootDetailHeader`), so [measuredPx] is always the
     * TRUE natural height — assign directly. Shrinks (rotation to landscape,
     * shorter summary on root change) adapt immediately; the offset is clamped
     * into the new range so a stale fully-collapsed offset can never exceed
     * the new height.
     */
    fun onNaturalHeightMeasured(measuredPx: Int) {
        if (measuredPx > 0 && measuredPx != subtitleHeightPx) {
            subtitleHeightPx = measuredPx
            headerOffsetPx = headerOffsetPx.coerceIn(-subtitleHeightPx.toFloat(), 0f)
        }
    }

    /**
     * Direct-touch drive for the header's own `scrollable` modifier (large-font
     * trap fix): drags landing on the non-scrollable subtitle area would never
     * dispatch nested scroll, so they drive the same clamped physics directly.
     * Same sign convention as nested deltas — drag-up (`delta < 0`) collapses.
     *
     * Returns the actually applied delta so the scrollable can report honest
     * consumption: the remainder keeps flowing through the nested chain (and,
     * on finger lift, surfaces as real leftover velocity in [onPostFling]).
     */
    fun onHandledDrag(delta: Float): Float {
        if (isSelectionMode || subtitleHeightPx <= 0) return 0f
        val prev = headerOffsetPx
        headerOffsetPx = (headerOffsetPx + delta).coerceIn(-subtitleHeightPx.toFloat(), 0f)
        return headerOffsetPx - prev
    }

    val nestedScrollConnection: NestedScrollConnection = HeaderNestedScrollConnection()

    private inner class HeaderNestedScrollConnection : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Selection guard: freeze the header while multi-selecting so it
            // can't abruptly expand under the user's taps and cause miss-clicks.
            if (isSelectionMode) return Offset.Zero
            val delta = available.y
            // Collapse only: consume upward drags before the list sees them.
            if (delta < 0f && subtitleHeightPx > 0 && headerOffsetPx > -subtitleHeightPx) {
                val prev = headerOffsetPx
                headerOffsetPx = (headerOffsetPx + delta).coerceIn(-subtitleHeightPx.toFloat(), 0f)
                return Offset(0f, headerOffsetPx - prev)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (isSelectionMode) return Offset.Zero
            // Expand only from leftover: the active LazyColumn consumed
            // everything it could, so a remaining downward delta means the
            // list is at index 0 / offset 0. Works across all pager tabs with
            // zero LazyListState reads (no recomposition overhead) and handles
            // flings that reach the top mid-gesture in the same motion.
            val delta = available.y
            if (delta > 0f && subtitleHeightPx > 0 && headerOffsetPx < 0f) {
                val prev = headerOffsetPx
                headerOffsetPx = (headerOffsetPx + delta).coerceIn(-subtitleHeightPx.toFloat(), 0f)
                return Offset(0f, headerOffsetPx - prev)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (isSelectionMode) return Velocity.Zero
            // Leftover of a header-originated fling (the header hit its clamp
            // mid-gesture): continue it into the visible tab's list with the
            // standard decay, so the glide feels identical to list-originated
            // scrolling. A new touch cancels via LazyList's MutatorMutex.
            val list = activeListProvider?.invoke() ?: return Velocity.Zero
            val velocityY = available.y
            if (velocityY == 0f) return Velocity.Zero
            var last = 0f
            list.scroll {
                Animatable(0f).animateDecay(velocityY, exponentialDecay()) {
                    scrollBy(value - last)
                    last = value
                }
            }
            return available
        }
    }

    companion object {
        val Saver: Saver<CollapsingHeaderState, Pair<Int, Float>> = Saver(
            save = { it.subtitleHeightPx to it.headerOffsetPx },
            restore = { (h, o) -> CollapsingHeaderState(h, o) }
        )
    }
}

@Composable
fun rememberCollapsingHeaderState(): CollapsingHeaderState {
    // Saveable so back-nav / process death restores collapse (legacy behaviour),
    // but scoped to the header — parent never reads these values directly.
    return rememberSaveable(saver = CollapsingHeaderState.Saver) {
        CollapsingHeaderState()
    }
}

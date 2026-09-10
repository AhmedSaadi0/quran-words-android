package io.github.ahmedsaadi0.quranwords.ui.roots.detail.components

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
import kotlin.math.roundToInt

/**
 * Isolated collapse state for RootDetail header (Quick-Return / Enter Always).
 *
 * - Finger drag up collapses the subtitle first (`onPreScroll`, `delta < 0`).
 * - Finger drag down re-expands it immediately anywhere in the page
 *   (`onPreScroll`, `delta > 0`, before the list consumes anything) —
 *   mirroring SurahDetailScreen's proven behavior.
 * - Offset clamped to `[-effectiveHeight, 0]` (natural height capped to
 *   40% of the viewport so tabs stay grabbable at any font scale),
 *   progress = `1 + offset / height` (for alpha).
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
     * Viewport cap in px, reported by the header's `BoxWithConstraints`
     * (40% of the Scaffold content height). Transient layout data like
     * [isSelectionMode] — deliberately NOT part of [Saver].
     */
    var maxHeightPx: Int by mutableIntStateOf(Int.MAX_VALUE)
        internal set

    /** Visible height budget: the natural height capped to the viewport share. */
    val effectiveHeightPx: Int
        get() = minOf(subtitleHeightPx, maxHeightPx)

    /** True when the summary overflows the cap and is clipped. */
    val isCapped: Boolean
        get() = subtitleHeightPx > effectiveHeightPx && effectiveHeightPx > 0

    val collapseProgress: Float
        get() = if (effectiveHeightPx > 0) {
            (1f + (headerOffsetPx / effectiveHeightPx.toFloat())).coerceIn(0f, 1f)
        } else {
            1f
        }

    val currentHeightPx: Int
        get() = if (effectiveHeightPx > 0) {
            (effectiveHeightPx + headerOffsetPx.roundToInt()).coerceIn(0, effectiveHeightPx)
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
            headerOffsetPx = headerOffsetPx.coerceIn(-effectiveHeightPx.toFloat(), 0f)
        }
    }

    fun onViewportCapChanged(capPx: Int) {
        if (capPx > 0 && capPx != maxHeightPx) {
            maxHeightPx = capPx
            headerOffsetPx = headerOffsetPx.coerceIn(-effectiveHeightPx.toFloat(), 0f)
        }
    }

    /**
     * Direct-touch drive for the header's own `draggable` modifier (large-font
     * trap fix): drags landing on the non-scrollable subtitle area would never
     * dispatch nested scroll, so they drive the same clamped physics directly.
     * Same sign convention as nested deltas — drag-up (`delta < 0`) collapses.
     */
    fun onHandledDrag(delta: Float) {
        if (isSelectionMode || subtitleHeightPx <= 0) return
        headerOffsetPx = (headerOffsetPx + delta).coerceIn(-effectiveHeightPx.toFloat(), 0f)
    }

    val nestedScrollConnection: NestedScrollConnection = HeaderNestedScrollConnection()

    private inner class HeaderNestedScrollConnection : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Selection guard: freeze the header while multi-selecting so it
            // can't abruptly expand under the user's taps and cause miss-clicks.
            if (isSelectionMode) return Offset.Zero
            val delta = available.y
            if (effectiveHeightPx > 0) {
                // Quick-Return (Enter Always), cf. SurahDetailScreen:
                // collapse on drag-up, re-expand on drag-down anywhere in the
                // page — consumed here in pre-scroll, before the list sees it.
                if ((delta < 0f && headerOffsetPx > -effectiveHeightPx) ||
                    (delta > 0f && headerOffsetPx < 0f)
                ) {
                    val prev = headerOffsetPx
                    headerOffsetPx = (headerOffsetPx + delta).coerceIn(-effectiveHeightPx.toFloat(), 0f)
                    return Offset(0f, headerOffsetPx - prev)
                }
            }
            return Offset.Zero
        }
        // NOTE: onPostScroll intentionally deleted — with both directions
        // handled in pre-scroll (proven by SurahDetailScreen), a post handler
        // would double-consume upward drags.
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

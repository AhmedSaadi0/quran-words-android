package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafSegment
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafPageUi
import io.github.ahmedsaadi0.quranwords.ui.theme.AmiriQuran

internal const val FIT_MIN_SP = 12f
internal const val FIT_MAX_SP = 28f
internal const val FIT_LINE_HEIGHT_FACTOR = 1.75f
internal const val FIT_ITERATIONS = 6

/**
 * Vertical budget reserved per surah banner, matching the rendered
 * ultra-compact metrics exactly — measured budget always equals rendered
 * chrome, so no drift pushes verses off the page. Below [FIT_MIN_SP] the
 * body's scroll fallback engages instead of shrinking further (hard floor,
 * never clips).
 */
internal val BANNER_RESERVE = 48.dp
internal val COMPACT_BANNER_RESERVE = 44.dp

/**
 * Pure, unit-tested: largest size in [min, max] whose [measure] fits [budgetPx].
 * [measure] must be monotonic non-decreasing in size (true for text height).
 */
internal fun binarySearchFontSize(
    min: Float,
    max: Float,
    iterations: Int,
    budgetPx: Int,
    measure: (Float) -> Int
): Float {
    var lo = min
    var hi = max
    repeat(iterations) {
        val mid = (lo + hi) / 2f
        if (measure(mid) <= budgetPx) lo = mid else hi = mid
    }
    return lo
}

/**
 * Fitted body font size for one Mushaf page: the whole page text is measured
 * (same builder and style as the renderer) and binary-searched in
 * [FIT_MIN_SP, FIT_MAX_SP] against the banner-discounted height budget.
 * Sparse pages cap at [FIT_MAX_SP] and are centered by the body layout.
 */
@Composable
internal fun rememberFittedMushafFontSize(
    pageUi: MushafPageUi,
    markerStyle: SpanStyle,
    bannerReservePx: Int,
    maxWidthPx: Int,
    maxHeightPx: Int
): Float {
    val measurer = rememberTextMeasurer()
    return remember(pageUi, markerStyle, bannerReservePx, maxWidthPx, maxHeightPx) {
        if (maxWidthPx <= 0 || maxHeightPx <= 0) return@remember FIT_MAX_SP
        val textRuns = pageUi.segments.filterIsInstance<MushafSegment.TextRun>().flatMap { it.ayat }
        if (textRuns.isEmpty()) return@remember FIT_MAX_SP
        val fullText: AnnotatedString = buildMushafAnnotated(textRuns, markerStyle)
        val budgetPx = (maxHeightPx - bannerReservePx).coerceAtLeast(0)
        binarySearchFontSize(FIT_MIN_SP, FIT_MAX_SP, FIT_ITERATIONS, budgetPx) { size ->
            measurer.measure(
                text = fullText,
                style = TextStyle(
                    fontFamily = AmiriQuran,
                    fontSize = size.sp,
                    lineHeight = (size * FIT_LINE_HEIGHT_FACTOR).sp
                ),
                constraints = Constraints(maxWidth = maxWidthPx)
            ).size.height
        }
    }
}

package com.quranwords.ui.surah.detail.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.domain.model.Ayah
import com.quranwords.ui.theme.QuranFont
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Smallest fitted font size (sp) — floor even when the page still overflows. */
internal const val MUSHAF_FIT_MIN_SP = 10f

/** Largest fitted font size (sp) — short pages stay book-like. */
internal const val MUSHAF_FIT_MAX_SP = 40f

/** Fraction of the text slot the fitted block may occupy. Tight (0.5% slack)
 * because the capped line-stretch + px verification absorb rounding — this is
 * what makes a page end at the bottom edge instead of leaving a visible band. */
private const val MUSHAF_FIT_SAFETY = 0.995f

/** Maximum extra leading as a fraction of the natural line height. */
internal const val MUSHAF_STRETCH_MAX_RATIO = 0.25f

/** Binary-search resolution (sp) — fine enough to leave almost no dead space. */
private const val FIT_RESOLUTION_SP = 0.1f

/** Rectangle refinement: smallest downward font step and its hard bounds. */
internal const val RECT_REFINE_STEP_SP = 0.25f
internal const val RECT_REFINE_MAX_SHRINK_SP = 2.5f
internal const val RECT_REFINE_SHRINK_RATIO = 0.12f
/** Segments shorter than this cannot meaningfully "complete a rectangle". */
internal const val RECT_REFINE_MIN_LINES = 4

/**
 * Conservative px shaved off the measured width: the slot arrives as Dp from
 * BoxWithConstraints while the real Text gets integer-px constraints after
 * its own padding — the slack guarantees the measured width never exceeds the
 * real one, so a line break can never be missed (which would overflow).
 */
private const val WIDTH_SLACK_PX = 2

/** MushafFlowBlock's own Text padding — the fit measures the inner text only. */
private val BLOCK_H_PADDING = 4.dp
private val BLOCK_V_PADDING = 2.dp

/** Gap between banner/text nodes inside one page slot (shared with the pager). */
internal val MUSHAF_SEGMENT_GAP: Dp = 8.dp

/** Font size + extra leading (sp) that make a whole page fill its slot exactly. */
internal data class MushafPageFit(val fontSize: Float, val extraLineSpacingSp: Float)

/** Geometry-only identity of one fit: content + measured slot + font metrics.
 * Theme colours are deliberately absent — they never affect text bounds. */
internal data class MushafFitKey(
    val ayahIds: List<Int>,
    val innerWidthPx: Int,
    val innerHeightPx: Int,
    val quranFontKey: String,
    val fontScale: Float
)

/** Tiny screen-level LRU of fit RESULTS ONLY (no TextLayoutResult retained). */
internal class MushafFitCache : LinkedHashMap<MushafFitKey, MushafPageFit>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<MushafFitKey, MushafPageFit>?): Boolean =
        size > 32
}

/**
 * Canonical MushafFlowBlock [TextStyle]: the cut's leading plus an optional
 * stretch, no font padding, paragraph-balanced breaking and full justification.
 * Shared by render and measurement so they can never drift; the last-line
 * justification reads the real layout and follows the stretch automatically.
 */
internal fun mushafBlockTextStyle(
    base: TextStyle,
    fontSize: Float,
    quranFont: QuranFont,
    extraLineSpacingSp: Float = 0f
): TextStyle =
    base.copy(
        fontSize = fontSize.sp,
        lineHeight = (fontSize * quranFont.lineHeightMultiplier + extraLineSpacingSp).sp,
        fontWeight = FontWeight.Normal,
        fontFamily = quranFont.fontFamily,
        textAlign = TextAlign.Justify,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineBreak = LineBreak.Paragraph,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

/** Fit measurement uses the same character builder as the renderer (P3-1). */
internal fun mushafBlockLayoutText(ayat: List<Ayah>, fontSize: Float): AnnotatedString =
    buildMushafAnnotatedString(ayat, fontSize)

/**
 * Pure sub-pixel-safe line-spacing resolver. The per-line extra is floored to
 * whole px before distribution (so N lines can never add up to more than the
 * available leftover), capped to [MUSHAF_STRETCH_MAX_RATIO] of natural leading
 * (short pages stay book-like), then verified against the real measured total.
 * [fits] re-measures with the final style and catches per-line rounding.
 */
internal fun resolveExtraLineSpacingPx(
    leftoverPx: Float,
    totalLines: Int,
    naturalLineHeightPx: Float,
    fits: (Float) -> Boolean
): Float {
    if (totalLines <= 0 || leftoverPx <= 0f || naturalLineHeightPx <= 0f) return 0f
    val maxExtra = naturalLineHeightPx * MUSHAF_STRETCH_MAX_RATIO
    var extra = floor(min(leftoverPx / totalLines, maxExtra))
    while (extra > 0f && !fits(extra)) extra -= 1f
    return extra
}

/**
 * Pure bounded search for the largest font size at or below [maxFitSp] whose
 * last line completes the rectangle. Checks [maxFitSp] first (zero extra work
 * when it already qualifies), then scans downward in [stepSp] steps, never
 * shrinking more than [maxShrinkSp]. A candidate must satisfy [isRectangle]
 * (the real `computeLastLineJustification` rule), still fit vertically, and
 * leave no more slack than Phase 2's line-stretch can absorb.
 *
 * Pure lambdas keep the policy unit-testable without a Compose fixture; no
 * glyph scale / letter spacing is ever altered — only the uniform font size.
 */
internal fun refineFontSizeForRectangle(
    maxFitSp: Float,
    stepSp: Float = RECT_REFINE_STEP_SP,
    maxShrinkSp: Float = min(RECT_REFINE_MAX_SHRINK_SP, maxFitSp * RECT_REFINE_SHRINK_RATIO),
    isRectangle: (Float) -> Boolean,
    fitsVertically: (Float) -> Boolean,
    slackWithinCap: (Float) -> Boolean
): Float {
    if (maxFitSp <= 0f || stepSp <= 0f || maxShrinkSp <= 0f) return maxFitSp
    // Phase 1 already guaranteed maxFitSp fits vertically — one rectangle
    // check is enough to keep it with zero extra work.
    if (isRectangle(maxFitSp)) return maxFitSp
    var distance = stepSp
    while (distance <= maxShrinkSp + 1e-4f) {
        val candidate = maxFitSp - distance
        if (candidate < MUSHAF_FIT_MIN_SP) break
        if (isRectangle(candidate) && fitsVertically(candidate) && slackWithinCap(candidate)) {
            return candidate
        }
        distance += stepSp
    }
    return maxFitSp
}

/**
 * Fits ALL surah segments of one mushaf page at once: one shared font size for
 * every block, banner/basmalah heights (measured by the caller) subtracted from
 * the slot, then a strictly capped line-spacing stretch that consumes only the
 * sub-line leftover. Addresses 2- and 3-surah pages (Juz Amma) uniformly.
 */
@Composable
internal fun rememberMushafPageFit(
    segments: List<MushafPageSegment>,
    bannerHeightsPx: List<Int>,
    quranFont: QuranFont,
    slotWidth: Dp,
    slotHeight: Dp,
    fitCache: MushafFitCache? = null
): MushafPageFit {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val baseStyle = LocalTextStyle.current
    return remember(segments, bannerHeightsPx, quranFont, slotWidth, slotHeight, fontScale, baseStyle, fitCache) {
        if (segments.isEmpty()) return@remember MushafPageFit(MUSHAF_FIT_MIN_SP, 0f)
        val bannerPx = bannerHeightsPx.sum().toFloat()
        val innerWidthPx = with(density) { (slotWidth - BLOCK_H_PADDING * 2).toPx() }.roundToInt() - WIDTH_SLACK_PX
        // Every segment is its own Text (own vertical padding), and the
        // segment column inserts a gap between banner/text nodes.
        val nodeCount = segments.size + bannerHeightsPx.size
        val gapsPx = ((nodeCount - 1).coerceAtLeast(0)) * with(density) { MUSHAF_SEGMENT_GAP.toPx() }
        val innerHeightPx = with(density) { slotHeight.toPx() } -
            with(density) { (BLOCK_V_PADDING * 2).toPx() } * segments.size - bannerPx - gapsPx
        if (innerWidthPx <= 0 || innerHeightPx <= 0) return@remember MushafPageFit(MUSHAF_FIT_MIN_SP, 0f)

        // Geometry-only key: revisiting a disposed page reuses its tiny fit.
        val key = MushafFitKey(
            ayahIds = segments.flatMap { segment -> segment.ayat.map { it.id } },
            innerWidthPx = innerWidthPx,
            innerHeightPx = innerHeightPx.roundToInt(),
            quranFontKey = quranFont.key,
            fontScale = fontScale
        )
        fitCache?.get(key)?.let { return@remember it }

        val targetPx = innerHeightPx * MUSHAF_FIT_SAFETY
        // Strictly local: layout results die with this fit computation.
        val measureCache = HashMap<Pair<Float, Float>, List<TextLayoutResult>>()
        fun measure(fontSize: Float, extraSp: Float = 0f) =
            measureCache.getOrPut(fontSize to extraSp) {
                segments.map { segment ->
                    textMeasurer.measure(
                        text = mushafBlockLayoutText(segment.ayat, fontSize),
                        style = mushafBlockTextStyle(baseStyle, fontSize, quranFont, extraSp),
                        constraints = Constraints(maxWidth = innerWidthPx),
                        // Mushaf blocks always render RTL regardless of the app locale.
                        layoutDirection = LayoutDirection.Rtl
                    )
                }
            }

        fun fitsAt(fontSize: Float): Boolean =
            measure(fontSize).sumOf { it.size.height } <= targetPx

        val maxFitSp = when {
            !fitsAt(MUSHAF_FIT_MIN_SP) -> MUSHAF_FIT_MIN_SP
            fitsAt(MUSHAF_FIT_MAX_SP) -> MUSHAF_FIT_MAX_SP
            else -> {
                var low = MUSHAF_FIT_MIN_SP
                var high = MUSHAF_FIT_MAX_SP
                while (high - low > FIT_RESOLUTION_SP) {
                    val mid = (low + high) / 2f
                    if (fitsAt(mid)) low = mid else high = mid
                }
                low
            }
        }

        // Rectangle awareness: reuse the REAL last-line rule (same text/style/
        // constraints as rendering) so the chosen size is exactly the one the
        // block will justify at draw time.
        fun lastLineCompletesRectangle(size: Float): Boolean {
            val layouts = measure(size)
            val style = mushafBlockTextStyle(baseStyle, size, quranFont)
            // One space measurement per candidate, reused by every segment.
            val spaceWidthPx = textMeasurer.measure(
                text = " ",
                style = style,
                constraints = Constraints(maxWidth = innerWidthPx),
                layoutDirection = LayoutDirection.Rtl
            ).size.width.toFloat()
            return segments.indices.any { index ->
                val layout = layouts[index]
                if (layout.lineCount < RECT_REFINE_MIN_LINES) return@any false
                if (layout.lastLineEmptySpace() <= LAST_LINE_EPS_PX) return@any true
                val text = mushafBlockLayoutText(segments[index].ayat, size)
                // Exact geometry prefilter: skip token re-measuring when the
                // per-gap width cannot possibly satisfy the renderer's rule.
                val lefts = lastLineTokenRanges(text, layout).map { range ->
                    minOf(layout.getBoundingBox(range.first).left, layout.getBoundingBox(range.last).left)
                }
                val fill = resolveLastLineFill(lefts, layout.getLineLeft(layout.lineCount - 1))
                if (fill?.isAlreadyFull == true) return@any true
                if (!lastLineCanBeJustified(fill, spaceWidthPx)) return@any false
                computeLastLineJustification(
                    text = text,
                    layout = layout,
                    style = style,
                    textMeasurer = textMeasurer
                ) != null
            }
        }

        fun slackWithinStretchCap(size: Float): Boolean {
            val layouts = measure(size)
            val totalLines = layouts.sumOf { it.lineCount }.coerceAtLeast(1)
            val leftoverPx = targetPx - layouts.sumOf { it.size.height }
            val perLineCapPx = with(density) { size.sp.toPx() } * quranFont.lineHeightMultiplier *
                MUSHAF_STRETCH_MAX_RATIO
            return leftoverPx <= totalLines * perLineCapPx + 0.5f
        }

        val fontSize = refineFontSizeForRectangle(
            maxFitSp = maxFitSp,
            isRectangle = { size -> lastLineCompletesRectangle(size) },
            fitsVertically = { size -> fitsAt(size) },
            slackWithinCap = { size -> slackWithinStretchCap(size) }
        )

        val measured = measure(fontSize)
        val totalLines = measured.sumOf { it.lineCount }
        val leftoverPx = targetPx - measured.sumOf { it.size.height }
        val naturalLineHeightPx = with(density) { fontSize.sp.toPx() } * quranFont.lineHeightMultiplier
        // toSp() yields a TextUnit; the fit/style API speaks raw Float sp.
        val extraPx = resolveExtraLineSpacingPx(leftoverPx, totalLines, naturalLineHeightPx) { extra ->
            measure(fontSize, with(density) { extra.toSp().value }).sumOf { it.size.height } <= targetPx
        }
        val fit = MushafPageFit(fontSize, with(density) { extraPx.toSp().value })
        fitCache?.put(key, fit)
        fit
    }
}

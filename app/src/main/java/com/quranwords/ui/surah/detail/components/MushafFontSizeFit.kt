package com.quranwords.ui.surah.detail.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.core.util.sanitizeUthmanicText
import com.quranwords.core.util.toEasternArabicDigits
import com.quranwords.ui.theme.QuranFont
import kotlin.math.roundToInt

/** Smallest fitted font size (sp) — floor even when the page still overflows. */
internal const val MUSHAF_FIT_MIN_SP = 10f

/**
 * Largest fitted font size (sp) — caps short pages (e.g. a surah boundary
 * group that contributes only part of a page) so they stay book-like.
 */
internal const val MUSHAF_FIT_MAX_SP = 40f

/** Fraction of the text slot the fitted block may occupy (absorbs rounding). */
private const val MUSHAF_FIT_SAFETY = 0.98f

/** Binary-search resolution (sp) — fine enough to leave almost no dead space. */
private const val FIT_RESOLUTION_SP = 0.1f

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

/**
 * The canonical MushafFlowBlock [TextStyle]: the cut's leading, no font
 * padding, paragraph-balanced line breaking and full justification. Shared
 * by the block's own Text and the pages-mode fit so render and measurement
 * can never drift apart.
 */
internal fun mushafBlockTextStyle(base: TextStyle, fontSize: Float, quranFont: QuranFont): TextStyle =
    base.copy(
        fontSize = fontSize.sp,
        lineHeight = (fontSize * quranFont.lineHeightMultiplier).sp,
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

/**
 * Character-identical mirror of the [MushafFlowBlock] AnnotatedString —
 * sanitized words, inter-ayah/inter-word spaces and the ﴿n﴾ end markers
 * (marker size ×0.9 bold, both layout-affecting) with all paint-only styling
 * (colors, washes, tap annotations) omitted. Keep both builders in sync: this
 * is the exact text the auto-fit measures with TextMeasurer.
 */
internal fun mushafBlockLayoutText(group: AyahFlowGroup, fontSize: Float): AnnotatedString =
    buildAnnotatedString {
        group.ayat.forEachIndexed { ayahIndex, ayah ->
            // Inter-ayah space belongs to neither ayah (mirror of the block).
            if (ayahIndex > 0) append(" ")
            if (ayah.words.isNotEmpty()) {
                ayah.words.forEachIndexed { wordIndex, word ->
                    if (wordIndex > 0) append(" ")
                    append(sanitizeUthmanicText(word.text))
                }
            } else {
                append(sanitizeUthmanicText(ayah.textUthmani))
            }
            withStyle(SpanStyle(fontSize = (fontSize * 0.9f).sp, fontWeight = FontWeight.Bold)) {
                // Non-breaking space + ornate brackets, exactly as MushafFlowBlock.
                append("\u00A0﴿${ayah.ayah.toEasternArabicDigits()}﴾")
            }
        }
    }

/**
 * Largest mushaf font size (sp, resolved to ~0.1) at which [group]'s flow
 * block fits inside a slot of [slotWidth] × [slotHeight] — the pages-mode
 * auto-fit that replaces the user's font-size preference (which keeps ruling
 * scroll mode).
 *
 * Measures the REAL block text ([mushafBlockLayoutText]) with the exact
 * block style ([mushafBlockTextStyle]) and RTL layout. The continuous binary
 * search converges to within 0.1sp of the true maximum, so the fitted page
 * uses nearly all of its slot instead of leaving a stair-step gap.
 */
@Composable
internal fun rememberFittedMushafFontSize(
    group: AyahFlowGroup,
    quranFont: QuranFont,
    slotWidth: Dp,
    slotHeight: Dp
): Float {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // rememberTextMeasurer captures density/fontScale, but fontScale changes
    // recreate the activity (config change); keyed as insurance anyway.
    val fontScale = density.fontScale
    val baseStyle = LocalTextStyle.current

    return remember(group, quranFont, slotWidth, slotHeight, fontScale, baseStyle) {
        // The block's Text padds itself (4dp sides, 2dp top/bottom) inside the
        // slot — subtract so the layout measures the inner text area only.
        val innerWidthPx = with(density) { (slotWidth - BLOCK_H_PADDING * 2).toPx() }.roundToInt() - WIDTH_SLACK_PX
        val innerHeightPx = with(density) { (slotHeight - BLOCK_V_PADDING * 2).toPx() }.roundToInt()
        if (innerWidthPx <= 0 || innerHeightPx <= 0) return@remember MUSHAF_FIT_MIN_SP

        val targetPx = innerHeightPx * MUSHAF_FIT_SAFETY

        fun fitsAt(fontSize: Float): Boolean {
            val result = textMeasurer.measure(
                text = mushafBlockLayoutText(group, fontSize),
                style = mushafBlockTextStyle(baseStyle, fontSize, quranFont),
                constraints = Constraints(maxWidth = innerWidthPx),
                // The block always renders RTL regardless of the app locale.
                layoutDirection = LayoutDirection.Rtl
            )
            return result.size.height <= targetPx
        }

        when {
            // Even the floor overflows (extreme small slot) — clamp to it.
            !fitsAt(MUSHAF_FIT_MIN_SP) -> MUSHAF_FIT_MIN_SP
            fitsAt(MUSHAF_FIT_MAX_SP) -> MUSHAF_FIT_MAX_SP
            else -> {
                // Invariant: low fits, high does not. Narrow to ~0.1sp steps.
                var low = MUSHAF_FIT_MIN_SP
                var high = MUSHAF_FIT_MAX_SP
                while (high - low > FIT_RESOLUTION_SP) {
                    val mid = (low + high) / 2f
                    if (fitsAt(mid)) low = mid else high = mid
                }
                low
            }
        }
    }
}

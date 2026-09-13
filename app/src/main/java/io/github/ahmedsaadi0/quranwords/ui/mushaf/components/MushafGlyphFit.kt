package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** Smallest printable QCF size before a line is declared overflowing. */
internal const val MIN_GLYPH_SP = 13f

/** First fit attempt: print-size glyphs, shrinking only on overflow. */
internal const val START_GLYPH_SP = 30f

/** Upper clamp so user font scaling can never blow out the 15-line grid. */
internal const val MAX_GLYPH_SP = 40f

/**
 * QCF glyphs carry tall ascenders/diacritics: roomy lines keep baselines
 * steady and diacritics unclipped. Shared by text lines and the basmalah.
 */
internal const val GLYPH_LINE_HEIGHT_FACTOR = 1.72f

/**
 * Shrinks [START_GLYPH_SP] → [MIN_GLYPH_SP] until [text] fits [maxPx].
 * Measures the exact string that gets rendered, so measurement == rendering.
 */
internal fun fitGlyphSize(
    measurer: TextMeasurer,
    base: TextStyle,
    text: AnnotatedString,
    maxPx: Float = Float.MAX_VALUE
): Float {
    var size = START_GLYPH_SP
    while (size > MIN_GLYPH_SP &&
        measurer.measure(text, base.copy(fontSize = size.sp)).size.width > maxPx
    ) {
        size -= 1f
    }
    return size
}

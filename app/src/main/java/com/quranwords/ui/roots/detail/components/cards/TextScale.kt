package com.quranwords.ui.roots.detail.components.cards

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Reference preference value at which card text renders at theme size.
 * Matches the DataStore default and `SurahDetailUiState.fontSize` default.
 */
internal const val BASE_FONT_SIZE = 24f

/** Proportional factor for the shared font-size preference (1.0 at default). */
internal fun fontScale(fontSize: Float): Float = fontSize / BASE_FONT_SIZE

/**
 * Returns this style with font size and line height scaled by the shared
 * font-size preference. Theme proportions are preserved, so at the default
 * preference the rendering is identical to the unscaled style.
 */
internal fun TextStyle.scaled(fontSize: Float): TextStyle {
    val scale = fontScale(fontSize)
    return copy(
        fontSize = (this.fontSize.value * scale).sp,
        lineHeight = (lineHeight.value * scale).sp
    )
}

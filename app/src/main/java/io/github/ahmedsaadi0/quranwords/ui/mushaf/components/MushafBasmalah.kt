package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette

/**
 * Single U+FDFD ligature — the only codepoint `bismillah.ttf` draws.
 * Built from the codepoint int so the source stays pure ASCII.
 */
private const val BASMALAH_CODEPOINT = 0xFDFD
val BASMALAH_LIGATURE: String = Character.toString(BASMALAH_CODEPOINT)

/**
 * The basmalah preface rendered in the dedicated basmalah font (official
 * quran.com `bismillah.ttf` via `MushafPageData.basmalahFont`).
 *
 * One centered glyph, fitted to its slot exactly like text lines
 * ([fitGlyphSize]) so the 15-line rhythm holds. Inert by design — no
 * morphology lives on this line, so taps toggle chrome like ayah markers.
 */
@Composable
fun MushafBasmalah(
    pageNumber: Int,
    lineNumber: Int,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    fontScale: Float = 1f,
    onBackgroundTapped: () -> Unit = {}
) {
    val palette = rememberMushafPalette()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = LocalTextStyle.current
    val text = remember { AnnotatedString(BASMALAH_LIGATURE) }
    val fallbackDesc = stringResource(R.string.mushaf_basmalah)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxPx = with(density) { maxWidth.toPx() }
        val fittedSp = remember(text, maxPx, fontScale, fontFamily) {
            (fitGlyphSize(measurer, baseStyle.copy(fontFamily = fontFamily), text, maxPx) *
                fontScale).coerceIn(MIN_GLYPH_SP, MAX_GLYPH_SP)
        }
        Text(
            text = text,
            style = baseStyle.copy(
                fontFamily = fontFamily,
                fontSize = fittedSp.sp,
                color = palette.ink,
                lineHeight = (fittedSp * GLYPH_LINE_HEIGHT_FACTOR).sp,
                textAlign = TextAlign.Center,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mushaf_basmalah_${pageNumber}_$lineNumber")
                .semantics { contentDescription = fallbackDesc }
                .pointerInput(pageNumber, lineNumber) {
                    detectTapGestures(onTap = { onBackgroundTapped() })
                }
        )
    }
}

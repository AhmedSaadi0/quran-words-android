package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.domain.model.MushafLine
import io.github.ahmedsaadi0.quranwords.ui.theme.MushafTokens
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette

const val MUSHAF_WORD_ANNOTATION_TAG = "wordAyahId"

/** Marks ayah-end marker spans, which stay inert (toggle chrome on tap). */
const val MUSHAF_MARKER_ANNOTATION_TAG = "ayahMarker"

/** Forgiveness around the laid-out glyph run before a tap counts as margin. */
private const val TAP_SLOP_DP = 8

/**
 * One printed text line as a single [Text]: words share one layout, so the
 * measured width IS the rendered width and [TextAlign.Justify] stretches
 * inter-word spaces exactly like print.
 *
 * Short lines (≤ [MushafTokens.SHORT_LINE_WORD_THRESHOLD] words: surah-final
 * lines, opening pages) fall back to [TextAlign.Center] — justifying them
 * would spray a few words edge-to-edge.
 *
 * Taps resolve via `onTextLayout` + `detectTapGestures` (no deprecated
 * `ClickableText`). `detectTapGestures` observes taps only — horizontal drag
 * deltas pass straight through to the `HorizontalPager`, so page flips stay
 * 100% fluid.
 *
 * Disambiguation order: exact word glyph → morphology; inter-word whitespace
 * → nearest adjacent word (a finger landing on a gap meant the word);
 * ayah-end marker → [onBackgroundTapped] (markers have no morphology);
 * anything outside the laid-out glyph run (margins, slot padding) →
 * [onBackgroundTapped] so chrome stays toggleable.
 */
@Composable
fun MushafGlyphLine(
    pageNumber: Int,
    line: MushafLine,
    fontFamily: FontFamily,
    onWordTapped: (wordAyahId: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedWordAyahId: Int? = null,
    fontScale: Float = 1f,
    onBackgroundTapped: () -> Unit = {}
) {
    val palette = rememberMushafPalette()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = LocalTextStyle.current

    val annotated = remember(line, selectedWordAyahId, palette) {
        buildGlyphAnnotatedString(line, selectedWordAyahId, palette.inkFaded, palette.selectedWord)
    }
    val textAlign = remember(line.words.size) {
        if (line.words.size <= MushafTokens.SHORT_LINE_WORD_THRESHOLD) {
            TextAlign.Center
        } else {
            TextAlign.Justify
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Width is resolved by the parent slot (fillMaxWidth); fitting measures
    // against the incoming max width through the measurer constraint below.
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val maxPx = with(density) { maxWidth.toPx() }
        val fittedSp = remember(annotated, maxPx, fontScale, fontFamily) {
            (fitGlyphSize(measurer, baseStyle.copy(fontFamily = fontFamily), annotated, maxPx) *
                fontScale).coerceIn(MIN_GLYPH_SP, MAX_GLYPH_SP)
        }
        val style = baseStyle.copy(
            // rendered size: fitted base × user scale
            fontFamily = fontFamily,
            fontSize = fittedSp.sp,
            color = palette.ink,
            lineHeight = (fittedSp * GLYPH_LINE_HEIGHT_FACTOR).sp,
            textAlign = textAlign,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
        val lineDesc = stringResource(
            R.string.mushaf_line_desc,
            line.lineNumber,
            line.words.size
        )
        Text(
            text = annotated,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mushaf_glyphs_${pageNumber}_${line.lineNumber}")
                .semantics {
                    contentDescription = lineDesc
                }
                .pointerInput(pageNumber, line.lineNumber) {
                    detectTapGestures(
                        onTap = { offset ->
                            val tapped = layoutResult?.let { layout ->
                                resolveWordTap(layout, annotated, offset, density)
                            }
                            if (tapped != null) onWordTapped(tapped) else onBackgroundTapped()
                        }
                    )
                }
        )
    }
}

private fun buildGlyphAnnotatedString(
    line: MushafLine,
    selectedWordAyahId: Int?,
    markerColor: Color,
    selectedColor: Color
): AnnotatedString = buildAnnotatedString {
    line.words.forEachIndexed { index, word ->
        if (index > 0) append(" ")
        val start = length
        append(word.codeV2)
        val end = length
        if (word.wordAyahId != null) {
            addStringAnnotation(
                tag = MUSHAF_WORD_ANNOTATION_TAG,
                annotation = word.wordAyahId.toString(),
                start = start,
                end = end
            )
            if (word.wordAyahId == selectedWordAyahId) {
                addStyle(SpanStyle(background = selectedColor), start, end)
            }
        } else {
            // Ayah-end markers: visible hierarchy (faded ink), inert — their
            // own annotation routes taps to chrome toggling, never to a
            // neighboring word. Same size as words: fit stays exact.
            addStringAnnotation(
                tag = MUSHAF_MARKER_ANNOTATION_TAG,
                annotation = "1",
                start = start,
                end = end
            )
            addStyle(SpanStyle(color = markerColor), start, end)
        }
    }
}

/**
 * Ordered tap resolution for one glyph line (see [MushafGlyphLine] KDoc).
 * Pure over [layout] + [annotated]: word identity, never glyph position.
 */
private fun resolveWordTap(
    layout: TextLayoutResult,
    annotated: AnnotatedString,
    tap: Offset,
    density: Density
): Int? {
    if (annotated.isEmpty()) return null
    val lastIndex = annotated.length - 1
    val charOffset = layout.getOffsetForPosition(tap).coerceIn(0, lastIndex)
    // Margin taps outside the laid-out glyph run fall through to chrome.
    val slopPx = with(density) { TAP_SLOP_DP.dp.toPx() }
    val box = layout.getBoundingBox(charOffset)
    if (tap.x < box.left - slopPx || tap.x > box.right + slopPx ||
        tap.y < box.top - slopPx || tap.y > box.bottom + slopPx
    ) {
        return null
    }
    annotated.wordAt(charOffset)?.let { return it }
    if (annotated.isMarkerAt(charOffset)) return null
    // Inter-word whitespace: the finger meant the adjacent word.
    if (charOffset > 0) annotated.wordAt(charOffset - 1)?.let { return it }
    if (charOffset < lastIndex) annotated.wordAt(charOffset + 1)?.let { return it }
    return null
}

private fun AnnotatedString.wordAt(offset: Int): Int? =
    getStringAnnotations(
        tag = MUSHAF_WORD_ANNOTATION_TAG,
        start = offset,
        end = offset
    ).firstOrNull()?.item?.toIntOrNull()

private fun AnnotatedString.isMarkerAt(offset: Int): Boolean =
    getStringAnnotations(
        tag = MUSHAF_MARKER_ANNOTATION_TAG,
        start = offset,
        end = offset
    ).isNotEmpty()

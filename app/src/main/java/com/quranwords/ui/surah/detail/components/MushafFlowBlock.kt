package com.quranwords.ui.surah.detail.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.quranwords.core.util.sanitizeUthmanicText
import com.quranwords.core.util.toEasternArabicDigits
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.theme.AppMotion
import com.quranwords.ui.theme.QuranFont

private const val FLOW_WORD_TAG = "word_id"
internal const val FLOW_AYAH_TAG = "ayah_num"

/**
 * Continuous-flow Mushaf block: every ayah of [group] concatenated into ONE
 * [Text] with full justification forming a solid rectangular block.
 */
@Composable
fun MushafFlowBlock(
    group: AyahFlowGroup,
    fontSize: Float,
    quranFont: QuranFont,
    isSelectionMode: Boolean,
    selectedAyahs: Set<Int>,
    onWordClick: (WordToken, Ayah) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEnterSelection: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bookmarkedAyahs: Set<Int> = emptySet(),
    pulseAyah: Int? = null,
    onPulseDone: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val endMarkerColor = MaterialTheme.colorScheme.primary
    val selectionBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    // Bookmark wash: tertiary maps to the QuranGold/Amber family in both
    // light (NaturalAmberContainer) and dark (NaturalAmberContainerDark)
    // schemes (Theme.kt), so no new token is needed. Lower alpha than
    // selectionBg keeps selection the dominant action signal.
    val bookmarkBg = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    val bookmarkMarkerColor = MaterialTheme.colorScheme.tertiary
    // Deep-link pulse base (alpha is animated, so no baked alpha here).
    // Same hue family as selection but the motion — not a new color — is the
    // "here you are" signal. Drawn as an overlay (see drawWithContent), so
    // the AnnotatedString below stays static: zero text relayout per frame.
    val pulseBase = MaterialTheme.colorScheme.primaryContainer
    val pulseAlpha = remember { Animatable(0f) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val ayahByNumber = remember(group) { group.ayat.associateBy { it.ayah } }

    val annotated = remember(
        group,
        selectedAyahs,
        bookmarkedAyahs,
        fontSize,
        endMarkerColor,
        selectionBg,
        bookmarkBg,
        bookmarkMarkerColor,
        quranFont
    ) {
        // NOTE: the character content of this builder is mirrored by
        // mushafBlockLayoutText in MushafFontSizeFit.kt (pages-mode auto-fit
        // measures this exact text with TextMeasurer). Keep both in sync.
        buildAnnotatedString {
            group.ayat.forEachIndexed { ayahIndex, ayah ->
                // Inter-ayah space belongs to neither range: highlights stay isolated.
                if (ayahIndex > 0) append(" ")
                val ayahStart = length
                val isSelected = ayah.ayah in selectedAyahs
                val isBookmarked = ayah.ayah in bookmarkedAyahs
                if (ayah.words.isNotEmpty()) {
                    ayah.words.forEachIndexed { wordIndex, word ->
                        if (wordIndex > 0) append(" ")
                        val wordStart = length
                        append(sanitizeUthmanicText(word.text))
                        addStringAnnotation(FLOW_WORD_TAG, word.wordAyahId.toString(), wordStart, length)
                    }
                } else {
                    append(sanitizeUthmanicText(ayah.textUthmani))
                }
                withStyle(
                    SpanStyle(
                        // Combined state: selection owns the body wash while the
                        // end-marker carries the bookmark signal (no muddy blend).
                        color = if (isBookmarked) bookmarkMarkerColor else endMarkerColor,
                        fontSize = (fontSize * 0.9f).sp,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    // استخدام مسافة غير قابلة للكسر (\u00A0) حتى لا ينفصل رقم الآية منفرداً في سطر مستقل.
                    // الأقواس المزخرفة ﴿ ﴾ هي عرف التطبيق الموحد (البحث/العلامات/النسخ) وتُعرض
                    // مؤطرة في كل الخطوط الأربعة، بما فيها خط مصحف المدينة.
                    append("\u00A0﴿${ayah.ayah.toEasternArabicDigits()}﴾")
                }
                addStringAnnotation(FLOW_AYAH_TAG, ayah.ayah.toString(), ayahStart, length)
                if (isSelected) {
                    addStyle(SpanStyle(background = selectionBg), ayahStart, length)
                } else if (isBookmarked) {
                    addStyle(SpanStyle(background = bookmarkBg), ayahStart, length)
                }
            }
        }
    }

    // Canonical block style — also the reference style the last-line
    // justification re-lays-out tokens with, so measure == render.
    val blockStyle = mushafBlockTextStyle(LocalTextStyle.current, fontSize, quranFont)
    val textMeasurer = rememberTextMeasurer()
    // Word-gap justification of the last line (uniform rectangle). Computed
    // once per layout/annotation change; drawing applies per-token shifts
    // with no text relayout. Null keeps the centered-last-line fallback.
    val justification = remember(layoutResult, annotated, blockStyle) {
        val result = layoutResult
        if (result == null) null
        else computeLastLineJustification(annotated, result, blockStyle, textMeasurer)
    }
    // pointerInput's keys never change with the layout, so its lambda would
    // capture a stale justification — read it fresh per tap instead.
    val currentJustification by rememberUpdatedState(justification)

    // Line bands covering the pulse target, split for the centered last line.
    // Pure geometry over the static text: no span changes, no relayout.
    val pulseGeo = remember(layoutResult, annotated, pulseAyah, justification) {
        val target = pulseAyah
        val result = layoutResult
        if (target == null || result == null) null
        else result.pulseGeometry(annotated, target, justification)
    }

    // Two soft fades (~1s total). Keys cover late layout delivery; the effect
    // never touches its own keys, so no restart loop is possible. Blocks not
    // owning the target return early without firing onPulseDone.
    LaunchedEffect(pulseAyah, layoutResult) {
        val geo = pulseGeo
        if (pulseAyah == null || geo == null || geo.isEmpty) return@LaunchedEffect
        repeat(2) {
            pulseAlpha.animateTo(0.5f, tween(200, easing = AppMotion.EasingStandard))
            delay(100)
            pulseAlpha.animateTo(0.12f, tween(180, easing = AppMotion.EasingExit))
        }
        pulseAlpha.snapTo(0f)
        onPulseDone()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(isSelectionMode, group.key, group.firstAyahIndex) {
                detectTapGestures(
                    onTap = { offset ->
                        val result = layoutResult ?: return@detectTapGestures
                        // Last line is drawn shifted (justified per-token or
                        // centered fallback): re-map the visual tap back to
                        // layout coordinates.
                        val position = result.remapForLastLine(currentJustification, offset)
                        val ayahNum = result.ayahAt(annotated, position) ?: return@detectTapGestures
                        if (isSelectionMode) {
                            onToggleSelection(ayahNum)
                        } else {
                            val word = result.wordAt(annotated, ayahByNumber, position)
                            if (word != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onWordClick(word, ayahByNumber.getValue(ayahNum))
                            }
                        }
                    },
                    onLongPress = { offset ->
                        val result = layoutResult ?: return@detectTapGestures
                        val position = result.remapForLastLine(currentJustification, offset)
                        val ayahNum = result.ayahAt(annotated, position) ?: return@detectTapGestures
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnterSelection(ayahNum)
                    }
                )
            }
            .testTag("mushaf_block_${group.key}_${group.firstAyahIndex}")
            .semantics(mergeDescendants = true) {
                val firstNum = group.ayat.firstOrNull()?.ayah?.toEasternArabicDigits().orEmpty()
                val lastNum = group.ayat.lastOrNull()?.ayah?.toEasternArabicDigits().orEmpty()
                val range = "الآيات $firstNum–$lastNum"
                contentDescription = if (group.pageNumber != null) {
                    "صفحة ${group.pageNumber.toEasternArabicDigits()}، $range"
                } else {
                    range
                }
                customActions = group.ayat.flatMap { ayah ->
                    val toggleLabel = if (ayah.ayah in bookmarkedAyahs) {
                        "الآية ${ayah.ayah.toEasternArabicDigits()} (محفوظة)"
                    } else {
                        "الآية ${ayah.ayah.toEasternArabicDigits()}"
                    }
                    val toggle = CustomAccessibilityAction(toggleLabel) {
                        if (isSelectionMode) onToggleSelection(ayah.ayah)
                        else onEnterSelection(ayah.ayah)
                        true
                    }
                    // TalkBack has no long-press: expose range extension (the
                    // Screen routes onEnterSelection to RangeSelect in this mode).
                    if (isSelectionMode) {
                        listOf(
                            toggle,
                            CustomAccessibilityAction(
                                "تحديد المدى حتى الآية ${ayah.ayah.toEasternArabicDigits()}"
                            ) {
                                onEnterSelection(ayah.ayah)
                                true
                            }
                        )
                    } else {
                        listOf(toggle)
                    }
                }
            }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = annotated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .drawWithContent {
                        // Pulse overlay (behind text): per-line rounded bands over
                        // the target ayah with GPU-animated alpha. The bands are
                        // pre-split exactly like the content below (justified
                        // per-token or centered two-pass) so the last line stays
                        // aligned. Zero text relayout: the AnnotatedString never
                        // changes during the pulse.
                        // (If the ayah also carries a static span wash, that
                        // opaque wash covers the overlay there — but such an
                        // ayah is already marked, so no cue is lost.)
                        val geo = pulseGeo
                        val alpha = pulseAlpha.value
                        if (geo != null && !geo.isEmpty && alpha > 0.01f) {
                            val color = pulseBase.copy(alpha = alpha)
                            val radius = CornerRadius(6.dp.toPx())
                            val expandX = 3.dp.toPx()
                            fun drawBand(rect: Rect) {
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(rect.left - expandX, rect.top),
                                    size = Size(rect.width + expandX * 2f, rect.height),
                                    cornerRadius = radius
                                )
                            }
                            if (justification != null) {
                                // Justified rectangle: bands above the last line
                                // live in original coordinates; the last line's
                                // bands are already re-positioned per token.
                                clipRect(top = 0f, bottom = justification.lineTop) {
                                    geo.above.forEach(::drawBand)
                                }
                                geo.lastLine.forEach(::drawBand)
                            } else if (geo.centered) {
                                clipRect(top = 0f, bottom = geo.lastLineTop) {
                                    geo.above.forEach(::drawBand)
                                }
                                clipRect(top = geo.lastLineTop, bottom = size.height) {
                                    translate(left = -geo.emptySpace / 2f) {
                                        geo.lastLine.forEach(::drawBand)
                                    }
                                }
                            } else {
                                geo.above.forEach(::drawBand)
                                geo.lastLine.forEach(::drawBand)
                            }
                        }
                        // Uniform rectangle: when the last line is justified,
                        // everything above it is drawn unshifted and the last
                        // line's tokens are re-drawn at their gap-widened
                        // positions (word spacing only — glyph size/shape
                        // untouched). Otherwise the Madinah Mushaf page effect
                        // applies: Compose has no `text-align-last: center`,
                        // so the last line is drawn shifted to the center while
                        // taps are re-mapped back (see remapForCenteredLastLine).
                        val result = layoutResult
                        if (result != null && result.lineCount > 0) {
                            val just = justification
                            if (just != null) {
                                clipRect(top = 0f, bottom = just.lineTop) {
                                    this@drawWithContent.drawContent()
                                }
                                just.tokens.forEach { token ->
                                    drawText(
                                        token.layout,
                                        topLeft = Offset(token.finalLeft, just.lineTop)
                                    )
                                }
                            } else {
                                val lastLineIndex = result.lineCount - 1
                                val emptySpace = result.lastLineEmptySpace()
                                if (emptySpace > LAST_LINE_EPS_PX) {
                                    val lastLineTop = result.getLineTop(lastLineIndex)
                                    clipRect(top = 0f, bottom = lastLineTop) {
                                        this@drawWithContent.drawContent()
                                    }
                                    clipRect(top = lastLineTop, bottom = size.height) {
                                        translate(left = -emptySpace / 2f) {
                                            this@drawWithContent.drawContent()
                                        }
                                    }
                                } else {
                                    drawContent()
                                }
                            }
                        } else {
                            drawContent()
                        }
                    },
                color = MaterialTheme.colorScheme.onSurface,
                // Canonical block style (mushafBlockTextStyle): justify +
                // paragraph balancing + no font padding + the cut's leading —
                // the exact style the auto-fit and the last-line justification
                // measure with.
                style = blockStyle,
                onTextLayout = { layoutResult = it }
            )
        }
    }
}

/**
 * Overlay bands covering one ayah's characters. In the centered fallback the
 * bands are pre-split for the two-pass clip/translate drawing; in the
 * justified rectangle the last-line bands are already re-positioned with
 * their tokens.
 */
private data class PulseGeometry(
    val above: List<Rect>,
    val lastLine: List<Rect>,
    val centered: Boolean,
    val lastLineTop: Float,
    val emptySpace: Float
) {
    val isEmpty: Boolean get() = above.isEmpty() && lastLine.isEmpty()
}

/**
 * Layout-space line rects intersecting the given ayah's annotated range.
 * Each band is clipped horizontally to the ayah's own characters on that
 * line (not the full line width), so verses sharing a line never bleed into
 * each other. Returns null when the layout is empty or the ayah is not in
 * this block.
 */
private fun TextLayoutResult.pulseGeometry(
    text: AnnotatedString,
    ayahNum: Int,
    justification: LastLineJustification?
): PulseGeometry? {
    if (lineCount == 0 || text.isEmpty()) return null
    val span = text.getStringAnnotations(FLOW_AYAH_TAG, 0, text.length)
        .firstOrNull { it.item.toIntOrNull() == ayahNum } ?: return null
    val lastLineIndex = lineCount - 1
    val emptySpace = lastLineEmptySpace()
    val centered = justification == null && emptySpace > LAST_LINE_EPS_PX
    val lastLineTop = getLineTop(lastLineIndex)
    val above = mutableListOf<Rect>()
    val lastLine = mutableListOf<Rect>()
    val just = justification
    for (line in 0 until lineCount) {
        // Justified rectangle: the last line is owned by the re-positioned
        // tokens below.
        if (just != null && line == lastLineIndex) continue
        if (getLineEnd(line) <= span.start || getLineStart(line) >= span.end) continue
        // Character-range intersection: the ayah may start/end mid-line.
        // (segStart < segEnd is guaranteed by the filter above, so both box
        // lookups below use valid character indices.)
        val segStart = maxOf(getLineStart(line), span.start)
        val segEnd = minOf(getLineEnd(line), span.end)
        // Union of the segment's first/last glyph boxes. Glyph boxes — unlike
        // zero-width cursor rects — are well-defined at line boundaries in
        // RTL; min/max keeps this direction-correct.
        val firstBox = getBoundingBox(segStart)
        val lastBox = getBoundingBox(segEnd - 1)
        val lineLeft = getLineLeft(line)
        val lineRight = getLineRight(line)
        val x1 = minOf(firstBox.left, lastBox.left)
        val x2 = maxOf(firstBox.right, lastBox.right)
        // Defensive fallback: a collapsed band would be invisible, so widen
        // to the full line instead (the old acceptable behavior).
        val left = if (x2 - x1 < 1f) lineLeft else x1.coerceIn(lineLeft, lineRight)
        val right = if (x2 - x1 < 1f) lineRight else x2.coerceIn(lineLeft, lineRight)
        val rect = Rect(
            left = left,
            top = getLineTop(line),
            right = right,
            bottom = getLineBottom(line)
        )
        if (centered && line == lastLineIndex) lastLine.add(rect) else above.add(rect)
    }
    // Justified rectangle: the target ayah's tokens re-positioned in place.
    if (just != null) {
        just.tokens.filter { it.ayah == ayahNum }.forEach { token ->
            lastLine.add(
                Rect(
                    left = token.finalLeft,
                    top = just.lineTop,
                    right = token.finalRight,
                    bottom = getLineBottom(lastLineIndex)
                )
            )
        }
    }
    return PulseGeometry(above, lastLine, centered, lastLineTop, emptySpace)
}

/**
 * Resolves the tapped ayah with a ±2 char window so taps landing on the
 * inter-ayah space still map to a verse instead of dropping silently.
 */
private fun TextLayoutResult.ayahAt(text: AnnotatedString, position: Offset): Int? {
    val center = getOffsetForPosition(position).coerceIn(0, text.length)
    val start = (center - 2).coerceAtLeast(0)
    val end = (center + 2).coerceAtMost(text.length)
    return text.getStringAnnotations(FLOW_AYAH_TAG, start, end).firstOrNull()?.item?.toIntOrNull()
}

/** Exact word hit at the tap position, or null (e.g. tap on a ﴿n﴾ marker). */
private fun TextLayoutResult.wordAt(
    text: AnnotatedString,
    ayahByNumber: Map<Int, Ayah>,
    position: Offset
): WordToken? {
    val center = getOffsetForPosition(position).coerceIn(0, text.length)
    val wordId = text.getStringAnnotations(FLOW_WORD_TAG, center, center)
        .firstOrNull()?.item?.toIntOrNull() ?: return null
    val ayahNum = text.getStringAnnotations(FLOW_AYAH_TAG, center, center)
        .firstOrNull()?.item?.toIntOrNull() ?: return null
    val ayah = ayahByNumber[ayahNum] ?: return null
    return ayah.words.find { it.wordAyahId == wordId }
        ?: ayah.words.find { it.wordId == wordId }
}
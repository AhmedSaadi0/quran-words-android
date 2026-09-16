package io.github.ahmedsaadi0.quranwords.ui.surah.detail.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.core.util.sanitizeUthmanicText
import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.theme.QuranFont

private const val FLOW_WORD_TAG = "word_id"
private const val FLOW_AYAH_TAG = "ayah_num"
/** Sub-pixel tolerance (px) below which the last line counts as full. */
private const val LAST_LINE_EPS_PX = 0.5f

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
    bookmarkedAyahs: Set<Int> = emptySet()
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
                    // استخدام مسافة غير قابلة للكسر (\u00A0) حتى لا ينفصل رقم الآية منفرداً في سطر مستقل
                    append("\u00A0${ayah.ayah.toEasternArabicDigits()}")
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(isSelectionMode, group.key, group.firstAyahIndex) {
                detectTapGestures(
                    onTap = { offset ->
                        val result = layoutResult ?: return@detectTapGestures
                        // Last line is drawn centered (see drawWithContent below):
                        // re-map the visual tap back to layout coordinates.
                        val position = result.remapForCenteredLastLine(offset)
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
                        val position = result.remapForCenteredLastLine(offset)
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
                        // Madinah Mushaf page effect: Compose has no
                        // `text-align-last: center`, so the last line is drawn
                        // shifted to the center while taps are re-mapped back
                        // (see remapForCenteredLastLine).
                        val result = layoutResult
                        if (result != null && result.lineCount > 0) {
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
                        } else {
                            drawContent()
                        }
                    },
                fontSize = fontSize.sp,
                fontFamily = quranFont.fontFamily,
                lineHeight = (fontSize * quranFont.lineHeightMultiplier).sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                // 1. تفعيل المحاذاة المتساوية للأطراف
                textAlign = TextAlign.Justify,
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    // 2. تفعيل خوارزمية تكسير وتوزيع الأسطر المتناسقة (Paragraph Balancing)
                    lineBreak = LineBreak.Paragraph,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                ),
                onTextLayout = { layoutResult = it }
            )
        }
    }
}

/**
 * Unused width on the last line, in pixels. Uses the text layout width
 * ([TextLayoutResult.size]) rather than the draw-scope size so the centered
 * shift stays pixel-perfect regardless of the [Text] padding.
 */
private fun TextLayoutResult.lastLineEmptySpace(): Float {
    if (lineCount == 0) return 0f
    // getLineWidth() exists only on newer Compose; this BOM (2024.09.00)
    // provides getLineLeft()/getLineRight() — equivalent width.
    val last = lineCount - 1
    return size.width - (getLineRight(last) - getLineLeft(last))
}

/**
 * Re-maps a visual tap/long-press position back to text-layout coordinates.
 * The last line is drawn shifted left by `emptySpace / 2` (centering an
 * RTL start-aligned line), so taps on it must be shifted back right by the
 * same amount before querying [ayahAt]/[wordAt]. Positions above the last
 * line pass through untouched.
 */
private fun TextLayoutResult.remapForCenteredLastLine(position: Offset): Offset {
    if (lineCount == 0) return position
    val lastLineIndex = lineCount - 1
    val emptySpace = lastLineEmptySpace()
    if (emptySpace <= LAST_LINE_EPS_PX) return position
    if (position.y < getLineTop(lastLineIndex)) return position
    return position.copy(x = position.x + emptySpace / 2f)
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
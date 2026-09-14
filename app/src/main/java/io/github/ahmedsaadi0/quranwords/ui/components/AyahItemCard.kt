package io.github.ahmedsaadi0.quranwords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken

private const val WORD_ANNOTATION_TAG = "word_id"

/**
 * Continuous fluid verse: a single [Text] flow per ayah — no FlowRow boxes,
 * no per-word chips, no dots, no card chrome. Short verses (<= 4 words) are
 * centered Mushaf-style; longer verses are start-aligned (natural RTL).
 *
 * Single deterministic gesture handler: normal-mode word tap resolves via
 * [TextLayoutResult], long-press enters selection; in selection mode any tap
 * toggles the whole ayah.
 */
@Composable
fun AyahItemCard(
    ayah: Ayah,
    fontSize: Float,
    onWordClick: (WordToken) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    onEnterSelectionMode: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val endMarkerColor = MaterialTheme.colorScheme.tertiary
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotated = remember(ayah, fontSize, endMarkerColor) {
        buildAnnotatedString {
            if (ayah.words.isNotEmpty()) {
                ayah.words.forEachIndexed { index, word ->
                    if (index > 0) append(" ")
                    val start = length
                    append(word.text)
                    addStringAnnotation(
                        tag = WORD_ANNOTATION_TAG,
                        annotation = word.wordAyahId.toString(),
                        start = start,
                        end = length
                    )
                }
            } else {
                append(ayah.textUthmani)
            }
            withStyle(
                SpanStyle(
                    color = endMarkerColor,
                    fontSize = (fontSize * 0.9f).sp,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(" ﴿${ayah.ayah.toEasternArabicDigits()}﴾")
            }
        }
    }

    val wordCount = if (ayah.words.isNotEmpty()) ayah.words.size else ayah.wordCount
    val textAlign = if (wordCount <= 4) TextAlign.Center else TextAlign.Start

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.background
            )
            .pointerInput(isSelectionMode, ayah.ayah) {
                detectTapGestures(
                    onTap = { offset ->
                        if (isSelectionMode) {
                            onToggleSelection?.invoke()
                        } else {
                            val result = layoutResult ?: return@detectTapGestures
                            val textOffset = result.getOffsetForPosition(offset)
                            val annotation = annotated.getStringAnnotations(
                                tag = WORD_ANNOTATION_TAG,
                                start = textOffset,
                                end = textOffset
                            ).firstOrNull()
                            val wordId = annotation?.item?.toIntOrNull()
                            val word = ayah.words.find { it.wordAyahId == wordId }
                                ?: ayah.words.find { it.wordId == wordId }
                            if (word != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onWordClick(word)
                            }
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!isSelectionMode) {
                            onEnterSelectionMode?.invoke()
                        } else {
                            onToggleSelection?.invoke()
                        }
                    }
                )
            }
            .testTag("ayah_card_${ayah.ayah}")
            .semantics(mergeDescendants = true) {
                contentDescription = "الآية ${ayah.ayah} من السورة ${ayah.surah}"
                customActions = ayah.words.map { word ->
                    CustomAccessibilityAction("كلمة: ${word.text}") {
                        onWordClick(word)
                        true
                    }
                }
            }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = annotated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 2.4f).sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = textAlign,
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
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

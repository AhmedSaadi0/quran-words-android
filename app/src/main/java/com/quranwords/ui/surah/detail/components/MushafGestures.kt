package com.quranwords.ui.surah.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import com.quranwords.core.util.AyahRef
import com.quranwords.domain.model.Ayah

/** Stable gesture lambdas; inputs are read fresh so layout-only keys stay put. */
internal data class MushafGestures(
    val onTap: (Offset) -> Unit,
    val onLongPress: (Offset) -> Unit
)

private data class GestureInputs(
    val isSelectionMode: Boolean,
    val activeWordAction: WordActionTarget?,
    val wordInteraction: WordInteractionCallbacks,
    val layoutResult: TextLayoutResult?,
    val annotated: AnnotatedString,
    val justification: LastLineJustification?,
    val ayahByNumber: Map<Int, Ayah>,
    val group: AyahFlowGroup,
    val blockKey: String,
    val outerMarginSlopPx: Float,
    val wordRectOffset: Offset,
    val onToggleSelection: (AyahRef) -> Unit,
    val onEnterSelection: (AyahRef) -> Unit,
    val haptic: HapticFeedback
)

@Composable
internal fun rememberMushafGestures(
    isSelectionMode: Boolean,
    activeWordAction: WordActionTarget?,
    wordInteraction: WordInteractionCallbacks,
    layoutResult: TextLayoutResult?,
    annotated: AnnotatedString,
    justification: LastLineJustification?,
    ayahByNumber: Map<Int, Ayah>,
    group: AyahFlowGroup,
    blockKey: String,
    outerMarginSlopPx: Float,
    wordRectOffset: Offset,
    onToggleSelection: (AyahRef) -> Unit,
    onEnterSelection: (AyahRef) -> Unit
): MushafGestures {
    val haptic = LocalHapticFeedback.current
    val current by rememberUpdatedState(
        GestureInputs(
            isSelectionMode, activeWordAction, wordInteraction, layoutResult, annotated,
            justification, ayahByNumber, group, blockKey, outerMarginSlopPx, wordRectOffset,
            onToggleSelection, onEnterSelection, haptic
        )
    )
    return remember {
        MushafGestures(
            onTap = { offset ->
                val input = current
                when {
                    input.activeWordAction != null -> input.wordInteraction.onDismissWordAction()
                    input.isSelectionMode -> {
                        val result = input.layoutResult
                        if (result != null) {
                            val position = result.remapForLastLine(input.justification, offset)
                            val ayahNum = result.ayahAt(input.annotated, position)
                            val ayah = ayahNum?.let { num ->
                                input.group.ayat.firstOrNull { it.ayah == num }
                            }
                            ayah?.let { input.onToggleSelection(AyahRef(it.surah, it.ayah)) }
                        }
                    }
                    else -> input.wordInteraction.onToggleChrome()
                }
            },
            onLongPress = { offset ->
                val input = current
                val result = input.layoutResult
                if (result != null) {
                    val hit = result.mushafHitAtPoint(
                        text = input.annotated,
                        ayahByNumber = input.ayahByNumber,
                        position = offset,
                        lastLineJustification = input.justification,
                        lastLineEmptySpace = result.lastLineEmptySpace(),
                        outerMarginSlopPx = input.outerMarginSlopPx
                    )
                    if (hit != null) {
                        input.haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (hit) {
                            is MushafHit.AyahMarker -> {
                                val ayah = input.group.ayat.firstOrNull { it.ayah == hit.ayahNum }
                                ayah?.let { input.onEnterSelection(AyahRef(it.surah, it.ayah)) }
                            }
                            is MushafHit.Word -> input.wordInteraction.onWordLongPressed(
                                WordActionTarget(
                                    wordId = hit.hit.word.wordAyahId,
                                    word = hit.hit.word,
                                    ayah = hit.hit.ayah,
                                    blockKey = input.blockKey,
                                    visualRect = hit.hit.visualRect.translate(input.wordRectOffset)
                                )
                            )
                        }
                    }
                }
            }
        )
    }
}

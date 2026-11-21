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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.quranwords.R
import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.toEasternArabicDigits
import com.quranwords.ui.theme.AppMotion
import com.quranwords.ui.theme.LocalWordFocusHighlight
import com.quranwords.ui.theme.QuranFont

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
    selectedAyahs: Set<AyahRef>,
    /** Shared gesture seam from the screen; single tap no longer opens words. */
    wordInteraction: WordInteractionCallbacks,
    onToggleSelection: (AyahRef) -> Unit,
    onEnterSelection: (AyahRef) -> Unit,
    modifier: Modifier = Modifier,
    bookmarkedAyahs: Set<AyahRef> = emptySet(),
    pulseAyah: AyahRef? = null,
    onPulseDone: () -> Unit = {},
    /** Pages-mode line-spacing stretch (sp); 0 in the scrolling reader. */
    extraLineSpacingSp: Float = 0f,
    /** Non-null only on the block owning the open long-press toolbar. */
    activeWordAction: WordActionTarget? = null
) {
    val blockKey = "${group.key}_${group.firstAyahIndex}"
    val focusedWordId = activeWordAction?.takeIf { it.blockKey == blockKey }?.wordId
    val morphologyLabel = stringResource(R.string.word_action_root)
    val outerMarginSlopPx = with(LocalDensity.current) { 24.dp.toPx() }
    val wordRectOffset = with(LocalDensity.current) { Offset(4.dp.toPx(), 2.dp.toPx()) }
    val colors = MaterialTheme.colorScheme
    val pulseAlpha = remember { Animatable(0f) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val ayahByNumber = remember(group) { group.ayat.associateBy { it.ayah } }
    val wordFocusHighlight = LocalWordFocusHighlight.current
    val annotated = remember(
        group, selectedAyahs, bookmarkedAyahs, fontSize, colors.primary,
        colors.primaryContainer, colors.tertiaryContainer, colors.tertiary,
        quranFont, focusedWordId, wordFocusHighlight
    ) {
        buildMushafAnnotatedString(
            ayat = group.ayat,
            fontSize = fontSize,
            options = MushafTextOptions(
                includeAnnotations = true,
                selectedAyahs = selectedAyahs,
                bookmarkedAyahs = bookmarkedAyahs,
                focusedWordId = focusedWordId,
                endMarkerColor = colors.primary,
                bookmarkMarkerColor = colors.tertiary,
                selectionBackground = colors.primaryContainer.copy(alpha = 0.4f),
                bookmarkBackground = colors.tertiaryContainer.copy(alpha = 0.35f),
                wordFocusHighlight = wordFocusHighlight
            )
        )
    }

    val blockStyle = mushafBlockTextStyle(LocalTextStyle.current, fontSize, quranFont, extraLineSpacingSp)
    val textMeasurer = rememberTextMeasurer()
    val justification = remember(layoutResult, annotated, blockStyle) {
        val result = layoutResult
        if (result == null) null
        else computeLastLineJustification(annotated, result, blockStyle, textMeasurer)
    }
    val gestures = rememberMushafGestures(
        isSelectionMode, activeWordAction, wordInteraction, layoutResult, annotated,
        justification, ayahByNumber, group, blockKey, outerMarginSlopPx, wordRectOffset,
        onToggleSelection, onEnterSelection
    )
    val accessibilityActions = rememberMushafAccessibilityActions(
        group, selectedAyahs, bookmarkedAyahs, isSelectionMode, morphologyLabel,
        wordInteraction, onToggleSelection, onEnterSelection
    )

    val pulseGeo = remember(layoutResult, annotated, pulseAyah, justification, group) {
        val target = pulseAyah
        val result = layoutResult
        val surahMatches = target?.surahId == group.ayat.firstOrNull()?.surah
        if (target == null || result == null || !surahMatches) null
        else result.pulseGeometry(annotated, target.ayah, justification)
    }
    // Two soft fades (~1s total); keys cover late layout delivery.
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

    val firstNum = group.ayat.firstOrNull()?.ayah?.toEasternArabicDigits().orEmpty()
    val lastNum = group.ayat.lastOrNull()?.ayah?.toEasternArabicDigits().orEmpty()
    val pageNum = group.pageNumber?.toEasternArabicDigits()
    val description = if (pageNum != null) "صفحة $pageNum، الآيات $firstNum–$lastNum" else "الآيات $firstNum–$lastNum"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(isSelectionMode, group.key, group.firstAyahIndex) {
                detectTapGestures(onTap = gestures.onTap, onLongPress = gestures.onLongPress)
            }
            .testTag("mushaf_block_${group.key}_${group.firstAyahIndex}")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                customActions = accessibilityActions
            }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = annotated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .drawWithContent {
                        drawMushafPulseOverlay(pulseGeo, pulseAlpha.value, colors.primaryContainer, justification)
                        drawMushafText(layoutResult, justification) { drawContent() }
                    },
                color = colors.onSurface,
                style = blockStyle,
                onTextLayout = { layoutResult = it }
            )
        }

        val action = activeWordAction
        if (action != null && action.blockKey == blockKey) {
            WordActionPopup(
                target = action,
                onDismiss = wordInteraction.onDismissWordAction,
                onMorphology = { wordInteraction.onOpenWord(action.word, action.ayah) },
                onSelectAyah = { onEnterSelection(AyahRef(action.ayah.surah, action.ayah.ayah)) },
                onCopyWord = { wordInteraction.onCopyWord(action.word.text) }
            )
        }
    }
}

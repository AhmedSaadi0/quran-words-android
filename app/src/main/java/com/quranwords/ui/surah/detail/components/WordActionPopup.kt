package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quranwords.R
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import kotlin.math.roundToInt

/** What the long-press popup is acting on; [visualRect] is in the owning
 * block's coordinates (Text bounds + its internal padding). */
data class WordActionTarget(
    val wordId: Int,
    val word: WordToken,
    val ayah: Ayah,
    val blockKey: String,
    val visualRect: Rect
)

/** Shared gesture seam passed from the screen to both reading modes. */
data class WordInteractionCallbacks(
    /** TalkBack-only: opens the morphology sheet for this exact word/ayah. */
    val onOpenWord: (WordToken, Ayah) -> Unit,
    val onWordLongPressed: (WordActionTarget) -> Unit,
    /** Single tap outside selection mode: toggle the reading chrome. */
    val onToggleChrome: () -> Unit,
    val onDismissWordAction: () -> Unit,
    val onCopyWord: (String) -> Unit
)

/**
 * Pure popup placement: combine the anchor composable's window bounds with the
 * word's block-local rect, center horizontally, prefer above the word, flip
 * below near the top edge, and clamp inside the window (RTL/LTR safe because
 * centering + clamping are direction-agnostic). Works after vertical scrolling
 * because [anchorBounds] is re-delivered by Compose as the block moves.
 */
internal fun wordPopupPosition(
    anchorBounds: IntRect,
    visualRect: Rect,
    windowSize: IntSize,
    popupSize: IntSize,
    gapPx: Int
): IntOffset {
    if (popupSize.width <= 0 || popupSize.height <= 0) return IntOffset.Zero
    val wordLeft = anchorBounds.left + visualRect.left.roundToInt()
    val wordRight = anchorBounds.left + visualRect.right.roundToInt()
    val wordTop = anchorBounds.top + visualRect.top.roundToInt()
    val wordBottom = anchorBounds.top + visualRect.bottom.roundToInt()

    var x = (wordLeft + wordRight - popupSize.width) / 2
    x = x.coerceIn(0, (windowSize.width - popupSize.width).coerceAtLeast(0))

    var y = wordTop - popupSize.height - gapPx
    if (y < 0) y = wordBottom + gapPx
    y = y.coerceIn(0, (windowSize.height - popupSize.height).coerceAtLeast(0))
    return IntOffset(x, y)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordActionIcon(
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp).testTag(tag)
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    }
}

/** Compact floating icon toolbar for the long-pressed word. Back dismissal is
 * owned by SurahDetailScreen (priority: sheet → popup → selection), hence
 * `dismissOnBackPress = false`; outside taps dismiss here and are also
 * intercepted by the block's tap handler so they never toggle chrome. */
@Composable
fun WordActionPopup(
    target: WordActionTarget,
    onDismiss: () -> Unit,
    onMorphology: () -> Unit,
    onSelectAyah: () -> Unit,
    onCopyWord: () -> Unit
) {
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    val positionProvider = remember(target, gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = wordPopupPosition(
                anchorBounds = anchorBounds,
                visualRect = target.visualRect,
                windowSize = windowSize,
                popupSize = popupContentSize,
                gapPx = gapPx
            )
        }
    }
    val rootLabel = stringResource(R.string.word_action_root)
    val selectLabel = stringResource(R.string.word_action_select_ayah)
    val copyLabel = stringResource(R.string.word_action_copy)

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = false,
            dismissOnClickOutside = true,
            clippingEnabled = true
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                WordActionIcon(
                    icon = Icons.Filled.AccountTree,
                    label = rootLabel,
                    tag = "word_action_root"
                ) {
                    onDismiss()
                    onMorphology()
                }
                WordActionIcon(
                    icon = Icons.Filled.SelectAll,
                    label = selectLabel,
                    tag = "word_action_select_ayah"
                ) {
                    onDismiss()
                    onSelectAyah()
                }
                WordActionIcon(
                    icon = Icons.Filled.ContentCopy,
                    label = copyLabel,
                    tag = "word_action_copy"
                ) {
                    onDismiss()
                    onCopyWord()
                }
            }
        }
    }
}

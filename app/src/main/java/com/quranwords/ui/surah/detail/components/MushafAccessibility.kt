package com.quranwords.ui.surah.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.sanitizeUthmanicText
import com.quranwords.core.util.toEasternArabicDigits

private data class AccessibilityCallbacks(
    val wordInteraction: WordInteractionCallbacks,
    val onToggleSelection: (AyahRef) -> Unit,
    val onEnterSelection: (AyahRef) -> Unit
)

/**
 * TalkBack actions are rebuilt only when visible selection state changes;
 * callbacks are read fresh so unrelated recompositions keep the list stable.
 */
@Composable
internal fun rememberMushafAccessibilityActions(
    group: AyahFlowGroup,
    selectedAyahs: Set<AyahRef>,
    bookmarkedAyahs: Set<AyahRef>,
    isSelectionMode: Boolean,
    morphologyLabel: String,
    wordInteraction: WordInteractionCallbacks,
    onToggleSelection: (AyahRef) -> Unit,
    onEnterSelection: (AyahRef) -> Unit
): List<CustomAccessibilityAction> {
    val current by rememberUpdatedState(
        AccessibilityCallbacks(wordInteraction, onToggleSelection, onEnterSelection)
    )
    return remember(group, selectedAyahs, bookmarkedAyahs, isSelectionMode, morphologyLabel) {
        buildList {
            group.ayat.forEach { ayah ->
                ayah.words.forEach { word ->
                    add(
                        CustomAccessibilityAction(
                            "$morphologyLabel: ${sanitizeUthmanicText(word.text)}"
                        ) {
                            current.wordInteraction.onOpenWord(word, ayah)
                            true
                        }
                    )
                }
            }
            group.ayat.forEach { ayah ->
                val ref = AyahRef(ayah.surah, ayah.ayah)
                val saved = if (ref in bookmarkedAyahs) " (محفوظة)" else ""
                add(CustomAccessibilityAction("الآية ${ayah.ayah.toEasternArabicDigits()}$saved") {
                    if (isSelectionMode) current.onToggleSelection(ref) else current.onEnterSelection(ref)
                    true
                })
                if (isSelectionMode) {
                    add(
                        CustomAccessibilityAction(
                            "تحديد المدى حتى الآية ${ayah.ayah.toEasternArabicDigits()}"
                        ) {
                            current.onEnterSelection(ref)
                            true
                        }
                    )
                }
            }
        }
    }
}

package com.quranwords.ui.surah.detail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.quranwords.ui.surah.detail.components.WordInteractionCallbacks

/** Reader chrome/word callbacks, kept stable across recompositions (P3-2). */
@Composable
internal fun rememberReaderWordInteraction(
    onEvent: (SurahDetailEvent) -> Unit,
    isImmersive: Boolean,
    hasWordAction: Boolean
): WordInteractionCallbacks {
    val current by rememberUpdatedState(Triple(onEvent, isImmersive, hasWordAction))
    return remember {
        WordInteractionCallbacks(
            onOpenWord = { word, ayah ->
                current.first(SurahDetailEvent.SetWordAction(null))
                current.first(SurahDetailEvent.WordSelected(word, ayah))
            },
            onWordLongPressed = { target -> current.first(SurahDetailEvent.SetWordAction(target)) },
            onToggleChrome = {
                if (current.third) current.first(SurahDetailEvent.SetWordAction(null))
                else current.first(SurahDetailEvent.SetImmersive(!current.second))
            },
            onDismissWordAction = { current.first(SurahDetailEvent.SetWordAction(null)) },
            onCopyWord = { text ->
                current.first(SurahDetailEvent.SetWordAction(null))
                current.first(SurahDetailEvent.CopyWord(text))
            }
        )
    }
}

/** Back priority: sheet → popup → selection → exit, owned by the shell. */
@Composable
internal fun ReaderBackHandlers(uiState: SurahDetailUiState, onEvent: (SurahDetailEvent) -> Unit) {
    BackHandler(enabled = uiState.isSelectionMode && uiState.selectedWord == null) {
        onEvent(SurahDetailEvent.ClearSelection)
    }
    BackHandler(enabled = uiState.wordAction != null && uiState.selectedWord == null) {
        onEvent(SurahDetailEvent.SetWordAction(null))
    }
    BackHandler(enabled = uiState.selectedWord != null) { onEvent(SurahDetailEvent.DismissWord) }
}

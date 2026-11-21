package com.quranwords.ui.surah.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import com.quranwords.ui.components.MorphologyBottomSheet
import com.quranwords.ui.surah.detail.components.FontSizeSheet

/** Reader's two modal surfaces, isolated from the shell (P3-2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSheets(
    uiState: SurahDetailUiState,
    sheetState: SheetState,
    onEvent: (SurahDetailEvent) -> Unit,
    onNavigateToRootDetail: (Int) -> Unit
) {
    uiState.selectedWord?.let { word ->
        MorphologyBottomSheet(
            word = word,
            ayah = uiState.selectedWordAyah,
            sheetState = sheetState,
            onDismiss = { onEvent(SurahDetailEvent.DismissWord) },
            onNavigateToRoot = { rootId, _ -> if (rootId > 0) onNavigateToRootDetail(rootId) },
            aiExtended = uiState.aiExtended,
            aiModel = uiState.aiModel,
            aiGeneratedAt = uiState.aiGeneratedAt,
            fontSize = uiState.fontSize
        )
    }

    if (uiState.fontSheetVisible) {
        FontSizeSheet(
            fontSize = uiState.fontSize,
            quranFont = uiState.quranFont,
            pillTransparency = uiState.pillTransparency,
            readingMode = uiState.readingMode,
            onFontSizeChange = { onEvent(SurahDetailEvent.SetFontSize(it)) },
            onPillTransparencyChange = { onEvent(SurahDetailEvent.SetPillTransparency(it)) },
            onReadingModeChange = { onEvent(SurahDetailEvent.SetReadingMode(it)) },
            onDismiss = { onEvent(SurahDetailEvent.SetFontSheetVisible(false)) }
        )
    }
}

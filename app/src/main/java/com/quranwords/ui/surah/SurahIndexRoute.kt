package com.quranwords.ui.surah

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel. Collects
 * [SurahIndexViewModel.uiState] once; navigation stays screen callbacks.
 */
@Composable
fun SurahIndexRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    viewModel: SurahIndexViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    SurahIndexScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToSurahDetail = onNavigateToSurahDetail,
        onJumpToPage = { page ->
            scope.launch {
                val target = viewModel.surahForPage(page)
                if (target != null) onNavigateToSurahDetail(target.first, target.second)
            }
        },
        onJumpToSurahAyah = { s, a -> onNavigateToSurahDetail(s, a) }
    )
}
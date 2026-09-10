package io.github.ahmedsaadi0.quranwords.ui.surah

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel. Collects
 * [SurahIndexViewModel.uiState] once; navigation stays screen callbacks.
 */
@Composable
fun SurahIndexRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int) -> Unit,
    viewModel: SurahIndexViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SurahIndexScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToSurahDetail = onNavigateToSurahDetail
    )
}
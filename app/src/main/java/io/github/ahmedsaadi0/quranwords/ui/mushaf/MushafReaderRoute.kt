package io.github.ahmedsaadi0.quranwords.ui.mushaf

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful Mushaf reader entry point: the ONLY layer aware of the ViewModel.
 * Dispatches the cold-open request once per destination ([page] wins when
 * valid, otherwise resolves ([surahId], [ayah]) to its Mushaf page).
 */
@Composable
fun MushafReaderRoute(
    page: Int,
    surahId: Int,
    ayah: Int,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    viewModel: MushafReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(page, surahId, ayah) {
        if (page > 0) {
            viewModel.onEvent(MushafReaderEvent.OpenAtPage(page))
        } else {
            viewModel.onEvent(MushafReaderEvent.OpenAtAyah(surahId, ayah))
        }
    }

    MushafReadingScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToRootDetail = onNavigateToRootDetail,
        snackbarHostState = snackbarHostState
    )
}

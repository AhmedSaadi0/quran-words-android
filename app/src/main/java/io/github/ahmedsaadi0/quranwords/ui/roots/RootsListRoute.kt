package io.github.ahmedsaadi0.quranwords.ui.roots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel. Collects
 * [RootsListViewModel.uiState] once and forwards events; navigation stays
 * a screen callback.
 */
@Composable
fun RootsListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    viewModel: RootsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RootsListScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToRootDetail = onNavigateToRootDetail
    )
}
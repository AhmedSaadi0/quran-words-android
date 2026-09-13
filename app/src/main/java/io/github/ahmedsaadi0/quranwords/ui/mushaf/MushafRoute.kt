package io.github.ahmedsaadi0.quranwords.ui.mushaf

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ahmedsaadi0.quranwords.R

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel. Collects
 * state once and hosts the snackbar + morphology sheet wiring.
 */
@Composable
fun MushafRoute(
    initialPage: Int,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    viewModel: MushafViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    if (uiState.wordLookupFailed) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(context.getString(R.string.mushaf_word_needs_db))
            viewModel.onEvent(MushafEvent.WordLookupFailedShown)
        }
    }

    MushafScreen(
        uiState = uiState,
        initialPage = initialPage,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToRootDetail = onNavigateToRootDetail,
        snackbarHostState = snackbarHostState
    )
}

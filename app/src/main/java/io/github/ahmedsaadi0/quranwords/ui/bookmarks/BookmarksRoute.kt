package io.github.ahmedsaadi0.quranwords.ui.bookmarks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel. Collects
 * [BookmarksViewModel.uiState] once; navigation stays screen callbacks.
 */
@Composable
fun BookmarksRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BookmarksScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToSurahDetail = onNavigateToSurahDetail
    )
}
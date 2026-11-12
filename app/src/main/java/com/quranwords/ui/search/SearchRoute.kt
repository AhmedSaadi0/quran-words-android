package com.quranwords.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel. Collects
 * [SearchViewModel.uiState] once; navigation stays screen callbacks.
 */
@Composable
fun SearchRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val querySnapshot = uiState.query
    SearchScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToRootDetail = { id ->
            if (querySnapshot.isNotBlank()) viewModel.recordRecent(querySnapshot)
            onNavigateToRootDetail(id)
        },
        onNavigateToSurahDetail = { surahId, ayah ->
            if (querySnapshot.isNotBlank()) viewModel.recordRecent(querySnapshot)
            onNavigateToSurahDetail(surahId, ayah)
        },
        onRecordRecent = { q -> if (q.isNotBlank()) viewModel.recordRecent(q) },
        bookmarkedAyat = uiState.bookmarkedAyat,
        onToggleAyahBookmark = { s, a -> viewModel.toggleAyahBookmark(s, a) }
    )
}
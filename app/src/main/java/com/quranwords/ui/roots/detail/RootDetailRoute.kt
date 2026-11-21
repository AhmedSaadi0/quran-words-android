package com.quranwords.ui.roots.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranwords.ui.roots.detail.report.ReportMeaningViewModel
import com.quranwords.ui.roots.detail.util.rememberShareHandler

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel, coroutine
 * scope and platform context. Platform events go through [handleRootDetailEvent].
 */
@Composable
fun RootDetailRoute(
    rootId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onNavigateToWordAyat: (Int, Int) -> Unit = { _, _ -> },
    viewModel: RootDetailViewModel = hiltViewModel(),
    reportViewModel: ReportMeaningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareHandler = rememberShareHandler(snackbarHostState, scope)
    val context = LocalContext.current

    LaunchedEffect(rootId) { viewModel.onEvent(RootDetailEvent.Load(rootId)) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RootDetailEffect.NavigateToSurah ->
                    onNavigateToSurahDetail(effect.surahId, effect.ayahNum)
                is RootDetailEffect.NavigateToWordAyat ->
                    onNavigateToWordAyat(effect.rootId, effect.wordId)
            }
        }
    }

    RootDetailScreen(
        uiState = uiState,
        rootId = rootId,
        onEvent = { event ->
            handleRootDetailEvent(event, uiState, viewModel, shareHandler, context, scope)
        },
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState
    )

    if (uiState.reportDialogVisible) {
        RootDetailReportDialog(uiState, viewModel, reportViewModel, shareHandler, scope)
    }
}

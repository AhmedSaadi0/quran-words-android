package io.github.ahmedsaadi0.quranwords.ui.roots.word

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.rememberShareHandler
import kotlinx.coroutines.launch

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel, coroutine
 * scope and platform context. Collects [WordAyatViewModel.uiState] once,
 * intercepts copy/share platform events via [ShareHandler] and delegates
 * state events to the ViewModel.
 */
@Composable
fun WordAyatRoute(
    rootId: Int,
    wordId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    viewModel: WordAyatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareHandler = rememberShareHandler(snackbarHostState, scope)
    val context = LocalContext.current

    LaunchedEffect(rootId, wordId) {
        viewModel.onEvent(WordAyatEvent.Load(rootId, wordId))
    }

    fun onEvent(event: WordAyatEvent) {
        when (event) {
            WordAyatEvent.CopyAll -> scope.launch {
                val total = uiState.totalCount
                shareHandler.copyFormatted(
                    provider = { viewModel.getAllFormatted() },
                    successMessage = context.resources.getQuantityString(
                        R.plurals.copied_ayat_count, total, total
                    ),
                    emptyMessage = context.getString(R.string.no_ayat_copy)
                )
            }
            WordAyatEvent.ShareAll -> scope.launch {
                shareHandler.shareFormatted(
                    provider = { viewModel.getAllFormatted() },
                    chooserTitle = context.getString(R.string.share_ayat),
                    emptyMessage = context.getString(R.string.no_ayat_share),
                    noAppMessage = context.getString(R.string.no_share_app)
                )
            }
            else -> viewModel.onEvent(event)
        }
    }

    WordAyatScreen(
        uiState = uiState,
        onEvent = ::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToSurahDetail = onNavigateToSurahDetail,
        snackbarHostState = snackbarHostState
    )
}
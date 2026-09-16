package io.github.ahmedsaadi0.quranwords.ui.surah.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.roots.detail.util.rememberShareHandler
import io.github.ahmedsaadi0.quranwords.ui.surah.detail.components.rememberSurahCollapsingHeaderState
import kotlinx.coroutines.launch

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel, coroutine
 * scope and platform context. Owns the collapse state holder and routes
 * copy/share platform events through [ShareHandler].
 */
@Composable
fun SurahDetailRoute(
    surahId: Int,
    targetAyah: Int,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    viewModel: SurahDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.screenState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareHandler = rememberShareHandler(snackbarHostState, scope)
    val context = LocalContext.current
    val collapseState = rememberSurahCollapsingHeaderState()

    fun onEvent(event: SurahDetailEvent) {
        when (event) {
            SurahDetailEvent.CopySelection -> scope.launch {
                val formatted = viewModel.getFormattedSelection()
                if (formatted.isNotBlank()) {
                    val count = uiState.selectedAyahs.size
                    shareHandler.copyImmediate(
                        formatted,
                        context.resources.getQuantityString(R.plurals.copied_ayat_count, count, count)
                    )
                    viewModel.onEvent(SurahDetailEvent.ClearSelection)
                }
            }
            SurahDetailEvent.ShareSelection -> scope.launch {
                val formatted = viewModel.getFormattedSelection()
                if (formatted.isNotBlank()) {
                    shareHandler.shareImmediate(
                        formatted,
                        context.getString(R.string.share_ayat),
                        context.getString(R.string.no_share_app)
                    )
                    viewModel.onEvent(SurahDetailEvent.ClearSelection)
                }
            }
            else -> viewModel.onEvent(event)
        }
    }

    fun onBookmarkSelection() {
        // Count first: the VM clears the selection synchronously on bookmark.
        val count = uiState.selectedAyahs.size
        viewModel.onEvent(SurahDetailEvent.BookmarkSelection)
        if (count > 0) {
            shareHandler.showMessage(
                context.resources.getQuantityString(R.plurals.bookmarked_ayat_count, count, count)
            )
        }
    }

    SurahDetailScreen(
        uiState = uiState,
        surahId = surahId,
        targetAyah = targetAyah,
        collapseState = collapseState,
        onEvent = { event ->
            if (event == SurahDetailEvent.BookmarkSelection) onBookmarkSelection()
            else onEvent(event)
        },
        onNavigateBack = onNavigateBack,
        onNavigateToRootDetail = onNavigateToRootDetail,
        snackbarHostState = snackbarHostState
    )
}
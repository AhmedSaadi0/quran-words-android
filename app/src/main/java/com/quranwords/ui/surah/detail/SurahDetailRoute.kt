package com.quranwords.ui.surah.detail

import android.provider.Settings
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranwords.R
import com.quranwords.core.util.Result
import com.quranwords.ui.navigation.SurahEnter
import com.quranwords.ui.roots.detail.util.rememberShareHandler
import kotlinx.coroutines.launch

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel, coroutine
 * scope and platform context. Routes copy/share platform events through
 * [ShareHandler].
 */
@Composable
fun SurahDetailRoute(
    surahId: Int,
    targetAyah: Int,
    enter: SurahEnter,
    onNavigateBack: () -> Unit,
    onNavigateToAdjacentSurah: (surahId: Int, ayah: Int, fromNext: Boolean, initialImmersive: Boolean) -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    viewModel: SurahDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val barsController = rememberReaderSystemBarsController()
    val animatorScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareHandler = rememberShareHandler(snackbarHostState, scope)

    LaunchedEffect(uiState.isImmersive) { barsController.setImmersive(uiState.isImmersive) }
    DisposableEffect(barsController) {
        onDispose { barsController.restore() }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SurahDetailEffect.BookmarkSelectionSucceeded -> shareHandler.showMessage(
                    context.resources.getQuantityString(
                        R.plurals.bookmarked_ayat_count, effect.count, effect.count
                    )
                )
                is SurahDetailEffect.BookmarkSelectionFailed -> shareHandler.showMessage(effect.message)
            }
        }
    }

    fun onEvent(event: SurahDetailEvent) {
        when (event) {
            is SurahDetailEvent.CopyWord -> {
                shareHandler.copyImmediate(event.text, context.getString(R.string.copied_text))
                // The toolbar already dismissed itself and cleared the highlight.
            }
            SurahDetailEvent.CopySelection -> scope.launch {
                when (val result = viewModel.getFormattedSelection()) {
                    is Result.Success -> if (result.data.isNotBlank()) {
                        val count = uiState.selectedAyahs.size
                        shareHandler.copyImmediate(
                            result.data,
                            context.resources.getQuantityString(R.plurals.copied_ayat_count, count, count)
                        )
                        viewModel.onEvent(SurahDetailEvent.ClearSelection)
                    }
                    is Result.Error -> shareHandler.showMessage(context.getString(R.string.copy_failed))
                }
            }
            SurahDetailEvent.ShareSelection -> scope.launch {
                when (val result = viewModel.getFormattedSelection()) {
                    is Result.Success -> if (result.data.isNotBlank()) {
                        shareHandler.shareImmediate(
                            result.data,
                            context.getString(R.string.share_ayat),
                            context.getString(R.string.no_share_app)
                        )
                        viewModel.onEvent(SurahDetailEvent.ClearSelection)
                    }
                    is Result.Error -> shareHandler.showMessage(context.getString(R.string.share_failed))
                }
            }
            else -> viewModel.onEvent(event)
        }
    }

    SurahDetailScreen(
        uiState = uiState,
        surahId = surahId,
        targetAyah = targetAyah,
        enter = enter,
        animationsEnabled = animatorScale != 0f,
        onEvent = { event -> onEvent(event) },
        onNavigateBack = onNavigateBack,
        onNavigateToAdjacentSurah = onNavigateToAdjacentSurah,
        onNavigateToRootDetail = onNavigateToRootDetail,
        snackbarHostState = snackbarHostState
    )
}
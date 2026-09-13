package io.github.ahmedsaadi0.quranwords.ui.mushaf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.ui.components.MorphologyBottomSheet
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafBottomOverlay
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafPage15Line
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafPageFrame
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafPageSkeleton
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafSystemBarsEffect
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafTopOverlay
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Chrome rests visible, then retreats so the page owns the viewport. */
private const val CHROME_AUTOHIDE_MS = 2500L

private const val BACK_EXIT_WINDOW_MS = 2000L // Double-back exit window.

/**
 * Stateless Mushaf preview: forced RTL pager over [MUSHAF_DEMO_PAGES], paper
 * cards on a darker outer backdrop.
 *
 * Chrome diet: no docked TopAppBar/bottomBar steal vertical space from the
 * 15-line grid. Instead a glass-paper top bar + page chips float as overlays:
 * background taps (markers, margins, empty slots) toggle them, and they
 * auto-hide after [CHROME_AUTOHIDE_MS] unless the user is dragging pages
 * (the timer restarts on every toggle and every drag end). Hidden chrome =
 * immersive full-screen too. Back reveals first, then exits on a second
 * press within [BACK_EXIT_WINDOW_MS]. Pages load on settle; word taps open
 * [MorphologyBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafScreen(
    uiState: MushafUiState,
    initialPage: Int,
    onEvent: (MushafEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val startIndex = (MUSHAF_DEMO_PAGES.indexOf(initialPage)).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex) { MUSHAF_DEMO_PAGES.size }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    val palette = rememberMushafPalette()
    val toggleChrome = { chromeVisible = !chromeVisible }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { onEvent(MushafEvent.PageSettled(MUSHAF_DEMO_PAGES[it])) }
    }

    // Auto-hide pauses while the pager is dragged: entering a drag restarts
    // this effect and the guard skips hiding; the timer resumes on drag end.
    LaunchedEffect(chromeVisible, pagerState.isScrollInProgress) {
        if (chromeVisible && !pagerState.isScrollInProgress) {
            delay(CHROME_AUTOHIDE_MS)
            chromeVisible = false
        }
    }

    // Back reveals chrome first; a second press in-window exits. The sheet
    // keeps its own handler and still dismisses first while open.
    var lastBackPress by remember { mutableLongStateOf(0L) }
    val backHint = stringResource(R.string.mushaf_press_back_again)
    BackHandler {
        val now = SystemClock.elapsedRealtime()
        val wasHidden = !chromeVisible
        chromeVisible = true
        if (now - lastBackPress < BACK_EXIT_WINDOW_MS) {
            onNavigateBack()
        } else {
            lastBackPress = now
            if (!wasHidden) scope.launch { snackbarHostState.showSnackbar(backHint) }
        }
    }

    MushafSystemBarsEffect(immersive = !chromeVisible)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("mushaf_screen"),
            containerColor = palette.outer,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // NOTE: edge-glow suppression is intentionally absent. Verified
                // against Foundation 1.7.2 (BOM 2024.09.00): the only pager-glow
                // knob is the deprecated LocalOverscrollConfiguration, and
                // HorizontalPager exposes no overscrollEffect param. Revisit on
                // the next BOM bump instead of shipping a deprecated call.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 2
                ) { pagerIndex ->
                    val page = MUSHAF_DEMO_PAGES[pagerIndex]
                    val data = uiState.loaded[page]
                    when {
                        data != null -> MushafPageFrame(
                            pageNumber = page,
                            footerLabel = footerLabel(page, data.page.juzNumber)
                        ) {
                            MushafPage15Line(
                                data = data,
                                onWordTapped = { onEvent(MushafEvent.WordTapped(it)) },
                                selectedWordAyahId = uiState.selectedWord?.wordAyahId,
                                onBackgroundTapped = toggleChrome
                            )
                        }
                        uiState.error != null && page == uiState.currentPage -> MushafPageFrame(
                            pageNumber = page,
                            footerLabel = footerLabel(page, null)
                        ) {
                            MushafErrorBox(
                                message = uiState.error,
                                onRetry = { onEvent(MushafEvent.Retry) }
                            )
                        }
                        else -> MushafPageSkeleton(
                            pageNumber = page,
                            footerLabel = footerLabel(page, null)
                        )
                    }
                }

                MushafTopOverlay(
                    visible = chromeVisible,
                    currentPage = uiState.currentPage,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                MushafBottomOverlay(
                    visible = chromeVisible,
                    currentPage = uiState.currentPage,
                    onSelectPage = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        uiState.selectedWord?.let { word ->
            MorphologyBottomSheet(
                word = word,
                ayah = uiState.selectedWordAyah,
                sheetState = sheetState,
                onDismiss = { onEvent(MushafEvent.DismissWord) },
                onNavigateToRoot = { rootId, _ ->
                    if (rootId > 0) onNavigateToRootDetail(rootId)
                },
                aiSummary = uiState.aiSummary,
                aiModel = uiState.aiModel,
                aiGeneratedAt = uiState.aiGeneratedAt,
                isAiLoading = uiState.isAiLoading
            )
        }
    }
}

@Composable
private fun footerLabel(page: Int, juzNumber: Int?): String {
    val pagePart = stringResource(
        R.string.mushaf_page_label,
        MushafConstants.toEasternArabic(page)
    )
    return if (juzNumber != null) {
        "$pagePart • " + stringResource(R.string.juz_label, juzNumber)
    } else {
        pagePart
    }
}

@Composable
private fun MushafErrorBox(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("mushaf_error"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry, modifier = Modifier.testTag("mushaf_retry")) {
                Text(text = stringResource(R.string.mushaf_retry))
            }
        }
    }
}

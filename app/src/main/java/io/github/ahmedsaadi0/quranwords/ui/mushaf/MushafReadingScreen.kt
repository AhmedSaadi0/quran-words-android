package io.github.ahmedsaadi0.quranwords.ui.mushaf

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.SurahMetadata
import io.github.ahmedsaadi0.quranwords.ui.components.MorphologyBottomSheet
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafBottomBar
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafPage
import io.github.ahmedsaadi0.quranwords.ui.mushaf.components.MushafTopBar
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Standalone Mushaf reading screen. Quran pages always flip right-to-left
 * (forced RTL pager regardless of app locale). The pager is the position
 * authority; the ViewModel owns data. Tapping non-interactive areas toggles
 * immersive mode: in-app bars fade and system bars hide (restored on exit).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafReadingScreen(
    uiState: MushafReaderUiState,
    onEvent: (MushafReaderEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var jumpedToInitial by rememberSaveable { mutableStateOf(false) }

    // True immersive system bars. Content already draws edge-to-edge
    // app-wide, so hiding only removes bars without layout jumps.
    // Restored on every toggle-off and on dispose (back navigation).
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(uiState.isImmersive) {
        val window = (context as? Activity)?.window
        if (window == null) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        if (uiState.isImmersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val pagerState = rememberPagerState(
        initialPage = ((uiState.initialPage ?: 1) - 1).coerceAtLeast(0),
        pageCount = { uiState.totalPages.coerceAtLeast(1) }
    )

    // Cold-open jump once the VM resolves the initial page.
    LaunchedEffect(uiState.initialPage) {
        val initial = uiState.initialPage
        if (initial != null && !jumpedToInitial) {
            jumpedToInitial = true
            pagerState.scrollToPage((initial - 1).coerceIn(0, (uiState.totalPages - 1).coerceAtLeast(0)))
        }
    }

    // Safety clamp if the page count ever shrinks below the position.
    LaunchedEffect(uiState.totalPages) {
        val last = (uiState.totalPages - 1).coerceAtLeast(0)
        if (pagerState.currentPage > last) {
            pagerState.scrollToPage(last)
        }
    }

    // Pager reports settled positions; the VM preloads and tracks last-read.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { onEvent(MushafReaderEvent.PageSettled(it + 1)) }
    }

    // Surface VM errors without blocking reading.
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val currentPageUi = uiState.pages[pagerState.currentPage + 1]
    val titleSurahId = currentPageUi?.ayat?.firstOrNull()?.surah
        ?: uiState.selectedWordAyah?.surah
    val title = titleSurahId?.let { id ->
        SurahMetadata.SURAHS.firstOrNull { it.id == id }?.let { "سورة ${it.nameAr}" }
    } ?: stringResource(R.string.mushaf_title)

    val barsVisible = !uiState.isImmersive
    val barEnter = fadeIn(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
    val barExit = fadeOut(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // MX-Player overlay pattern: Layer 0 (pager canvas) is ALWAYS
        // fillMaxSize and never recomposes on bar toggles; Layer 1 (bars)
        // floats on top, so showing/hiding moves zero text pixels.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !uiState.isDbReady && uiState.isInitialLoad -> MushafDbMissing()
                    uiState.isInitialLoad -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    else -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("mushaf_pager"),
                            beyondViewportPageCount = 1
                        ) { index ->
                            val page = index + 1
                            MushafPage(
                                page = page,
                                pageUi = uiState.pages[page],
                                isLoading = uiState.loadingPages.contains(page),
                                bookmarkedAyat = uiState.bookmarkedAyat,
                                onWordClick = { word, ayah ->
                                    onEvent(MushafReaderEvent.WordTapped(word.wordAyahId, word.position))
                                },
                                onMarkerClick = { surahId, ayah ->
                                    onEvent(MushafReaderEvent.MarkerTapped(surahId, ayah))
                                },
                                onEmptyTap = { onEvent(MushafReaderEvent.PageTapped) },
                                onRetry = { onEvent(MushafReaderEvent.PageSettled(page)) }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = barsVisible,
                enter = barEnter + slideInVertically { -it },
                exit = barExit + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                MushafTopBar(
                    title = title,
                    onNavigateBack = onNavigateBack
                )
            }

            AnimatedVisibility(
                visible = barsVisible,
                enter = barEnter + slideInVertically { it },
                exit = barExit + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MushafBottomBar(
                    currentPage = pagerState.currentPage + 1,
                    totalPages = uiState.totalPages,
                    onJumpToPage = { target ->
                        scope.launch { pagerState.animateScrollToPage(target - 1) }
                    }
                )
            }
        }

        uiState.selectedWord?.let { word ->
            MorphologyBottomSheet(
                word = word,
                ayah = uiState.selectedWordAyah,
                sheetState = sheetState,
                onDismiss = { onEvent(MushafReaderEvent.DismissWord) },
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
private fun MushafDbMissing() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = stringResource(R.string.db_missing_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.db_missing_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

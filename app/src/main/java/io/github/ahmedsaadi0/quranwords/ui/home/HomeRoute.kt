package io.github.ahmedsaadi0.quranwords.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ahmedsaadi0.quranwords.domain.model.DbUpdateState
import io.github.ahmedsaadi0.quranwords.ui.appstate.DbUpdateViewModel
import io.github.ahmedsaadi0.quranwords.ui.settings.SettingsDialog
import io.github.ahmedsaadi0.quranwords.ui.settings.SettingsViewModel

/**
 * Stateful entry point: the ONLY layer aware of the ViewModels (home data,
 * update banner, settings). Collects each state once and hosts the settings
 * dialog.
 */
@Composable
fun HomeRoute(
    onNavigateToSurahIndex: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    onNavigateToRoots: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToBookmarks: () -> Unit = {},
    homeViewModel: HomeViewModel = hiltViewModel(),
    dbUpdateViewModel: DbUpdateViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val updateState by dbUpdateViewModel.state.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.isDbReady) {
        if (uiState.isDbReady) dbUpdateViewModel.checkOnce()
    }

    // DB readiness is a file check, not reactive: refresh on every resume so
    // returning from Setup after download/import shows the updated state.
    LifecycleResumeEffect(Unit) {
        homeViewModel.refreshDbStatus()
        onPauseOrDispose { }
    }

    HomeScreen(
        uiState = uiState,
        updateState = updateState,
        onOpenSettings = { showSettingsDialog = true },
        onToggleDynamicColor = settingsViewModel::toggleDynamicColor,
        onToggleDarkMode = settingsViewModel::toggleDarkMode,
        onNavigateToSurahIndex = onNavigateToSurahIndex,
        onNavigateToSurahDetail = onNavigateToSurahDetail,
        onNavigateToRoots = onNavigateToRoots,
        onNavigateToRootDetail = onNavigateToRootDetail,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToGuide = onNavigateToGuide,
        onNavigateToSetup = onNavigateToSetup,
        onNavigateToBookmarks = onNavigateToBookmarks,
        onDismissUpdate = { available ->
            when (available) {
                is DbUpdateState.UpdateAvailable -> dbUpdateViewModel.dismiss(available.info)
                else -> Unit
            }
        }
    )

    if (showSettingsDialog) {
        SettingsDialog(
            uiState = settingsState,
            onEvent = settingsViewModel::onEvent,
            onDismiss = { showSettingsDialog = false }
        )
    }
}
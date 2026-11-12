package com.quranwords.ui.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

/**
 * Global in-app update banner host (Decision #29). Rendered once above the NavHost
 * so every screen gets the prompt; owns the ActivityResultLauncher the data layer
 * starts Play's sheet through — the UI layer owns the launch, per the plan.
 */
@Composable
fun InAppUpdateHandler(
    updateFlowStarter: AppUpdateFlowLauncher,
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val updateSheetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onEvent(UpdateEvent.UpdateSheetResult(result.resultCode))
    }

    // Fresh availability check on every START: a download that finished while the
    // app was backgrounded turns into the "restart to complete" prompt immediately.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.observeUpdates()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val bannerPadding = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
        when (val banner = uiState.banner) {
            UpdateBannerState.Hidden -> Unit

            UpdateBannerState.Available -> UpdateAvailableBanner(
                onUpdateClick = {
                    scope.launch {
                        if (!updateFlowStarter.startFlexibleUpdate(updateSheetLauncher)) {
                            viewModel.onEvent(UpdateEvent.FlowNotStarted)
                        }
                    }
                },
                onDismiss = { viewModel.onEvent(UpdateEvent.Dismiss) },
                modifier = bannerPadding,
            )

            is UpdateBannerState.Downloading -> UpdateDownloadingBanner(
                percent = banner.percent,
                modifier = bannerPadding,
            )

            UpdateBannerState.RestartRequired -> UpdateRestartBanner(
                onRestart = { viewModel.onEvent(UpdateEvent.RestartApp) },
                modifier = bannerPadding,
            )

            UpdateBannerState.Failed -> UpdateFailedBanner(
                onDismiss = { viewModel.onEvent(UpdateEvent.Dismiss) },
                modifier = bannerPadding,
            )
        }
    }
}

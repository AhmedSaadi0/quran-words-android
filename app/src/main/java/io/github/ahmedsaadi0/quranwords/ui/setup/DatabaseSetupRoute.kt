package io.github.ahmedsaadi0.quranwords.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful entry point: the ONLY layer aware of the ViewModel and the
 * document-picker platform boundary.
 */
@Composable
fun DatabaseSetupRoute(
    onNavigateBack: () -> Unit,
    viewModel: DatabaseSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(SetupEvent.ImportDatabase(uri))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SetupEvent.CheckForUpdate)
    }

    DatabaseSetupScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onPickImportFile = { importPicker.launch(arrayOf("*/*")) },
        onNavigateBack = onNavigateBack
    )
}
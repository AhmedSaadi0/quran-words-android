package com.quranwords.ui.roots.detail.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.quranwords.core.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Platform boundary for copy/share actions. The caller supplies formatted text
 * or a suspend provider returning [Result]; failures are shown as distinct
 * messages instead of being confused with empty data.
 */
class ShareHandler(
    private val clipboardManager: ClipboardManager,
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    /** Immediate copy of already-formatted text. No-op when [text] is blank. */
    fun copyImmediate(text: String, successMessage: String) {
        if (text.isBlank()) return
        // One write only (P1-2): Compose's clipboard is the in-app paste source
        // and also sets the platform clip on every supported API level.
        clipboardManager.setText(AnnotatedString(text))
        scope.launch { snackbarHostState.showSnackbar(successMessage) }
    }

    /** Immediate share of already-formatted text. No-op when [text] is blank. */
    fun shareImmediate(
        text: String,
        chooserTitle: String,
        noAppMessage: String,
    ) {
        if (text.isBlank()) return
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
        }
    }

    suspend fun copyFormatted(
        provider: suspend () -> Result<String>,
        successMessage: String,
        emptyMessage: String,
        failureMessage: String,
    ) {
        when (val result = provider()) {
            is Result.Success -> if (result.data.isNotBlank()) {
                copyImmediate(result.data, successMessage)
            } else {
                snackbarHostState.showSnackbar(emptyMessage)
            }
            is Result.Error -> snackbarHostState.showSnackbar(failureMessage)
        }
    }

    suspend fun shareFormatted(
        provider: suspend () -> Result<String>,
        chooserTitle: String,
        emptyMessage: String,
        noAppMessage: String,
        failureMessage: String,
    ) {
        when (val result = provider()) {
            is Result.Success -> if (result.data.isNotBlank()) {
                shareImmediate(result.data, chooserTitle, noAppMessage)
            } else {
                snackbarHostState.showSnackbar(emptyMessage)
            }
            is Result.Error -> snackbarHostState.showSnackbar(failureMessage)
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    /** Opens an external URL with visible feedback when no browser exists. */
    fun openUrl(url: String, noBrowserMessage: String) {
        if (url.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            showMessage(noBrowserMessage)
        }
    }
}

@Composable
fun rememberShareHandler(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
): ShareHandler {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    return remember(clipboardManager, context, snackbarHostState, scope) {
        ShareHandler(clipboardManager, context, snackbarHostState, scope)
    }
}

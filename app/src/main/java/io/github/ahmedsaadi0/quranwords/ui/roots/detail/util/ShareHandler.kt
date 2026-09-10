package io.github.ahmedsaadi0.quranwords.ui.roots.detail.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Platform boundary for copy/share actions (Phase 2a).
 *
 * Centralizes the 5× duplicated pattern from RootDetailScreen:
 * `format → isNotBlank → clipboard.setText + snackbar` OR
 * `Intent(ACTION_SEND) + createChooser + catch(ActivityNotFoundException)`.
 *
 * The handler owns [ClipboardManager], [Context], [SnackbarHostState] and
 * [CoroutineScope] so tab composables only resolve user-visible strings and
 * delegate. All methods are safe to call from composition callbacks.
 *
 * Formatting stays in the ViewModel (suspend providers); this class only
 * executes the platform side-effect and shows feedback.
 */
class ShareHandler(
    private val clipboardManager: ClipboardManager,
    private val context: Context,
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    /** Immediate copy of already-formatted text. No-op when [text] is blank. */
    fun copyImmediate(
        text: String,
        successMessage: String,
    ) {
        if (text.isBlank()) return
        // Use platform ClipData for reliability across API levels; also push to
        // Compose clipboard so in-app paste stays in sync.
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("quran_words", text))
        } catch (_: Exception) {
            // Fall through to Compose clipboard below.
        }
        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
        scope.launch {
            snackbarHostState.showSnackbar(successMessage)
        }
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
            scope.launch {
                snackbarHostState.showSnackbar(noAppMessage)
            }
        }
    }

    /**
     * Suspend-provider variant for ViewModel formatters
     * (`getAllOccurrencesFormatted()` etc.).
     * Shows [emptyMessage] when provider returns blank.
     */
    suspend fun copyFormatted(
        provider: suspend () -> String,
        successMessage: String,
        emptyMessage: String,
    ) {
        val formatted = try {
            provider()
        } catch (_: Exception) {
            ""
        }
        if (formatted.isNotBlank()) {
            copyImmediate(formatted, successMessage)
        } else {
            snackbarHostState.showSnackbar(emptyMessage)
        }
    }

    /** Suspend-provider variant for share (see [copyFormatted]). */
    suspend fun shareFormatted(
        provider: suspend () -> String,
        chooserTitle: String,
        emptyMessage: String,
        noAppMessage: String,
    ) {
        val formatted = try {
            provider()
        } catch (_: Exception) {
            ""
        }
        if (formatted.isNotBlank()) {
            shareImmediate(formatted, chooserTitle, noAppMessage)
        } else {
            snackbarHostState.showSnackbar(emptyMessage)
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    /** Opens an external URL (e.g. GitHub issue pre-fill). Silent no-op when no browser. */
    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                )
            )
        } catch (_: ActivityNotFoundException) {
            // Preserve legacy silent behaviour for report dialog.
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
        ShareHandler(
            clipboardManager = clipboardManager,
            context = context,
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }
}

package com.quranwords.ui

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.ClipboardManager
import androidx.test.core.app.ApplicationProvider
import com.quranwords.core.util.Result
import com.quranwords.ui.roots.detail.util.ShareHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShareHandlerTest {

    private class CountingClipboard : ClipboardManager {
        var writes = 0
        private var clip: AnnotatedString? = null
        override fun getText(): AnnotatedString? = clip
        override fun setText(annotatedString: AnnotatedString) {
            writes++
            clip = annotatedString
        }
    }

    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    @Test
    fun copy_immediate_writes_clipboard_once() {
        val clipboard = CountingClipboard()
        val handler = ShareHandler(
            clipboardManager = clipboard,
            context = ApplicationProvider.getApplicationContext(),
            snackbarHostState = SnackbarHostState(),
            scope = scope
        )

        handler.copyImmediate("text", "Copied")

        assertEquals(1, clipboard.writes)
    }

    @Test
    fun provider_error_shows_failure_not_empty_message() {
        val host = SnackbarHostState()
        val handler = ShareHandler(
            clipboardManager = CountingClipboard(),
            context = ApplicationProvider.getApplicationContext(),
            snackbarHostState = host,
            scope = scope
        )

        kotlinx.coroutines.runBlocking {
            handler.copyFormatted(
                provider = { Result.Error("db gone") },
                successMessage = "Copied",
                emptyMessage = "Nothing to copy",
                failureMessage = "Copy failed"
            )
        }

        assertEquals("Copy failed", host.currentSnackbarData?.message)
    }

    @Test
    fun open_url_without_browser_shows_snackbar() {
        val host = SnackbarHostState()
        val noBrowserContext = object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun startActivity(intent: Intent) {
                throw ActivityNotFoundException()
            }
        }
        val handler = ShareHandler(
            clipboardManager = CountingClipboard(),
            context = noBrowserContext,
            snackbarHostState = host,
            scope = scope
        )

        handler.openUrl("https://example.com", "No browser")

        assertEquals("No browser", host.currentSnackbarData?.message)
    }
}

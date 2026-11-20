package com.quranwords.ui.surah.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Owns the Activity/window side of immersive reading (P2-3). Created by the
 * Route with platform values; the screen only receives `animationsEnabled`.
 */
class ReaderSystemBarsController(context: Context, view: View) {
    private val window: android.view.Window? = findActivity(context)?.window
    private val controller = window?.let { WindowInsetsControllerCompat(it, view) }
    private val bars = WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()

    private fun findActivity(ctx: Context): Activity? {
        var current = ctx
        while (current is ContextWrapper && current !is Activity) current = current.baseContext
        return current as? Activity
    }

    fun setImmersive(enabled: Boolean) {
        val controller = controller ?: return
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) controller.hide(bars) else controller.show(bars)
    }

    /** Always show bars when the reader leaves the screen. */
    fun restore() {
        controller?.show(bars)
    }
}

@Composable
fun rememberReaderSystemBarsController(): ReaderSystemBarsController {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) { ReaderSystemBarsController(context, view) }
}

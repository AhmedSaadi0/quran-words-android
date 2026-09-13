package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Reader immersion: system bars follow the reader chrome. Hidden chrome =
 * true full-screen (a transient edge swipe reveals bars momentarily);
 * visible chrome = bars back. Disposal always restores the bars, so no other
 * screen inherits immersion.
 *
 * Zero layout shift by construction: the host is edge-to-edge and every
 * inset is consumed manually (`contentWindowInsets = 0` + padded overlays).
 */
@Composable
fun MushafSystemBarsEffect(immersive: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    val controller = remember(view, activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    LaunchedEffect(immersive, controller) {
        if (immersive) {
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(controller) {
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

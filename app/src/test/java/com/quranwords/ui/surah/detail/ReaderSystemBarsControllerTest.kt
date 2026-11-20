package com.quranwords.ui.surah.detail

import android.app.Activity
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReaderSystemBarsControllerTest {

    @Test
    fun immersive_toggle_hides_and_restores_bars_without_crashing() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val controller = ReaderSystemBarsController(activity, View(activity))

        controller.setImmersive(true)
        controller.setImmersive(false)
        controller.restore()

        // A non-Activity context is a safe no-op (preview / tests).
        val noWindow = ReaderSystemBarsController(ApplicationProvider.getApplicationContext(), View(activity))
        noWindow.setImmersive(true)
        noWindow.restore()
    }
}

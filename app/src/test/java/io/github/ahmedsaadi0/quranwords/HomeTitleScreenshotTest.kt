package io.github.ahmedsaadi0.quranwords

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.github.ahmedsaadi0.quranwords.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HomeTitleScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun home_title_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme { Text(stringResource(R.string.home_title)) }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_title.png")
  }
}

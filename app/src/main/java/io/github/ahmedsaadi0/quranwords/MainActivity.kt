package io.github.ahmedsaadi0.quranwords

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.ahmedsaadi0.quranwords.ui.navigation.AppNavigation
import io.github.ahmedsaadi0.quranwords.ui.settings.SettingsViewModel
import io.github.ahmedsaadi0.quranwords.ui.theme.MyApplicationTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
            // Triggers recomposition on language change. The locale switch itself
            // is applied by SettingsViewModel via LanguageManager (per-app locales
            // recreate the activity automatically).
            val isDark = when (settings.darkModeSetting) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark, useDynamicColor = settings.dynamicColorEnabled) {
                // No forced LayoutDirection here: the shell follows the app locale
                // (RTL for Arabic, LTR for English). Arabic data blocks opt into
                // RTL locally at their own call sites.
                key(settings.language) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
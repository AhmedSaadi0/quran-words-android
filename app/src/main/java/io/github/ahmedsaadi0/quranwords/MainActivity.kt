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
import io.github.ahmedsaadi0.quranwords.ui.theme.MyApplicationTheme
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkModeSetting by mainViewModel.darkModeSetting.collectAsStateWithLifecycle()
            val dynamicEnabled by mainViewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
            // Triggers recomposition on language change. The locale switch itself
            // is applied by MainViewModel via LanguageManager (per-app locales
            // recreate the activity automatically).
            val language by mainViewModel.language.collectAsStateWithLifecycle()
            val isDark = when (darkModeSetting) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark, useDynamicColor = dynamicEnabled) {
                // No forced LayoutDirection here: the shell follows the app locale
                // (RTL for Arabic, LTR for English). Arabic data blocks opt into
                // RTL locally at their own call sites.
                key(language) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(mainViewModel = mainViewModel)
                    }
                }
            }
        }
    }
}

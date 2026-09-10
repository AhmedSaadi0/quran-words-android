package io.github.ahmedsaadi0.quranwords.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.AppLanguage
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium

@Composable
fun SettingsDialog(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Theme section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.theme_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_system),
                        selected = uiState.darkModeSetting == 0,
                        onClick = { onEvent(SettingsEvent.DarkModeChanged(0)) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_light),
                        selected = uiState.darkModeSetting == 1,
                        onClick = { onEvent(SettingsEvent.DarkModeChanged(1)) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_dark),
                        selected = uiState.darkModeSetting == 2,
                        onClick = { onEvent(SettingsEvent.DarkModeChanged(2)) }
                    )
                }
                // Colors section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.theme_colors),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_colors_app),
                        subLabel = stringResource(R.string.theme_colors_app_sub),
                        selected = !uiState.dynamicColorEnabled,
                        onClick = { onEvent(SettingsEvent.DynamicColorChanged(false)) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.theme_colors_system),
                        subLabel = if (Build.VERSION.SDK_INT >= 31) {
                            stringResource(R.string.theme_colors_system_sub)
                        } else {
                            stringResource(R.string.theme_colors_unsupported)
                        },
                        selected = uiState.dynamicColorEnabled,
                        enabled = Build.VERSION.SDK_INT >= 31,
                        onClick = { if (Build.VERSION.SDK_INT >= 31) onEvent(SettingsEvent.DynamicColorChanged(true)) }
                    )
                }
                // Language section: system default clears the per-app override
                // (LanguageManager applies an empty locale list), ar/en pin it.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.lang_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.lang_system),
                        selected = uiState.language == AppLanguage.SYSTEM,
                        onClick = { onEvent(SettingsEvent.LanguageChanged(AppLanguage.SYSTEM)) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.lang_arabic),
                        selected = uiState.language == AppLanguage.ARABIC,
                        onClick = { onEvent(SettingsEvent.LanguageChanged(AppLanguage.ARABIC)) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.lang_english),
                        selected = uiState.language == AppLanguage.ENGLISH,
                        onClick = { onEvent(SettingsEvent.LanguageChanged(AppLanguage.ENGLISH)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_theme_dialog")
            ) {
                Text(stringResource(R.string.common_close))
            }
        },
        shape = ShapeMedium
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    subLabel: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            subLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = if (enabled) 1f else 0.5f)
                )
            }
        }
    }
}
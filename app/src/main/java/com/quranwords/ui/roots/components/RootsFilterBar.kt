package com.quranwords.ui.roots.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.ui.roots.RootModelOption

/**
 * Model chips for the roots lexicon — one chip per AI model found in the
 * corpus (multi-select) plus an "all models" chip that clears the filter.
 *
 * Model names are Latin, so the chip label is forced LTR inside the RTL
 * shell (AGENTS §10.5).
 */
@Composable
fun RootsFilterBar(
    availableModels: List<RootModelOption>,
    selectedModels: Set<String>,
    onToggleModel: (String) -> Unit,
    onClearModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (availableModels.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedModels.isEmpty(),
            onClick = onClearModels,
            label = {
                Text(
                    text = stringResource(R.string.roots_filter_all_models),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.testTag("roots_filter_all_models")
        )
        availableModels.forEach { option ->
            FilterChip(
                selected = option.model in selectedModels,
                onClick = { onToggleModel(option.model) },
                label = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = stringResource(R.string.roots_model_chip, option.model, option.count),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                modifier = Modifier.testTag("roots_filter_model_${option.model}")
            )
        }
    }
}

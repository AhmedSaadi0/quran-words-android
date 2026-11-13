package com.quranwords.ui.roots.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.quranwords.R
import com.quranwords.ui.roots.RootsSort

/** Sort order → user-facing label. */
private val RootsSort.labelRes: Int
    get() = when (this) {
        RootsSort.Default -> R.string.roots_sort_default
        RootsSort.ByModel -> R.string.roots_sort_model
        RootsSort.ByOccurrences -> R.string.roots_sort_occurrences
        RootsSort.ByMasadir -> R.string.roots_sort_masadir
        RootsSort.ByDerivatives -> R.string.roots_sort_derivatives
        RootsSort.Alphabetical -> R.string.roots_sort_alphabetical
    }

/**
 * Sort menu for the roots lexicon — lives in the top bar so the list keeps
 * the full width for the chips.
 */
@Composable
fun RootsSortMenu(
    current: RootsSort,
    onSelect: (RootsSort) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("roots_sort_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = stringResource(R.string.roots_sort_cd)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RootsSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes)) },
                    onClick = {
                        onSelect(sort)
                        expanded = false
                    },
                    leadingIcon = {
                        if (sort == current) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.testTag("roots_sort_item_${sort.name}")
                )
            }
        }
    }
}

package com.quranwords.ui.setup

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.quranwords.R

@Composable
internal fun SetupImportDialog(
    onDismiss: () -> Unit,
    onPickFile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.db_import_dialog_title), fontWeight = FontWeight.Bold) },
        text = { Text(text = stringResource(R.string.db_import_dialog_body), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onPickFile() }, modifier = Modifier.testTag("confirm_import_button")) {
                Text(stringResource(R.string.db_pick_file))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cd_cancel)) } }
    )
}

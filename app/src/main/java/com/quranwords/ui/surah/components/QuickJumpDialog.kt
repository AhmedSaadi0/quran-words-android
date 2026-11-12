package com.quranwords.ui.surah.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.SurahMetadata

/**
 * Quick-jump dialog (Full Plan v2, D3): jump by page (1..604) or by
 * surah:ayah (validated against metadata ayah counts).
 */
@Composable
fun QuickJumpDialog(
    onJumpToPage: (Int) -> Unit,
    onJumpToSurahAyah: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pageText by remember { mutableStateOf("") }
    var surahText by remember { mutableStateOf("") }
    var ayahText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val page = pageText.trim().toIntOrNull()
    val surahId = surahText.trim().toIntOrNull()
    val ayahNum = ayahText.trim().toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.quick_jump_title), fontWeight = FontWeight.Bold) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.quick_jump_cancel)) }
        },
        text = {
            Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it; error = null },
                    label = { Text(stringResource(R.string.quick_jump_page_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("quick_jump_page")
                )
                Button(
                    onClick = {
                        if (page != null && page in 1..604) {
                            onJumpToPage(page)
                        } else {
                            error = "page"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("quick_jump_go_page")
                ) {
                    Text(stringResource(R.string.quick_jump_go))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = surahText,
                        onValueChange = { surahText = it; error = null },
                        label = { Text(stringResource(R.string.quick_jump_surah_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("quick_jump_surah")
                    )
                    OutlinedTextField(
                        value = ayahText,
                        onValueChange = { ayahText = it; error = null },
                        label = { Text(stringResource(R.string.quick_jump_ayah_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("quick_jump_ayah")
                    )
                }
                Button(
                    onClick = {
                        val meta = surahId?.let { SurahMetadata.SURAHS.getOrNull(it - 1) }
                        if (surahId != null && ayahNum != null && meta != null && ayahNum in 1..meta.ayahCount) {
                            onJumpToSurahAyah(surahId, ayahNum)
                        } else {
                            error = "ayah"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("quick_jump_go_ayah")
                ) {
                    Text(stringResource(R.string.quick_jump_go_surah))
                }
                if (error != null) {
                    Text(
                        text = stringResource(R.string.quick_jump_invalid),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                }
            }
        }
    )
}

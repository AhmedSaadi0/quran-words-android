package com.quranwords.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quranwords.R
import com.quranwords.core.util.SizeUnit
import com.quranwords.core.util.formatFileSize

@Composable
fun formattedFileSize(bytes: Long): String {
    val size = formatFileSize(bytes) ?: return "—"
    return stringResource(
        if (size.unit == SizeUnit.MB) R.string.db_size_mb else R.string.db_size_kb,
        size.value
    )
}

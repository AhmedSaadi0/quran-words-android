package com.quranwords.ui.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.core.util.SurahMetadata
import com.quranwords.core.util.toEasternArabicDigits

@Composable
fun AyahSurahFilterBar(
    selectedSurahId: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val surahName = selectedSurahId?.let { id -> SurahMetadata.SURAHS.getOrNull(id - 1)?.nameAr }
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChipLike(
            label = stringResource(R.string.search_filter_all_quran),
            selected = selectedSurahId == null,
            onClick = { onSelect(null) },
            tag = "ayah_filter_all"
        )
        Box {
            FilterChipLike(
                label = if (selectedSurahId != null) "سورة $surahName" else stringResource(R.string.search_filter_surah),
                selected = selectedSurahId != null,
                onClick = { showMenu = true },
                tag = "ayah_filter_surah"
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                SurahMetadata.SURAHS.forEach { meta ->
                    DropdownMenuItem(
                        text = { Text("${meta.id.toEasternArabicDigits()}. سورة ${meta.nameAr}", fontSize = 12.5.sp) },
                        onClick = { onSelect(meta.id); showMenu = false },
                        modifier = Modifier.testTag("ayah_filter_item_${meta.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipLike(label: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(30.dp).testTag(tag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
            Icon(
                Icons.Filled.FilterList,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

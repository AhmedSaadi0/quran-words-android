package io.github.ahmedsaadi0.quranwords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import kotlin.math.roundToInt

@Composable
fun SurahDetailHeader(
    surah: Surah?,
    surahPages: List<Int>,
    currentPage: Int?,
    fontSize: Float,
    isBookmarked: Boolean,
    surahId: Int,
    onNavigateBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onPageClick: (Int) -> Unit,
    onHeaderHeightMeasured: (Int) -> Unit,
    headerOffsetPx: Float = 0f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .onSizeChanged { onHeaderHeightMeasured(it.height) }
            .testTag("surah_detail_header")
    ) {
        // Row 1 — 64dp pinned (title + controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
                Text(
                    text = surah?.let { "سُورَةُ ${it.nameAr}" } ?: "جاري التحميل...",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.testTag("bookmark_surah_$surahId")
                ) {
                    Text(
                        text = if (isBookmarked) "🔖" else "☆",
                        fontSize = 20.sp,
                        color = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FontSizeControls(
                    fontSize = fontSize,
                    onFontSizeChange = onFontSizeChange,
                    modifier = Modifier.size(width = 180.dp, height = 38.dp)
                )
            }
        }

        // Row 2 — 36dp pinned pages (always accessible)
        if (surahPages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .zIndex(1f)
                    .padding(bottom = 4.dp)
            ) {
                SurahPagesHeader(
                    pages = surahPages,
                    currentPage = currentPage,
                    onPageClick = onPageClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Collapsible subtitle — only this part slides away 1:1
        surah?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${it.revelationType} • ${it.ayahCount} آية",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package io.github.ahmedsaadi0.quranwords.ui.surah.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.components.AyahItemCard
import io.github.ahmedsaadi0.quranwords.ui.components.JuzHizbSeparator
import io.github.ahmedsaadi0.quranwords.ui.components.PageSeparator
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium

/**
 * Ayat list: Basmalah item, page/juz separators, staggered ayat cards and the
 * loading-more / end slots. Item appearance animation preserved 1:1.
 */
@Composable
fun SurahAyatList(
    ayat: List<Ayah>,
    surah: Surah?,
    fontSize: Float,
    bookmarkedAyat: Set<String>,
    surahId: Int,
    hasBasmalah: Boolean,
    isSelectionMode: Boolean,
    selectedAyahs: Set<Int>,
    isLoadingMore: Boolean,
    listState: LazyListState,
    onWordClick: (WordToken, Ayah) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEnterSelection: (Int) -> Unit,
    onBookmarkClick: (Int) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (hasBasmalah) {
            item(key = "basmalah") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeMedium)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            ShapeMedium
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontSize = 24.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        itemsIndexed(ayat, key = { _, ayah -> ayah.ayah }) { index, ayah ->
            val isAyahBookmarked = bookmarkedAyat.contains("$surahId:${ayah.ayah}")
            val prevAyah = ayat.getOrNull(index - 1)
            val isJuzStart = prevAyah?.juz != ayah.juz
            val isHizbStart = prevAyah?.hizb != ayah.hizb
            val isRubStart = prevAyah?.rubElHizb != ayah.rubElHizb
            val isPageStart = prevAyah?.pageNumber != ayah.pageNumber
            val showJuzHizb = index == 0 || isJuzStart || isHizbStart || isRubStart
            val showPage = isPageStart && ayah.pageNumber != null

            Column(
                modifier = Modifier.animateItem(
                    placementSpec = tween(
                        durationMillis = AppMotion.DurationMedium,
                        easing = AppMotion.EasingStandard
                    )
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showPage) {
                    PageSeparator(
                        pageNumber = ayah.pageNumber!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }
                if (showJuzHizb && ayah.juz != null) {
                    JuzHizbSeparator(
                        juz = ayah.juz,
                        hizb = ayah.hizb,
                        rubElHizb = ayah.rubElHizb,
                        isJuzStart = isJuzStart,
                        isHizbStart = isHizbStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 280,
                            delayMillis = (index % 20) * 18,
                            easing = AppMotion.EasingStandard
                        )
                    ) + slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = tween(
                            durationMillis = 280,
                            delayMillis = (index % 20) * 15,
                            easing = AppMotion.EasingEmphasized
                        )
                    )
                ) {
                    AyahItemCard(
                        ayah = ayah,
                        fontSize = fontSize,
                        isBookmarked = isAyahBookmarked,
                        onBookmarkClick = { onBookmarkClick(ayah.ayah) },
                        onWordClick = { word -> onWordClick(word, ayah) },
                        surah = surah,
                        isSelected = selectedAyahs.contains(ayah.ayah),
                        isSelectionMode = isSelectionMode,
                        onToggleSelection = { onToggleSelection(ayah.ayah) },
                        onEnterSelectionMode = { onEnterSelection(ayah.ayah) }
                    )
                }
            }
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
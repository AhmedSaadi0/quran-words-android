package io.github.ahmedsaadi0.quranwords.ui.surah.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.components.JuzHizbSeparator
import io.github.ahmedsaadi0.quranwords.ui.components.PageSeparator
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.QuranFont

/**
 * Ayat list: Basmalah item, page/juz separators, continuous-flow Mushaf
 * blocks (one Text per page group) and the loading-more / end slots. Item
 * appearance animation preserved 1:1.
 */
@Composable
fun SurahAyatList(
    ayat: List<Ayah>,
    fontSize: Float,
    quranFont: QuranFont,
    hasBasmalah: Boolean,
    isSelectionMode: Boolean,
    selectedAyahs: Set<Int>,
    isLoadingMore: Boolean,
    listState: LazyListState,
    onWordClick: (WordToken, Ayah) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEnterSelection: (Int) -> Unit
) {
    // Single source of truth for grouping — the screen derives its
    // scroll-index mapping from the same pure function.
    val groups = remember(ayat) { groupAyatByPage(ayat) }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasBasmalah) {
            item(key = "basmalah") {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    fontSize = 24.sp,
                    lineHeight = 36.sp,
                    fontFamily = quranFont.fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        itemsIndexed(groups, key = { _, group -> "mushaf_${group.key}" }) { groupIndex, group ->
            val first = group.ayat.first()
            val prevAyah = ayat.getOrNull(group.firstAyahIndex - 1)
            val isJuzStart = prevAyah?.juz != first.juz
            val isHizbStart = prevAyah?.hizb != first.hizb
            val isRubStart = prevAyah?.rubElHizb != first.rubElHizb
            val isPageStart = prevAyah?.pageNumber != first.pageNumber
            val showJuzHizb = group.firstAyahIndex == 0 || isJuzStart || isHizbStart || isRubStart
            val showPage = isPageStart && first.pageNumber != null

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
                        pageNumber = first.pageNumber!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }
                if (showJuzHizb && first.juz != null) {
                    JuzHizbSeparator(
                        juz = first.juz,
                        hizb = first.hizb,
                        rubElHizb = first.rubElHizb,
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
                            delayMillis = (groupIndex % 20) * 18,
                            easing = AppMotion.EasingStandard
                        )
                    ) + slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = tween(
                            durationMillis = 280,
                            delayMillis = (groupIndex % 20) * 15,
                            easing = AppMotion.EasingEmphasized
                        )
                    )
                ) {
                    MushafFlowBlock(
                        group = group,
                        fontSize = fontSize,
                        quranFont = quranFont,
                        isSelectionMode = isSelectionMode,
                        selectedAyahs = selectedAyahs,
                        onWordClick = onWordClick,
                        onToggleSelection = onToggleSelection,
                        onEnterSelection = onEnterSelection
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
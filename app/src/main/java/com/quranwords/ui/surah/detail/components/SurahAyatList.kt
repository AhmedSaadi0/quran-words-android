package com.quranwords.ui.surah.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.components.PageSeparator
import com.quranwords.ui.theme.AppMotion
import com.quranwords.ui.theme.QuranFont

@Composable
fun SurahAyatList(
    ayat: List<Ayah>,
    fontSize: Float,
    quranFont: QuranFont,
    hasBasmalah: Boolean,
    isSelectionMode: Boolean,
    selectedAyahs: Set<Int>,
    listState: LazyListState,
    onWordClick: (WordToken, Ayah) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEnterSelection: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Top padding must reserve the reading chrome (status bar + pill) at the
     * call site — frozen there, so immersive hide/show never re-measures this
     * list.
     */
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
    bookmarkedAyahs: Set<Int> = emptySet(),
    pulseAyah: Int? = null,
    onPulseDone: () -> Unit = {},
    surah: com.quranwords.domain.model.Surah? = null
) {
    val groups = remember(ayat) { groupAyatByPage(ayat) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (surah != null) {
            item(key = "surah_banner") {
                // Banner replaces the bare Basmalah line when surah is known;
                // the banner itself carries the Basmalah (except 1/9).
                SurahCardBanner(surah = surah, showBismillah = hasBasmalah)
            }
        } else if (hasBasmalah) {
            item(key = "basmalah") {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    fontFamily = quranFont.fontFamily,
                    fontSize = 24.sp,
                    lineHeight = (24f * quranFont.lineHeightMultiplier).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        itemsIndexed(groups, key = { _, group -> "mushaf_${group.key}_${group.firstAyahIndex}" }) { groupIndex, group ->
            MushafPageContent(
                group = group,
                groupIndex = groupIndex,
                ayat = ayat,
                quranFont = quranFont,
                isSelectionMode = isSelectionMode,
                selectedAyahs = selectedAyahs,
                bookmarkedAyahs = bookmarkedAyahs,
                pulseAyah = pulseAyah,
                onPulseDone = onPulseDone,
                onWordClick = onWordClick,
                onToggleSelection = onToggleSelection,
                onEnterSelection = onEnterSelection,
                animateEntrance = true,
                modifier = Modifier.animateItem(
                    placementSpec = tween(
                        durationMillis = AppMotion.DurationMedium,
                        easing = AppMotion.EasingStandard
                    )
                ),
                block = {
                    MushafFlowBlock(
                        group = group,
                        fontSize = fontSize,
                        quranFont = quranFont,
                        isSelectionMode = isSelectionMode,
                        selectedAyahs = selectedAyahs,
                        bookmarkedAyahs = bookmarkedAyahs,
                        pulseAyah = pulseAyah,
                        onPulseDone = onPulseDone,
                        onWordClick = onWordClick,
                        onToggleSelection = onToggleSelection,
                        onEnterSelection = onEnterSelection
                    )
                }
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * The full mushaf content of ONE page group: page header (surah | juz + hizb
 * start | page), the continuous-flow ayah block and the page footer — shared
 * by the vertical reading list and the book-pages pager so both render
 * byte-identical pages.
 *
 * [block] renders the flow block itself: the list passes a plain
 * [MushafFlowBlock] at the reader's font size, while the pager passes an
 * auto-fitted variant inside a BoxWithConstraints slot that expands
 * (`weight(1f)`) to absorb the remaining page height.
 *
 * [animateEntrance] runs the staggered fade/slide entrance used by the lazy
 * list; the pager disables it so a flipped-in page never replays the entrance.
 */
@Composable
fun MushafPageContent(
    group: AyahFlowGroup,
    groupIndex: Int,
    ayat: List<Ayah>,
    quranFont: QuranFont,
    isSelectionMode: Boolean,
    selectedAyahs: Set<Int>,
    bookmarkedAyahs: Set<Int>,
    pulseAyah: Int?,
    onPulseDone: () -> Unit,
    onWordClick: (WordToken, Ayah) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEnterSelection: (Int) -> Unit,
    animateEntrance: Boolean,
    block: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val chrome = mushafPageChrome(group, ayat)
    val first = group.ayat.first()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 0. Page header (Full Plan v2): surah name | juz (+ hizb start) | page.
        if (chrome.showHeader) {
            PageHeaderRow(
                surahId = first.surah,
                juz = first.juz,
                hizb = if (chrome.isHizbStart) first.hizb else null,
                pageNumber = first.pageNumber!!
            )
        }

        // 1. كتلة آيات الصفحة
        if (animateEntrance) {
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
                block()
            }
        } else {
            block()
        }

        // 2. رقم الصفحة أسفل كتلة الآيات مباشرة
        if (chrome.showPageNumber) {
            PageSeparator(
                pageNumber = first.pageNumber!!,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }
}

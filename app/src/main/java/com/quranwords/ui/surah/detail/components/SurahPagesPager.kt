package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken
import com.quranwords.ui.theme.QuranFont

/**
 * Book-pages reading mode: one mushaf page per pager page, flipped right/left
 * (RTL) like a paper book — Compose mirrors the flip with the layout direction
 * automatically. Page 0 carries the surah banner (which includes the
 * Basmalah) — or the bare Basmalah when the surah is unknown — before its
 * flow block, so page indices map 1:1 onto [AyahFlowGroup] indices.
 *
 * Overscroll is disabled at the call site (Android 12+ stretch otherwise eats
 * the edge deltas the adjacent-surah switch needs), and pages never scroll
 * vertically: the flow block lives in a weight(1f) slot whose font size is
 * auto-fitted ([rememberFittedMushafFontSize]) so the whole page fits the
 * screen at once — the user's font-size preference keeps ruling scroll mode.
 */
@Composable
fun SurahPagesPager(
    flowGroups: List<AyahFlowGroup>,
    ayat: List<Ayah>,
    quranFont: QuranFont,
    pagerState: PagerState,
    hasBasmalah: Boolean,
    surah: Surah?,
    isSelectionMode: Boolean,
    selectedAyahs: Set<Int>,
    bookmarkedAyahs: Set<Int>,
    pulseAyah: Int?,
    onPulseDone: () -> Unit,
    onWordClick: (WordToken, Ayah) -> Unit,
    onToggleSelection: (Int) -> Unit,
    onEnterSelection: (Int) -> Unit,
    /** Frozen top padding reserving the reading chrome (status bar + pill). */
    chromeTopPadding: Dp,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top
    ) { page ->
        val group = flowGroups[page]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = chromeTopPadding,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (page == 0) {
                if (surah != null) {
                    // Banner replaces the bare Basmalah line; it carries the
                    // Basmalah itself (except 1/9) — same as the scroll list.
                    SurahCardBanner(surah = surah, showBismillah = hasBasmalah)
                } else if (hasBasmalah) {
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

            MushafPageContent(
                group = group,
                groupIndex = page,
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
                animateEntrance = false,
                block = {
                    // Auto-fit slot: absorbs the page's leftover height so the
                    // chrome + block + footer always fill exactly one screen.
                    // The fitted size is measured against the real block text.
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val fittedFontSize = rememberFittedMushafFontSize(
                            group = group,
                            quranFont = quranFont,
                            slotWidth = maxWidth,
                            slotHeight = maxHeight
                        )
                        MushafFlowBlock(
                            group = group,
                            fontSize = fittedFontSize,
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
                }
            )
        }
    }
}

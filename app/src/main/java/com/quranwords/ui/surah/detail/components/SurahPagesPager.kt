package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.quranwords.core.util.AyahRef
import com.quranwords.core.util.SurahMetadata
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.Surah
import com.quranwords.ui.components.PageSeparator
import com.quranwords.ui.theme.QuranFont
import kotlinx.coroutines.flow.distinctUntilChanged

/** Initial banner height before the first real measurement. */
private const val BANNER_ESTIMATE_DP = 96
/** Tawbah / Fatihah have no separate Basmalah row. */
private const val BANNER_NO_BASMALLAH_ESTIMATE_DP = 72

/**
 * Book-pages reader over REAL mushaf pages 1..[totalPages]. Swiping always
 * moves page P → P±1; surah boundaries never navigate or recreate the screen.
 * Only a small window of pages lives in [pagesByNumber]; unloaded pages show a
 * spinner until the ViewModel's prefetch fills them.
 *
 * Mounted only after the VM has resolved [totalPages]/[entryPage] and preloaded
 * the entry page window — so `rememberPagerState` never starts at page 0 and
 * the first frame already shows the full Mushaf page.
 */
@Composable
fun SurahPagesPager(
    totalPages: Int,
    entryPage: Int,
    pagesByNumber: Map<Int, List<Ayah>>,
    quranFont: QuranFont,
    activeSurah: Surah?,
    isSelectionMode: Boolean,
    selectedAyahs: Set<AyahRef>,
    bookmarkedAyahs: Set<AyahRef>,
    pulseAyah: AyahRef?,
    onPulseDone: () -> Unit,
    wordInteraction: WordInteractionCallbacks,
    activeWordAction: WordActionTarget?,
    pendingPage: Int?,
    onPageChanged: (Int) -> Unit,
    onToggleSelection: (AyahRef) -> Unit,
    onEnterSelection: (AyahRef) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = (entryPage - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { totalPages })

    LaunchedEffect(Unit) { onPageChanged(entryPage) }
    LaunchedEffect(pagerState, onPageChanged) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> onPageChanged(page + 1) }
    }
    LaunchedEffect(pendingPage) {
        val page = pendingPage ?: return@LaunchedEffect
        if (page in 1..totalPages) pagerState.animateScrollToPage(page - 1)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top
    ) { index ->
        val pageNumber = index + 1
        val ayat = pagesByNumber[pageNumber]
        if (ayat.isNullOrEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            MushafPageContent(
                pageNumber = pageNumber,
                ayat = ayat,
                previousPageLast = pagesByNumber[pageNumber - 1]?.lastOrNull(),
                quranFont = quranFont,
                activeSurah = activeSurah,
                isSelectionMode = isSelectionMode,
                selectedAyahs = selectedAyahs,
                bookmarkedAyahs = bookmarkedAyahs,
                pulseAyah = pulseAyah,
                onPulseDone = onPulseDone,
                wordInteraction = wordInteraction,
                activeWordAction = activeWordAction,
                onToggleSelection = onToggleSelection,
                onEnterSelection = onEnterSelection
            )
        }
    }
}

@Composable
private fun MushafPageContent(
    pageNumber: Int,
    ayat: List<Ayah>,
    previousPageLast: Ayah?,
    quranFont: QuranFont,
    activeSurah: Surah?,
    isSelectionMode: Boolean,
    selectedAyahs: Set<AyahRef>,
    bookmarkedAyahs: Set<AyahRef>,
    pulseAyah: AyahRef?,
    onPulseDone: () -> Unit,
    wordInteraction: WordInteractionCallbacks,
    activeWordAction: WordActionTarget?,
    onToggleSelection: (AyahRef) -> Unit,
    onEnterSelection: (AyahRef) -> Unit
) {
    val density = LocalDensity.current
    // Full-bleed: chrome is an overlay, never part of this layout.
    val topPadding = 8.dp
    val bottomPadding = 8.dp
    val segments = remember(ayat) { splitPageIntoSegments(ayat, activeSurahId = null) }
    val first = ayat.first()

    val estimatePx = with(density) { BANNER_ESTIMATE_DP.dp.roundToPx() }
    val noBasmalahEstimatePx = with(density) { BANNER_NO_BASMALLAH_ESTIMATE_DP.dp.roundToPx() }
    var bannerHeights by remember(segments) {
        mutableStateOf(
            List(segments.size) { index ->
                if (segments[index].startsOnPage) {
                    if (showsBasmalah(segments[index].surahId)) estimatePx else noBasmalahEstimatePx
                } else 0
            }
        )
    }
    val bannerHeightsForFit = segments.mapIndexedNotNull { index, segment ->
        if (segment.startsOnPage) bannerHeights.getOrNull(index) ?: estimatePx else null
    }

    fun updateBannerHeight(index: Int, height: Int) {
        if (bannerHeights.getOrNull(index) == height) return
        bannerHeights = bannerHeights.toMutableList().also { it[index] = height }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PageHeaderRow(
            surahIds = segments.map { it.surahId },
            juz = first.juz,
            hizb = if (previousPageLast?.hizb != first.hizb) first.hizb else null,
            pageNumber = pageNumber
        )

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val fit = rememberMushafPageFit(
                segments = segments,
                bannerHeightsPx = bannerHeightsForFit,
                quranFont = quranFont,
                slotWidth = maxWidth,
                slotHeight = maxHeight
            )
            Column(verticalArrangement = Arrangement.spacedBy(MUSHAF_SEGMENT_GAP)) {
                segments.forEachIndexed { index, segment ->
                    if (segment.startsOnPage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clearAndSetSemantics { }
                                .onSizeChanged { size -> updateBannerHeight(index, size.height) }
                        ) {
                            SurahCardBanner(
                                surah = bannerSurah(segment.surahId, activeSurah),
                                showBismillah = showsBasmalah(segment.surahId)
                            )
                        }
                    }
                    MushafFlowBlock(
                        group = AyahFlowGroup(
                            key = "page_${pageNumber}_surah_${segment.surahId}",
                            pageNumber = pageNumber,
                            ayat = segment.ayat,
                            firstAyahIndex = index
                        ),
                        fontSize = fit.fontSize,
                        extraLineSpacingSp = fit.extraLineSpacingSp,
                        quranFont = quranFont,
                        isSelectionMode = isSelectionMode,
                        selectedAyahs = selectedAyahs,
                        bookmarkedAyahs = bookmarkedAyahs,
                        pulseAyah = pulseAyah,
                        onPulseDone = onPulseDone,
                        wordInteraction = wordInteraction,
                        activeWordAction = activeWordAction,
                        onToggleSelection = onToggleSelection,
                        onEnterSelection = onEnterSelection
                    )
                }
            }
        }

        PageSeparator(
            pageNumber = pageNumber,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/** Tawbah has no Basmalah; Fatiha's Basmalah is its ayah 1 (inside the text). */
private fun showsBasmalah(surahId: Int): Boolean = surahId != 1 && surahId != 9

/** Full Surah for a neighbour banner, preferring the already-loaded active one. */
private fun bannerSurah(surahId: Int, activeSurah: Surah?): Surah? {
    if (activeSurah?.id == surahId) return activeSurah
    val meta = SurahMetadata.SURAHS.getOrNull(surahId - 1) ?: return null
    return Surah(meta.id, meta.nameAr, meta.nameEn, meta.ayahCount, meta.revelationType, meta.juzStart)
}

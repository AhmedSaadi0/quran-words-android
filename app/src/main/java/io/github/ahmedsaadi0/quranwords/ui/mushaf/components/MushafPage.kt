package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.SurahMetadata
import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import io.github.ahmedsaadi0.quranwords.domain.model.Ayah
import io.github.ahmedsaadi0.quranwords.domain.model.WordToken
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafPageUi
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafSegment

/**
 * One assembled Mushaf page: header + body + footer directly on the screen
 * canvas (no card, no border). Padding is FIXED — never derived from system
 * insets — so bar toggles move zero text pixels. Taps on any non-interactive
 * area fall through to [onEmptyTap] (immersive toggle); word/marker taps are
 * consumed by the body and never reach it.
 */
@Composable
fun MushafPage(
    page: Int,
    pageUi: MushafPageUi?,
    isLoading: Boolean,
    bookmarkedAyat: Set<String>,
    onWordClick: (WordToken, Ayah) -> Unit,
    onMarkerClick: (surahId: Int, ayah: Int) -> Unit,
    onEmptyTap: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onEmptyTap() }) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("mushaf_page_$page")
    ) {
        when {
            pageUi != null -> MushafPageContent(
                page = page,
                pageUi = pageUi,
                bookmarkedAyat = bookmarkedAyat,
                onWordClick = onWordClick,
                onMarkerClick = onMarkerClick,
                onEmptyTap = onEmptyTap
            )
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            else -> MushafPageUnavailable(onRetry = onRetry)
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun MushafPageContent(
    page: Int,
    pageUi: MushafPageUi,
    bookmarkedAyat: Set<String>,
    onWordClick: (WordToken, Ayah) -> Unit,
    onMarkerClick: (surahId: Int, ayah: Int) -> Unit,
    onEmptyTap: () -> Unit
) {
    val firstAyah = pageUi.ayat.firstOrNull()
    val surahName = firstAyah?.let { ayah ->
        SurahMetadata.SURAHS.firstOrNull { it.id == ayah.surah }?.nameAr ?: ""
    } ?: ""
    val positionLabel = firstAyah?.let { ayah ->
        buildList {
            ayah.juz?.let { add("الجزء ${it.toEasternArabicDigits()}") }
            ayah.hizb?.let { add("الحزب ${it.toEasternArabicDigits()}") }
        }.joinToString(" • ")
    } ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        if (surahName.isNotBlank()) {
            MushafPageHeader(surahName = surahName, positionLabel = positionLabel)
            Spacer(Modifier.height(6.dp))
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Dynamic text fit: measure the whole page and binary-search the
            // largest size filling the height. Multi-surah pages compact
            // their banners, and the reserve tracks the rendered chrome.
            // Overflow beyond this still falls back to scroll inside the body.
            val density = LocalDensity.current
            val markerStyle = SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
            val bannerCount = pageUi.segments.count { it is MushafSegment.SurahStart }
            val compactBanners = bannerCount > 1
            val bannerReserve = if (compactBanners) COMPACT_BANNER_RESERVE else BANNER_RESERVE
            val fittedFontSize = rememberFittedMushafFontSize(
                pageUi = pageUi,
                markerStyle = markerStyle,
                bannerReservePx = with(density) { bannerReserve.roundToPx() } * bannerCount,
                maxWidthPx = with(density) { maxWidth.roundToPx() },
                maxHeightPx = with(density) { maxHeight.roundToPx() }
            )
            MushafPageBody(
                segments = pageUi.segments,
                fontSize = fittedFontSize,
                compactBanners = compactBanners,
                onWordClick = onWordClick,
                onMarkerClick = onMarkerClick,
                onEmptyTap = onEmptyTap
            )
        }
        Spacer(Modifier.height(4.dp))
        MushafPageFooter(page = page)
    }
}

@Composable
private fun MushafPageUnavailable(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.mushaf_page_unavailable),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.mushaf_retry))
        }
    }
}

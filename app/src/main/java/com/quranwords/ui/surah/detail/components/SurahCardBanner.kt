package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.quranwords.R
import com.quranwords.domain.model.Surah

/**
 * Measured from the original `chapter_hdr.png` (3420 × 360, now downscaled to
 * 1368 × 144 for cheaper decode): visible ink spans x 29..3385, y 32..326;
 * the blank centre between the ornaments spans x 760..2653.
 * Cropping to the ink keeps the frame's edges aligned with the caller's 16dp
 * text margin, and restricting the name to the measured blank centre keeps it
 * off the ornate ends and the top/bottom rules at every screen width.
 */
private const val HEADER_VISIBLE_ASPECT = (3385f - 29f) / (326f - 32f)
private const val HEADER_CENTER_WIDTH = (2653f - 760f) / (3385f - 29f)
private const val BANNER_GAP_DP = 4
private const val BANNER_TOP_PADDING_DP = 16
private const val BANNER_H_PADDING_DP = 2

private const val BISMILLAH = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"

/**
 * Deterministic height of [SurahCardBanner] for the fit pass. The Basmalah
 * line is measured with the real [TextStyle] and density, so large system
 * font scales (fontScale) can never make the text exceed the reserved slot —
 * no `onSizeChanged` feedback / second fit pass is needed.
 */
internal fun surahBannerHeightPx(
    slotWidth: Dp,
    showBismillah: Boolean,
    density: Density,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle
): Int {
    val contentWidthPx = with(density) { (slotWidth - BANNER_H_PADDING_DP.dp * 2).toPx() }
    val imageHeightPx = contentWidthPx / HEADER_VISIBLE_ASPECT
    val gapPx = with(density) { BANNER_GAP_DP.dp.toPx() }
    val bismillahPx = if (showBismillah) {
        with(density) { BANNER_TOP_PADDING_DP.dp.toPx() } +
            textMeasurer.measure(
                text = BISMILLAH,
                style = baseStyle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                constraints = Constraints(maxWidth = contentWidthPx.roundToInt().coerceAtLeast(1)),
                layoutDirection = LayoutDirection.Rtl
            ).size.height
    } else 0f
    // +2px absorbs sub-pixel rounding between measure and render.
    return (imageHeightPx + (if (showBismillah) gapPx else 0f) + bismillahPx).roundToInt() + 2
}

@Composable
internal fun rememberSurahBannerHeightPx(slotWidth: Dp, showBismillah: Boolean): Int {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val baseStyle = LocalTextStyle.current
    val fontScale = density.fontScale
    return remember(slotWidth, showBismillah, fontScale, baseStyle) {
        surahBannerHeightPx(slotWidth, showBismillah, density, textMeasurer, baseStyle)
    }
}

/**
 * Surah header: the ornamental QuranLexicon chapter frame, tinted with the
 * current Material primary so it works in light/dark/dynamic themes. The
 * surah name sits inside the frame's empty centre; revelation + ayah count,
 * basmalah and juz line stay directly below, so both reading modes keep the
 * same rhythm as before.
 */
@Composable
fun SurahCardBanner(
    surah: Surah?,
    showBismillah: Boolean,
    modifier: Modifier = Modifier
) {
    if (surah == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BANNER_H_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(BANNER_GAP_DP.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HEADER_VISIBLE_ASPECT)
                .testTag("surah_banner_${surah.id}")
        ) {
            Image(
                painter = painterResource(R.drawable.chapter_hdr),
                contentDescription = null, // decorative; the surah name below is the label
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(HEADER_CENTER_WIDTH)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.surah_title_template, surah.nameAr),
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        if (showBismillah) {
            Text(
                text = BISMILLAH,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = BANNER_TOP_PADDING_DP.dp)
                    .testTag("surah_banner_bismillah")
            )
        }
    }
}

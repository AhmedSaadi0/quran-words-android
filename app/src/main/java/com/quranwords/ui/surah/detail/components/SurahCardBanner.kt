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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.core.util.isMeccan
import com.quranwords.core.util.toEasternArabicDigits
import com.quranwords.domain.model.Surah

/**
 * Measured from `chapter_hdr.png` (3420 × 360): visible ink spans x 29..3385,
 * y 32..326; the blank centre between the ornaments spans x 760..2653.
 * Cropping to the ink keeps the frame's edges aligned with the caller's 16dp
 * text margin, and restricting the name to the measured blank centre keeps it
 * off the ornate ends and the top/bottom rules at every screen width.
 */
private const val HEADER_VISIBLE_ASPECT = (3385f - 29f) / (326f - 32f)
private const val HEADER_CENTER_WIDTH = (2653f - 760f) / (3385f - 29f)

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
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("surah_banner_bismillah")
            )
        }
    }
}

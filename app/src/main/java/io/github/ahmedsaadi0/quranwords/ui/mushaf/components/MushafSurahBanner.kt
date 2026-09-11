package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.core.util.toEasternArabicDigits
import io.github.ahmedsaadi0.quranwords.ui.mushaf.model.MushafSegment
import io.github.ahmedsaadi0.quranwords.ui.theme.AmiriQuran
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall

/**
 * Ultra-compact surah strip (~26–28dp) with a slim Basmalah (~20dp): total
 * footprint stays within ~48dp so verses keep the page on mid-page
 * transitions. The whole header sits on a single horizontal baseline —
 * combined metadata, centered name, Quranic ornament — with no border.
 * Rendered at the top of a page or inline mid-page wherever a surah begins.
 * [compact] (multi-surah pages) tightens type one step further to match
 * COMPACT_BANNER_RESERVE in the text-fit budget.
 */
@Composable
fun MushafSurahBanner(
    banner: MushafSegment.SurahStart,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mushaf_banner_${banner.surahId}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeSmall)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (banner.revelationType.isNotBlank() || banner.ayahCount > 0) {
                    Text(
                        text = buildString {
                            if (banner.revelationType.isNotBlank()) append(banner.revelationType)
                            if (banner.revelationType.isNotBlank() && banner.ayahCount > 0) append(" • ")
                            if (banner.ayahCount > 0) append(ayahCountLabel(banner.ayahCount))
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = "سورة ${banner.nameAr}",
                    fontFamily = AmiriQuran,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 14.sp else 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = "۝",
                    fontFamily = AmiriQuran,
                    fontSize = if (compact) 14.sp else 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
        if (banner.showBasmalah) {
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontFamily = AmiriQuran,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 1.dp).testTag("mushaf_basmalah")
            )
        }
    }
}

/**
 * Grammar-correct compact count label: plural آيات for 3–10, singular آية
 * otherwise (covers the full 3–286 surah range).
 */
internal fun ayahCountLabel(count: Int): String =
    "${count.toEasternArabicDigits()} ${if (count in 3..10) "آيات" else "آية"}"

package com.quranwords.ui.surah.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.core.util.FontSizePreference
import com.quranwords.core.util.PillTransparencyPreference
import com.quranwords.core.util.ReadingModePreference
import com.quranwords.ui.components.FontSizeControls
import com.quranwords.ui.theme.QuranFont

/**
 * Reading-chrome settings as a modal bottom sheet, opened from the reading
 * top bar's options menu (the collapsible header area no longer exists):
 *
 *  (a) **Mushaf font size** — the exact control the header used (A- / slider
 *      / A+ + numeric chip) plus a live preview rendered exactly like the
 *      mushaf (same cut and leading). In pages mode the mushaf font is
 *      auto-fitted to the screen, so the slider rules scroll mode only —
 *      an explicit note says so while that mode is active.
 *
 *  (b) **Pill transparency** — applies ONLY while the status bar is hidden
 *      (immersive reading); with the bars visible the indicator pill is
 *      always fully opaque. Persisted via the preferences pipeline.
 *
 *  (c) **Reading mode** — how a surah is laid out: continuous vertical scroll
 *      (default) or mushaf-style book pages flipped right/left. Persisted
 *      via the preferences pipeline.
 *
 * The caller composes this behind `if (showFontSheet)` so it leaves the
 * composition tree entirely once dismissed — no hidden sheet state lingers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeSheet(
    fontSize: Float,
    quranFont: QuranFont,
    pillTransparency: Float,
    readingMode: Int,
    onFontSizeChange: (Float) -> Unit,
    onPillTransparencyChange: (Float) -> Unit,
    onReadingModeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("font_size_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.reading_font_size_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FontSizeControls(
                fontSize = fontSize,
                onFontSizeChange = onFontSizeChange,
                modifier = Modifier.fillMaxWidth()
            )

            // Pages mode auto-fits the mushaf font to the screen (see
            // SurahPagesPager) — say so explicitly, so the slider's apparent
            // no-op in this mode reads as intent, not a bug.
            if (readingMode == ReadingModePreference.PAGES) {
                Text(
                    text = stringResource(R.string.reading_font_pages_auto_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("reading_font_pages_auto_note")
                )
            }

            // Live preview rendered exactly like the mushaf (same cut and
            // leading), so what the reader sees here is what the list becomes.
            val previewSize = fontSize.coerceIn(FontSizePreference.MIN, FontSizePreference.MAX)
            Text(
                text = stringResource(R.string.reading_font_preview_sample),
                fontFamily = quranFont.fontFamily,
                fontSize = previewSize.sp,
                lineHeight = (previewSize * quranFont.lineHeightMultiplier).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("font_size_preview_sample")
            )

            // Pill transparency — visually next to the font-size bar (user
            // spec). 15 steps over 0..0.8 = 5%-resolution; the chip shows the
            // percentage so the setting is readable, not just feelable.
            Text(
                text = stringResource(R.string.reading_pill_transparency_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.reading_pill_transparency_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val transparency = pillTransparency.coerceIn(
                    PillTransparencyPreference.MIN,
                    PillTransparencyPreference.MAX
                )
                Slider(
                    value = transparency,
                    onValueChange = onPillTransparencyChange,
                    valueRange = PillTransparencyPreference.MIN..PillTransparencyPreference.MAX,
                    steps = 15,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pill_transparency_slider")
                )
                Text(
                    text = stringResource(
                        R.string.reading_pill_transparency_percent,
                        (transparency * 100).toInt()
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("pill_transparency_value")
                )
            }

            // Reading mode — how a surah is laid out: continuous vertical
            // scroll or mushaf-style book pages. Two chips mirror the options.
            Text(
                text = stringResource(R.string.reading_mode_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.reading_mode_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = readingMode == ReadingModePreference.SCROLL,
                    onClick = { onReadingModeChange(ReadingModePreference.SCROLL) },
                    label = { Text(stringResource(R.string.reading_mode_scroll)) },
                    modifier = Modifier.testTag("reading_mode_scroll_chip")
                )
                FilterChip(
                    selected = readingMode == ReadingModePreference.PAGES,
                    onClick = { onReadingModeChange(ReadingModePreference.PAGES) },
                    label = { Text(stringResource(R.string.reading_mode_pages)) },
                    modifier = Modifier.testTag("reading_mode_pages_chip")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

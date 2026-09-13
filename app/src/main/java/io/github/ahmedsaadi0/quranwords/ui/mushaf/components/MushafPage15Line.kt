package io.github.ahmedsaadi0.quranwords.ui.mushaf.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.MushafConstants
import io.github.ahmedsaadi0.quranwords.domain.model.MushafLine
import io.github.ahmedsaadi0.quranwords.domain.model.MushafLineType
import io.github.ahmedsaadi0.quranwords.ui.mushaf.MushafPageData
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.theme.rememberMushafPalette

/**
 * One printed Mushaf page: exactly [MushafConstants.LINES_PER_PAGE] vertical
 * slots, content top-anchored (short pages leave the bottom empty, as in
 * print). Rendered inside [MushafPageFrame]; ink/paper come from the Mushaf
 * palette, never the app surface tokens.
 *
 * Tap routing: text lines own their detector ([MushafGlyphLine] opens words,
 * snaps gap taps to the nearest word, and forwards marker/margin taps);
 * header/basmalah/empty slots report background taps so the screen can
 * toggle chrome. No slot ever nests two detectors, and none consume drag
 * deltas — the pager stays fluid.
 */
@Composable
fun MushafPage15Line(
    data: MushafPageData,
    onWordTapped: (wordAyahId: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedWordAyahId: Int? = null,
    onBackgroundTapped: () -> Unit = {}
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val slotHeight = maxHeight / MushafConstants.LINES_PER_PAGE
        val byLine = remember(data.page) { data.page.lines.associateBy { it.lineNumber } }
        Column(modifier = Modifier.fillMaxSize()) {
            for (slot in MushafConstants.MIN_LINE..MushafConstants.MAX_LINE) {
                key(slot) {
                    val line = byLine[slot]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(slotHeight)
                            .testTag("mushaf_line_${data.page.pageNumber}_$slot")
                            .then(
                                if (line == null) {
                                    Modifier.pointerInput(data.page.pageNumber, slot) {
                                        detectTapGestures(onTap = { onBackgroundTapped() })
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (line != null) {
                            MushafLineContent(
                                data = data,
                                line = line,
                                onWordTapped = onWordTapped,
                                selectedWordAyahId = selectedWordAyahId,
                                onBackgroundTapped = onBackgroundTapped
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MushafLineContent(
    data: MushafPageData,
    line: MushafLine,
    onWordTapped: (wordAyahId: Int) -> Unit,
    selectedWordAyahId: Int?,
    onBackgroundTapped: () -> Unit
) {
    val palette = rememberMushafPalette()
    when (line.lineType) {
        MushafLineType.SURAH_HEADER -> {
            val name = data.page.surahNames[line.surahId] ?: ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .background(palette.goldSoft.copy(alpha = 0.4f), ShapeMedium)
                    .pointerInput(data.page.pageNumber, line.lineNumber) {
                        detectTapGestures(onTap = { onBackgroundTapped() })
                    }
                    .padding(1.dp)
                    .background(palette.paper, ShapeMedium)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.mushaf_surah_frame, name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        MushafLineType.BASMALAH -> {
            val basmalahFont = data.basmalahFont
            if (basmalahFont != null) {
                MushafBasmalah(
                    pageNumber = data.page.pageNumber,
                    lineNumber = line.lineNumber,
                    fontFamily = basmalahFont,
                    onBackgroundTapped = onBackgroundTapped
                )
            } else {
                // Decorative font missing: Unicode fallback keeps the slot
                // rhythm instead of a blank line (Decision 22).
                Text(
                    text = stringResource(R.string.mushaf_basmalah),
                    fontSize = 22.sp,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(data.page.pageNumber, line.lineNumber) {
                            detectTapGestures(onTap = { onBackgroundTapped() })
                        }
                )
            }
        }
        else -> {
            MushafGlyphLine(
                pageNumber = data.page.pageNumber,
                line = line,
                fontFamily = data.fontFamily,
                onWordTapped = onWordTapped,
                selectedWordAyahId = selectedWordAyahId,
                onBackgroundTapped = onBackgroundTapped
            )
        }
    }
}

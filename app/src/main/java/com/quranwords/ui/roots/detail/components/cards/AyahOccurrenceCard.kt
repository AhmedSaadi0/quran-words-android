package com.quranwords.ui.roots.detail.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.core.util.AyahHighlighter
import com.quranwords.domain.model.AyahOccurrenceModel
import com.quranwords.ui.theme.ArabicTextScale
import com.quranwords.ui.theme.ArabicTextScale.asMetadataLabel
import com.quranwords.ui.theme.QuranFont
import com.quranwords.ui.theme.ShapeSmall

@Composable
fun AyahOccurrenceCard(
    occ: AyahOccurrenceModel,
    onClick: () -> Unit,
    // Default = the shared baseline, NOT a hardcoded 24: callers that don't
    // expose the font-size preference (WordAyatScreen) still render verses at
    // the same size as the mushaf instead of drifting from the token.
    fontSize: Float = ArabicTextScale.DEFAULT_QURAN_SIZE,
    quranFont: QuranFont = QuranFont.KFGQPC_HAFS_1441
) {
    // Same scale as the mushaf: verses read 1:1 with the reading view.
    val textScale = ArabicTextScale.calculate(fontSize, quranFont.lineHeightMultiplier)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("occurrence_item_${occ.surahId}_${occ.ayahNum}")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeSmall,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    // surahNameAr is Arabic reference data (never translated).
                    text = stringResource(R.string.home_continue_template, occ.surahNameAr, occ.ayahNum),
                    style = MaterialTheme.typography.labelMedium.asMetadataLabel(textScale),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    // fill=false: the badge keeps its slot however far the
                    // slider is pushed (Arabic grows horizontally).
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = occ.matchedWordText,
                        style = MaterialTheme.typography.labelSmall.asMetadataLabel(textScale),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Cache highlight without SpanStyle object identity in keys.
            // Colors are stable value classes; SpanStyle is rebuilt only when they change.
            val highlightBg = MaterialTheme.colorScheme.tertiaryContainer
            val highlightFg = MaterialTheme.colorScheme.onTertiaryContainer
            val annotatedText = remember(occ.textUthmani, occ.matchedWordText, occ.ayahNum, highlightBg, highlightFg) {
                val highlightStyle = SpanStyle(
                    background = highlightBg,
                    color = highlightFg,
                    fontWeight = FontWeight.Bold
                )
                val suffix = " ﴿${occ.ayahNum}﴾"
                buildAnnotatedString {
                    append(occ.textUthmani)
                    AyahHighlighter.matchRanges(occ.textUthmani, occ.matchedWordText).forEach { range ->
                        addStyle(highlightStyle, range.first, range.last + 1)
                    }
                    append(suffix)
                }
            }

            // Same typeface, size and per-cut line rhythm as the surah reading
            // view — an occurrence is the same words the reader sees in mushaf.
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = quranFont.fontFamily,
                fontSize = textScale.verseTextSize,
                lineHeight = textScale.verseLineHeight,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
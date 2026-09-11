package io.github.ahmedsaadi0.quranwords.ui.roots.detail.components.cards

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
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.AyahHighlighter
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall

@Composable
fun AyahOccurrenceCard(
    occ: AyahOccurrenceModel,
    onClick: () -> Unit
) {
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
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = occ.matchedWordText,
                        style = MaterialTheme.typography.labelSmall,
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

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 28.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
package io.github.ahmedsaadi0.quranwords.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.data.util.ArabicNormalizer
import io.github.ahmedsaadi0.quranwords.domain.model.AyahOccurrenceModel
import io.github.ahmedsaadi0.quranwords.domain.model.DerivativeModel
import io.github.ahmedsaadi0.quranwords.domain.model.MasdarModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootMeaningModel
import io.github.ahmedsaadi0.quranwords.domain.model.RootWordModel
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall

@Composable
fun EmptyTabNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MeaningCard(meaning: RootMeaningModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeSmall,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = meaning.bookName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = meaning.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MasdarCard(masdar: MasdarModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeSmall,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = masdar.masdarAr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                masdar.pattern?.let {
                    Text(
                        // pattern is Arabic reference data; label is chrome.
                        text = stringResource(R.string.pattern_label, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (masdar.isAttested) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.attested_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DerivativeCard(derivative: DerivativeModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeSmall,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = derivative.formAr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    // derivativeType/pattern are Arabic data; label is chrome.
                    text = stringResource(R.string.search_derivative_line, derivative.derivativeType, derivative.pattern),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (derivative.isQuranic) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quranic_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordCard(
    word: RootWordModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_item_${word.wordId}")
            .semantics { selected = isSelected }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = ShapeSmall,
        border = if (isSelected) BorderStroke(2.dp, SolidColor(MaterialTheme.colorScheme.primary))
        else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.root_occurrences, word.occurrencesCount, word.occurrencesCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = if (isSelected) stringResource(R.string.cd_selected) else stringResource(R.string.cd_unselected),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedWordsBar(
    selectedCount: Int,
    isCopying: Boolean,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_words_bar"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = ShapeMedium,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pluralStringResource(R.plurals.selected_words_count, selectedCount, selectedCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onSelectAllClick,
                        modifier = Modifier.testTag("select_all_words_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.select_all),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = onClearClick,
                        modifier = Modifier.testTag("clear_words_selection_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.cd_cancel),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onCopyClick,
                    enabled = !isCopying && selectedCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_selected_words_btn"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = ShapeSmall
                ) {
                    if (isCopying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = if (isCopying) stringResource(R.string.copying) else stringResource(R.string.copy_ayat),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onShareClick,
                    enabled = !isCopying && selectedCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_selected_words_btn"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = ShapeSmall
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.common_share),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AyahOccurrenceCard(
    occ: AyahOccurrenceModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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

            val highlightStyle = SpanStyle(
                background = MaterialTheme.colorScheme.tertiaryContainer,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold
            )
            val annotatedText = remember(occ.textUthmani, occ.matchedWordText, highlightStyle) {
                buildHighlightedAyahText(
                    ayahText = occ.textUthmani,
                    matchedWord = occ.matchedWordText,
                    ayahNum = occ.ayahNum,
                    highlightStyle = highlightStyle
                )
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

@Composable
fun CopyAllOccurrencesBar(
    totalCount: Int,
    occurrencesSize: Int,
    isCopying: Boolean,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("copy_all_bar"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeMedium,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.root_ayat_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (totalCount > 0) stringResource(
                        R.string.occurrences_progress,
                        occurrencesSize,
                        totalCount,
                        pluralStringResource(R.plurals.root_occurrences, totalCount, totalCount)
                    ) else pluralStringResource(R.plurals.root_occurrences, occurrencesSize, occurrencesSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onCopyClick,
                    enabled = !isCopying && totalCount > 0,
                    modifier = Modifier.testTag("copy_all_occurrences_btn"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = ShapeSmall
                ) {
                    if (isCopying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = if (isCopying) stringResource(R.string.copying) else stringResource(R.string.copy_all),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onShareClick,
                    enabled = !isCopying && totalCount > 0,
                    modifier = Modifier.testTag("share_all_occurrences_btn"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = ShapeSmall
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.common_share),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun buildHighlightedAyahText(
    ayahText: String,
    matchedWord: String,
    ayahNum: Int,
    highlightStyle: SpanStyle
): androidx.compose.ui.text.AnnotatedString {
    val suffix = " ﴿$ayahNum﴾"
    if (matchedWord.isBlank()) {
        return buildAnnotatedString { append(ayahText + suffix) }
    }

    // Try direct substring match first (exact with diacritics)
    if (ayahText.contains(matchedWord)) {
        return buildAnnotatedString {
            var startIndex = 0
            var foundIndex = ayahText.indexOf(matchedWord, startIndex)
            while (foundIndex != -1) {
                append(ayahText.substring(startIndex, foundIndex))
                withStyle(highlightStyle) { append(ayahText.substring(foundIndex, foundIndex + matchedWord.length)) }
                startIndex = foundIndex + matchedWord.length
                foundIndex = ayahText.indexOf(matchedWord, startIndex)
            }
            append(ayahText.substring(startIndex))
            append(suffix)
        }
    }

    // Fallback: token-based match ignoring diacritics
    val strippedTarget = ArabicNormalizer.stripDiacritics(matchedWord)
    val normalizedTarget = ArabicNormalizer.normalizeAr(matchedWord)
    return buildAnnotatedString {
        val tokens = ayahText.split(" ")
        tokens.forEachIndexed { idx, token ->
            val strippedToken = ArabicNormalizer.stripDiacritics(token)
            val normalizedToken = ArabicNormalizer.normalizeAr(token)
            val isMatch = token == matchedWord ||
                strippedToken == strippedTarget ||
                normalizedToken == normalizedTarget ||
                strippedToken == normalizedTarget ||
                normalizedToken == strippedTarget

            if (isMatch) {
                withStyle(highlightStyle) { append(token) }
            } else {
                append(token)
            }
            if (idx < tokens.size - 1) append(" ")
        }
        append(suffix)
    }
}

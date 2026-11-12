package com.quranwords.ui.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.core.util.AyahHighlighter
import com.quranwords.core.util.toEasternArabicDigits
import com.quranwords.domain.model.Ayah
import com.quranwords.domain.model.DerivativeModel
import com.quranwords.domain.model.MasdarModel
import com.quranwords.domain.model.RootItem
import com.quranwords.domain.model.Surah
import com.quranwords.domain.model.WordToken

@Composable
fun AyahSearchCard(
    ayah: Ayah,
    query: String,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("ayah_result_${ayah.surah}_${ayah.ayah}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_ayah_ref, ayah.surah, ayah.ayah) + pageJuzSuffix(ayah),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_toggle_${ayah.surah}_${ayah.ayah}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_remove_bookmark),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HighlightedAyahText(text = ayah.textUthmani, query = query)
        }
    }
}

private fun pageJuzSuffix(ayah: Ayah): String {
    val page = ayah.pageNumber?.toEasternArabicDigits().orEmpty()
    val juz = ayah.juz?.toEasternArabicDigits().orEmpty()
    return when {
        page.isNotEmpty() && juz.isNotEmpty() -> " • الصفحة $page • الجزء $juz"
        page.isNotEmpty() -> " • الصفحة $page"
        juz.isNotEmpty() -> " • الجزء $juz"
        else -> ""
    }
}

@Composable
fun HighlightedAyahText(text: String, query: String, modifier: Modifier = Modifier) {
    val annotated = remember(text, query) {
        val ranges = AyahHighlighter.matchRanges(text, query.trim())
        if (ranges.isEmpty()) {
            AnnotatedString("$text ﴿${0.toEasternArabicDigits()}﴾")
        } else {
            buildAnnotatedStringWithHighlight(text, ranges)
        }
    }
    Text(text = annotated, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp, modifier = modifier)
}

private fun buildAnnotatedStringWithHighlight(text: String, ranges: List<IntRange>): AnnotatedString =
    buildAnnotatedStringFix(text, ranges)

private fun buildAnnotatedStringFix(text: String, ranges: List<IntRange>): AnnotatedString {
    val sb = AnnotatedString.Builder(text.length + 4)
    var pos = 0
    for (r in ranges) {
        if (pos < r.first) sb.append(text.substring(pos, r.first))
        sb.withStyle(SpanStyle(background = androidx.compose.ui.graphics.Color(0xFF285C4F), fontWeight = FontWeight.Bold)) {
            append(text.substring(r.first, r.last + 1))
        }
        pos = r.last + 1
    }
    if (pos < text.length) sb.append(text.substring(pos))
    sb.append(" ﴿")
    sb.append(0.toEasternArabicDigits())
    sb.append("﴾")
    return sb.toAnnotatedString()
}

@Composable
fun WordSearchCard(word: WordToken, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth().testTag("word_result_${word.wordId}")
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (word.rootText?.isNotBlank() == true) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = stringResource(R.string.search_word_root_label) + ": " + word.rootText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (word.translation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = word.translation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SurahSearchCard(surah: Surah, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth().testTag("surah_result_${surah.id}")
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = surah.id.toEasternArabicDigits(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bookmarks_surah_item, surah.nameAr),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = surah.nameEn + " • " + pluralStringResource(R.plurals.ayah_count, surah.ayahCount, surah.ayahCount),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RootSearchCard(root: RootItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth().testTag("root_result_${root.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = root.root, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (!root.glossAr.isNullOrBlank()) {
                Text(text = root.glossAr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MasdarSearchCard(masdar: MasdarModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("masdar_result_${masdar.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = masdar.masdarAr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.search_masdar_line, masdar.form ?: "", masdar.pattern ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DerivativeSearchCard(derivative: DerivativeModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("derivative_result_${derivative.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = derivative.formAr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.search_derivative_line, derivative.derivativeType, derivative.pattern),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptySearchNotice(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = modifier)
}

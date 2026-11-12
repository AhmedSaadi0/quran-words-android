package com.quranwords.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.domain.model.SearchResult

@Composable
fun SearchAllSectionsView(
    query: String,
    results: SearchResult,
    listState: LazyListState,
    bookmarkedAyat: Set<String>,
    onSelectSurah: (Int) -> Unit,
    onSelectAyah: (Int, Int) -> Unit,
    onToggleAyahBookmark: (Int, Int) -> Unit,
    onSelectRoot: (Int) -> Unit,
    onSelectWord: (com.quranwords.domain.model.WordToken) -> Unit,
    onSeeAllAyahs: () -> Unit,
    onSeeAllWords: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        results.exactReference?.let { exact ->
            item(key = "exact_ref") {
                VerseReferenceBanner(ayah = exact, onClick = { onSelectAyah(exact.surah, exact.ayah) })
            }
        }
        if (results.surahs.isNotEmpty()) {
            item(key = "sec_surahs") {
                SectionHeader(title = stringResource(R.string.search_section_surahs, results.surahs.size))
            }
            items(results.surahs, key = { "sur_${it.id}" }) { SurahSearchCard(surah = it, onClick = { onSelectSurah(it.id) }) }
        }
        if (results.words.isNotEmpty()) {
            item(key = "sec_words_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = stringResource(R.string.search_section_words, results.words.size))
                    if (results.words.size > 4) {
                        TextButton(onClick = onSeeAllWords) { Text(text = stringResource(R.string.search_see_all, results.words.size), fontSize = 12.sp) }
                    }
                }
            }
            items(results.words.take(4), key = { "w_${it.wordId}" }) { WordSearchCard(word = it, onClick = { onSelectWord(it) }) }
        }
        if (results.roots.isNotEmpty()) {
            item(key = "sec_roots") {
                SectionHeader(title = stringResource(R.string.search_section_roots, results.roots.size))
            }
            items(results.roots.take(4), key = { "r_${it.id}" }) { RootSearchCard(root = it, onClick = { onSelectRoot(it.id) }) }
        }
        if (results.ayat.isNotEmpty()) {
            item(key = "sec_ayat_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = stringResource(R.string.search_section_ayat, results.ayat.size))
                    if (results.ayat.size > 5) {
                        TextButton(onClick = onSeeAllAyahs) { Text(text = stringResource(R.string.search_see_all, results.ayat.size), fontSize = 12.sp) }
                    }
                }
            }
            items(results.ayat.take(5), key = { "a_${it.id}" }) { ayah ->
                val key = "${ayah.surah}:${ayah.ayah}"
                AyahSearchCard(
                    ayah = ayah,
                    query = query,
                    isBookmarked = bookmarkedAyat.contains(key),
                    onToggleBookmark = { onToggleAyahBookmark(ayah.surah, ayah.ayah) },
                    onClick = { onSelectAyah(ayah.surah, ayah.ayah) }
                )
            }
            if (results.ayat.size > 5) {
                item(key = "see_all_ayat") {
                    OutlinedButton(onClick = onSeeAllAyahs, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.search_see_all, results.ayat.size - 5))
                    }
                }
            }
        }
        if (results.surahs.isEmpty() && results.words.isEmpty() && results.roots.isEmpty() && results.ayat.isEmpty() && results.exactReference == null) {
            item(key = "no_results") {
                EmptySearchNotice(text = stringResource(R.string.search_no_ayat))
            }
        }
    }
}

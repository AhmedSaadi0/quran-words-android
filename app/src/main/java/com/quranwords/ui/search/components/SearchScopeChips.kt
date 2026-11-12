package com.quranwords.ui.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.ui.search.SearchScope

@Composable
fun SearchScopeChips(
    scope: SearchScope,
    ayahCount: Int,
    wordCount: Int,
    surahCount: Int,
    rootCount: Int,
    onScopeChanged: (SearchScope) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScopeChip(stringResource(R.string.search_scope_all), scope == SearchScope.ALL, null, { onScopeChanged(SearchScope.ALL) }, "search_scope_ALL")
        ScopeChip(stringResource(R.string.search_scope_ayahs), scope == SearchScope.AYAHS, ayahCount, { onScopeChanged(SearchScope.AYAHS) }, "search_scope_AYAHS")
        ScopeChip(stringResource(R.string.search_scope_words), scope == SearchScope.WORDS, wordCount, { onScopeChanged(SearchScope.WORDS) }, "search_scope_WORDS")
        ScopeChip(stringResource(R.string.search_scope_surahs), scope == SearchScope.SURAHS, surahCount, { onScopeChanged(SearchScope.SURAHS) }, "search_scope_SURAHS")
        ScopeChip(stringResource(R.string.search_scope_lexicon), scope == SearchScope.LEXICON, rootCount, { onScopeChanged(SearchScope.LEXICON) }, "search_scope_LEXICON")
    }
}

@Composable
private fun ScopeChip(label: String, selected: Boolean, count: Int?, onClick: () -> Unit, tag: String) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.height(34.dp).testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(5.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

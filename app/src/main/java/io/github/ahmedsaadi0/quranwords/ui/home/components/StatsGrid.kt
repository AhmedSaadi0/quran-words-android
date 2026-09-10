package io.github.ahmedsaadi0.quranwords.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.QuranStats
import io.github.ahmedsaadi0.quranwords.ui.components.StatCard

/** Corpus statistics section: header + 6 stat cards (2 per row). */
@Composable
fun StatsGrid(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.home_stats_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.home_stats_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = stringResource(R.string.home_stat_unique),
                tag = "unique",
                value = QuranStats.STATS_UNIQUE_WORDS,
                icon = Icons.Outlined.TextFields,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.home_stat_verified),
                tag = "verified",
                value = QuranStats.STATS_VERIFIED_ROOTS,
                icon = Icons.Outlined.Eco,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = stringResource(R.string.home_stat_masadir),
                tag = "masadir",
                value = QuranStats.STATS_MASADIR,
                icon = Icons.Outlined.MenuBook,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.home_stat_derivatives),
                tag = "derivatives",
                value = QuranStats.STATS_DERIVATIVES,
                icon = Icons.Outlined.AutoAwesome,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = stringResource(R.string.home_stat_positions),
                tag = "positions",
                value = QuranStats.STATS_WORD_POSITIONS,
                icon = Icons.Filled.LocationOn,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.home_stat_ayat),
                tag = "ayat",
                value = QuranStats.STATS_AYAT,
                icon = Icons.Filled.FormatListNumbered,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
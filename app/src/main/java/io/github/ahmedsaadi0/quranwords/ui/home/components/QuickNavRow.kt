package io.github.ahmedsaadi0.quranwords.ui.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.core.util.QuranStats
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium

/**
 * Quick-access navigation cards — equal-size via IntrinsicSize; icons are
 * vectors (Decision 13: 📖🌿📐 out).
 */
@Composable
fun QuickNavRow(
    onNavigateToSurahIndex: () -> Unit,
    onNavigateToRoots: () -> Unit,
    onNavigateToGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickNavCard(
            title = stringResource(R.string.home_nav_surahs_title),
            subtitle = stringResource(R.string.home_nav_surahs_subtitle, QuranStats.TOTAL_SURAHS),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = onNavigateToSurahIndex,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        QuickNavCard(
            title = stringResource(R.string.home_nav_roots_title),
            subtitle = stringResource(R.string.home_nav_roots_subtitle, QuranStats.STATS_VERIFIED_ROOTS),
            icon = Icons.Filled.Eco,
            onClick = onNavigateToRoots,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        QuickNavCard(
            title = stringResource(R.string.home_nav_guide_title),
            subtitle = stringResource(R.string.home_nav_guide_subtitle),
            icon = Icons.Filled.Architecture,
            onClick = onNavigateToGuide,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(ShapeMedium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AppMotion.DurationMedium,
                    easing = AppMotion.EasingStandard
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Fixed icon slot so the icon never shifts the text block.
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Fixed title slot (2 lines): font stays identical, overflow ellipsizes
            // instead of growing the card — cards remain equal by construction.
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
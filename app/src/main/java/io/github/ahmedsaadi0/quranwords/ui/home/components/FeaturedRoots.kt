package io.github.ahmedsaadi0.quranwords.ui.home.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.domain.model.RootItem
import io.github.ahmedsaadi0.quranwords.ui.components.RootItemCard
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall

/** Featured roots header + staggered list (Telegram-like 250ms animateItem). */
fun LazyListScope.FeaturedRootsSection(
    featuredRoots: List<RootItem>,
    onNavigateToRoots: () -> Unit,
    onNavigateToRootDetail: (Int) -> Unit
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_featured_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(
                onClick = onNavigateToRoots,
                shape = ShapeSmall
            ) {
                Text(stringResource(R.string.common_view_all))
            }
        }
    }
    items(
        count = featuredRoots.size,
        key = { index -> featuredRoots[index].id }
    ) { index ->
        val rootItem = featuredRoots[index]
        RootItemCard(
            rootItem = rootItem,
            onClick = { onNavigateToRootDetail(rootItem.id) },
            modifier = Modifier.animateItem(
                placementSpec = tween(
                    durationMillis = AppMotion.DurationMedium,
                    delayMillis = (index * AppMotion.StaggerDelayStep.toInt()).coerceAtMost(120),
                    easing = AppMotion.EasingStandard
                )
            )
        )
    }
    item {
        Spacer(modifier = Modifier.height(16.dp))
    }
}
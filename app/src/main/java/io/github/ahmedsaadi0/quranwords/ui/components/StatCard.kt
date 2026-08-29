package io.github.ahmedsaadi0.quranwords.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.domain.model.Surah
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatCard(
    title: String,
    value: Int,
    icon: String,
    modifier: Modifier = Modifier
) {
    val formattedValue = NumberFormat.getNumberInstance(Locale.US).format(value)
    Card(
        modifier = modifier
            .clip(ShapeMedium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AppMotion.DurationMedium,
                    easing = AppMotion.EasingStandard
                )
            )
            .testTag("stat_card_$title"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(ShapeSmall)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SurahItemCard(
    surah: Surah,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    onBookmarkClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AppMotion.DurationMedium,
                    easing = AppMotion.EasingStandard
                )
            )
            .testTag("surah_item_${surah.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Surah Number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.id.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "سُورَةُ ${surah.nameAr}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${surah.nameEn} • ${surah.ayahCount} آية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Bookmark toggle if provided
                if (onBookmarkClick != null) {
                    Box(
                        modifier = Modifier
                            .clip(ShapeSmall)
                            .background(
                                if (isBookmarked) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeSmall)
                            .clickable { onBookmarkClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("bookmark_surah_${surah.id}")
                    ) {
                        Text(
                            text = if (isBookmarked) "🔖" else "☆",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Revelation Type Badge - M3 Small 12dp unified
                Box(
                    modifier = Modifier
                        .clip(ShapeSmall)
                        .background(
                            if (surah.revelationType.contains("مكية"))
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                            else
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = surah.revelationType,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (surah.revelationType.contains("مكية"))
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

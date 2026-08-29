package io.github.ahmedsaadi0.quranwords.ui.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.core.util.QuranStats
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
import io.github.ahmedsaadi0.quranwords.ui.components.StatCard
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion

@Composable
fun StatsGrid(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.animateContentSize(tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(title = "كلمات فريدة", value = QuranMetaConstants.STATS_UNIQUE_WORDS, icon = "📝", modifier = Modifier.weight(1f))
            StatCard(title = "جذور محققة", value = QuranMetaConstants.STATS_VERIFIED_ROOTS, icon = "🌿", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(title = "مصادر لغوية", value = QuranMetaConstants.STATS_MASADIR, icon = "📚", modifier = Modifier.weight(1f))
            StatCard(title = "مشتقات وأوزان", value = QuranMetaConstants.STATS_DERIVATIVES, icon = "✨", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(title = "مواضع الكلمات", value = QuranMetaConstants.STATS_WORD_POSITIONS, icon = "📍", modifier = Modifier.weight(1f))
            StatCard(title = "الآيات الكريمة", value = QuranMetaConstants.STATS_AYAT, icon = "۝", modifier = Modifier.weight(1f))
        }
    }
}

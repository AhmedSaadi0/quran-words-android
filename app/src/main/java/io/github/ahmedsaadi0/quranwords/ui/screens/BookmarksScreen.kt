package io.github.ahmedsaadi0.quranwords.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ahmedsaadi0.quranwords.data.util.QuranMetaConstants
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarksScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit
) {
    val bookmarkedSurahs by mainViewModel.bookmarkedSurahs.collectAsState()
    val bookmarkedAyat by mainViewModel.bookmarkedAyat.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إشاراتي المرجعية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("bookmarks_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (bookmarkedSurahs.isEmpty() && bookmarkedAyat.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("⭐", fontSize = 48.sp)
                            Text(
                                "لا توجد إشارات مرجعية بعد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "استخدم أيقونة ☆ في صفحة السور والآيات لحفظ ما تريد الرجوع إليه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Surah bookmarks
            if (bookmarkedSurahs.isNotEmpty()) {
                item {
                    Text(
                        "السور المحفوظة (${bookmarkedSurahs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.animateItem(placementSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                    )
                }
                items(bookmarkedSurahs.toList().sortedBy { it.toIntOrNull() ?: 0 }, key = { "surah_$it" }) { surahIdStr ->
                    val surahId = surahIdStr.toIntOrNull() ?: 1
                    val meta = QuranMetaConstants.SURAHS.firstOrNull { it.id == surahId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(placementSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                            .clip(ShapeMedium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                            .clickable { onNavigateToSurahDetail(surahId, 1) }
                            .testTag("bookmark_surah_$surahId"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = ShapeMedium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${surahId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text(
                                        text = meta?.let { "سورة ${it.nameAr}" } ?: "سورة $surahId",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = meta?.let { "${it.nameEn} • ${it.ayahCount} آية" } ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onNavigateToSurahDetail(surahId, 1) },
                                    shape = ShapeSmall,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) { Text("فتح") }
                                IconButton(
                                    onClick = { mainViewModel.toggleSurahBookmark(surahId) },
                                    modifier = Modifier.testTag("remove_bookmark_surah_$surahId")
                                ) { Text("❌", fontSize = 14.sp) }
                            }
                        }
                    }
                }
            }

            // Ayah bookmarks
            if (bookmarkedAyat.isNotEmpty()) {
                item {
                    Text(
                        "الآيات المحفوظة (${bookmarkedAyat.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.animateItem(placementSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                    )
                }
                items(bookmarkedAyat.toList().sortedWith(compareBy({ it.split(":")[0].toIntOrNull() ?: 0 }, { it.split(":")[1].toIntOrNull() ?: 0 })), key = { "ayah_$it" }) { key ->
                    val parts = key.split(":")
                    val surahId = parts.getOrNull(0)?.toIntOrNull() ?: 1
                    val ayahNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                    val meta = QuranMetaConstants.SURAHS.firstOrNull { it.id == surahId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(placementSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard))
                            .clip(ShapeMedium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                            .clickable { onNavigateToSurahDetail(surahId, ayahNum) }
                            .testTag("bookmark_ayah_${surahId}_$ayahNum"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = ShapeMedium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("﴿$ayahNum﴾", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                                }
                                Column {
                                    Text(
                                        text = meta?.let { "سورة ${it.nameAr} • الآية $ayahNum" } ?: "سورة $surahId • $ayahNum",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "اضغط للانتقال مباشرة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onNavigateToSurahDetail(surahId, ayahNum) },
                                    shape = ShapeSmall,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) { Text("فتح") }
                                IconButton(
                                    onClick = { mainViewModel.toggleAyahBookmark(surahId, ayahNum) },
                                    modifier = Modifier.testTag("remove_bookmark_ayah_${surahId}_$ayahNum")
                                ) { Text("❌", fontSize = 14.sp) }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

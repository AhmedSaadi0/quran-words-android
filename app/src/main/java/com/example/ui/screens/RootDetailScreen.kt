package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.util.ArabicNormalizer
import com.example.domain.model.AyahOccurrenceModel
import com.example.domain.model.DerivativeModel
import com.example.domain.model.MasdarModel
import com.example.domain.model.RootMeaningModel
import com.example.ui.components.ReportIssueCard
import com.example.ui.theme.AppMotion
import com.example.ui.theme.Emerald700
import com.example.ui.theme.QuranGold
import com.example.ui.theme.ShapeLarge
import com.example.ui.theme.ShapeSmall
import com.example.ui.viewmodel.RootViewModel
import com.example.util.BuildIssueUrlOptions
import com.example.util.buildGithubIssueUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RootDetailScreen(
    rootId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSurahDetail: (Int, Int) -> Unit,
    viewModel: RootViewModel = viewModel()
) {
    val rootDetail by viewModel.rootDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val occurrences by viewModel.occurrences.collectAsState()
    val occurrencesHasMore by viewModel.occurrencesHasMore.collectAsState()
    val isOccurrencesLoadingMore by viewModel.isOccurrencesLoadingMore.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showReportDialog by remember { mutableStateOf(false) }

    // One LazyListState per tab — survives Crossfade switches, so scroll position is preserved
    val meaningsState = rememberLazyListState()
    val masadirState = rememberLazyListState()
    val derivativesState = rememberLazyListState()
    val ayatState = rememberLazyListState()

    LaunchedEffect(rootId) {
        viewModel.loadRootDetail(rootId)
    }

    // Pagination for ayat occurrences — only active when Ayat tab is selected
    LaunchedEffect(ayatState, selectedTabIndex, occurrences, occurrencesHasMore, isOccurrencesLoadingMore) {
        if (selectedTabIndex != 3) return@LaunchedEffect
        snapshotFlow { ayatState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collectLatest { lastIdx ->
                // Header(1) + Report(1) + stickyHeader(not counted) + occurrences items
                // occurrences start at index 2, so adjust: lastIdx - 2 = occurrence index
                val occIdx = lastIdx - 2
                viewModel.loadMoreOccurrencesIfNeeded(occIdx)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = rootDetail?.item?.let { "الجذر: [ ${it.root} ]" } ?: "تفاصيل الجذر",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (isLoading || rootDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val detail = rootDetail!!
            val item = detail.item

            // Each tab has its own LazyListState — switching tabs via Crossfade now preserves scroll position
            // Header + Report + TabRow are inside each list so they scroll together, but state is remembered per tab
            Crossfade(
                targetState = selectedTabIndex,
                animationSpec = tween(
                    durationMillis = AppMotion.DurationMedium,
                    easing = AppMotion.EasingStandard
                ),
                label = "RootDetailTabCrossfade"
            ) { animatedTabIndex ->
                val currentState = when (animatedTabIndex) {
                    0 -> meaningsState
                    1 -> masadirState
                    2 -> derivativesState
                    else -> ayatState
                }
                LazyColumn(
                    state = currentState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("root_detail_screen_${animatedTabIndex}"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Header Banner — same for every tab, scrolls with content; state per tab preserves position
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .animateContentSize(
                                    animationSpec = tween(
                                        durationMillis = AppMotion.DurationMedium,
                                        easing = AppMotion.EasingStandard
                                    )
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            shape = ShapeLarge
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.root.toCharArray().joinToString("  "),
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (occurrences.isNotEmpty() || detail.item.occurrencesCount > 0) {
                                            val countToShow = if (occurrences.isNotEmpty()) occurrences.size else detail.item.occurrencesCount
                                            val totalToShow = detail.item.occurrencesCount
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (totalToShow > countToShow) "$countToShow / $totalToShow موضع" else "$totalToShow موضع في التنزيل",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { showReportDialog = true },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .testTag("report_help_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HelpOutline,
                                                contentDescription = "الإبلاغ عن معنى",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (!detail.aiSummary.isNullOrBlank()) {
                                    Text(
                                        text = detail.aiSummary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else if (!item.glossAr.isNullOrBlank()) {
                                    Text(
                                        text = item.glossAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (!detail.aiModel.isNullOrBlank() || !detail.aiGeneratedAt.isNullOrBlank()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        detail.aiModel?.let { model ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "🤖 $model",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        detail.aiGeneratedAt?.let { rawDate ->
                                            val dateTimeFormatted = try {
                                                val cleaned = rawDate.replace("T", " ")
                                                if (cleaned.length >= 16) cleaned.substring(0, 16) else cleaned
                                            } catch (_: Exception) {
                                                rawDate
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "🕒 $dateTimeFormatted",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .animateItem()
                        ) {
                            ReportIssueCard(rootText = item.root, rootId = item.id)
                        }
                    }

                    stickyHeader {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text("المعاجم (${detail.meanings.size})", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = { Text("المصادر (${detail.masadir.size})", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                text = { Text("المشتقات (${detail.derivatives.size})", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTabIndex == 3,
                                onClick = { selectedTabIndex = 3 },
                                text = { Text("الآيات (${detail.item.occurrencesCount})", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    when (animatedTabIndex) {
                        0 -> {
                            if (detail.meanings.isEmpty()) {
                                item {
                                    EmptyTabNotice(text = "لا توجد معاني مدخلة لهذا الجذر حالياً")
                                }
                            } else {
                                items(detail.meanings, key = { it.id }) { meaning ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 5.dp)
                                            .animateItem()
                                    ) {
                                        MeaningCard(meaning)
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (detail.masadir.isEmpty()) {
                                item {
                                    EmptyTabNotice(text = "لا توجد مصادر مسجلة لهذا الجذر")
                                }
                            } else {
                                items(detail.masadir, key = { it.id }) { masdar ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 5.dp)
                                            .animateItem()
                                    ) {
                                        MasdarCard(masdar)
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (detail.derivatives.isEmpty()) {
                                item {
                                    EmptyTabNotice(text = "لا توجد مشتقات مسجلة لهذا الجذر")
                                }
                            } else {
                                items(detail.derivatives, key = { it.id }) { derivative ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 5.dp)
                                            .animateItem()
                                    ) {
                                        DerivativeCard(derivative)
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (occurrences.isEmpty() && !occurrencesHasMore && !isOccurrencesLoadingMore) {
                                item {
                                    EmptyTabNotice(text = "لا توجد مواضع مسجلة لهذا الجذر في هذه النسخة")
                                }
                            } else {
                                itemsIndexed(
                                    occurrences,
                                    key = { index, occ -> "${occ.surahId}-${occ.ayahNum}-${occ.matchedWordText}-${occ.textUthmani.hashCode()}-$index" }
                                ) { index, occ ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 5.dp)
                                            .animateItem()
                                    ) {
                                        AyahOccurrenceCard(
                                            occ = occ,
                                            onClick = { onNavigateToSurahDetail(occ.surahId, occ.ayahNum) }
                                        )
                                    }
                                }
                                if (isOccurrencesLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                } else if (occurrencesHasMore && occurrences.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "اسحب للأسفل لتحميل المزيد • ${occurrences.size} / ${detail.item.occurrencesCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else if (!occurrencesHasMore && occurrences.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "تم عرض جميع الآيات • ${occurrences.size} موضع",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
                // Report dialog — outside LazyColumn, inside Crossfade but after lists (shown once per switch, keep outside list)
                if (showReportDialog) {
                    val context = LocalContext.current
                    AlertDialog(
                        onDismissRequest = { showReportDialog = false },
                        title = { Text("الإبلاغ عن معنى", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "هل وجدت معنى غير صحيح أو ناقص للجذر [${item.root}]؟",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                ReportIssueCard(rootText = item.root, rootId = item.id)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val url = buildGithubIssueUrl(BuildIssueUrlOptions(item.root, item.id, null))
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    showReportDialog = false
                                }
                            ) { Text("فتح GitHub") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReportDialog = false }) { Text("إلغاء") }
                        },
                        shape = ShapeLarge
                    )
                }
            }
        }
    }
}

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
                        text = "الوزن: $it",
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
                        text = "وارد في التنزيل",
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
                    text = "${derivative.derivativeType} • وزن: ${derivative.pattern}",
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
                        text = "قرآني",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
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
                    text = "سورة ${occ.surahNameAr} • الآية ${occ.ayahNum}",
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
        // Split while keeping spaces - we rebuild with spaces
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

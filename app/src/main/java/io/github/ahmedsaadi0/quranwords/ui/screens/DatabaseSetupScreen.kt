package io.github.ahmedsaadi0.quranwords.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.ahmedsaadi0.quranwords.data.remote.DownloadState
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.Emerald700
import io.github.ahmedsaadi0.quranwords.ui.theme.QuranGold
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.DatabaseSetupViewModel
import io.github.ahmedsaadi0.quranwords.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DatabaseSetupScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    viewModel: DatabaseSetupViewModel = hiltViewModel()
) {
    val downloadState by viewModel.downloadState.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importDatabase(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إدارة قاعدة البيانات",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            mainViewModel.refreshDbStatus()
                            onNavigateBack()
                        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .animateContentSize(animationSpec = tween(durationMillis = AppMotion.DurationMedium))
                .testTag("db_setup_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (downloadState is DownloadState.Completed) "✅" else "💾",
                    fontSize = 38.sp
                )
            }

            Text(
                text = "قاعدة بيانات كلمات القرآن (118 ميجابايت)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "تحتوي على الإحصاء الكامل للقرآن الكريم: 77,429 موضع كلمة، 1,642 جذراً لغوياً، 5,273 مصدراً، 16,245 مشتقاً، ومعاجم لسان العرب والصحاح ومقاييس اللغة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = AppMotion.DurationMedium)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .animateContentSize(animationSpec = tween(durationMillis = AppMotion.DurationMedium)),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Crossfade(
                        targetState = downloadState,
                        animationSpec = tween(durationMillis = AppMotion.DurationMedium),
                        label = "downloadStateCrossfade"
                    ) { state ->
                        when (state) {
                            is DownloadState.Idle -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "حالة القاعدة: جاهزة للتنزيل",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = { viewModel.startDownload() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("start_download_button")
                                    ) {
                                        Text("بدء تنزيل قاعدة البيانات الكاملة")
                                    }
                                }
                            }
                            is DownloadState.Progress -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "جاري التنزيل: ${state.percentage}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (state.speedKbps > 0) {
                                            Text(
                                                text = "${state.speedKbps} ك.ب/ث",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { state.percentage / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    val mbDownloaded = state.downloadedBytes / (1024 * 1024)
                                    val mbTotal = state.totalBytes / (1024 * 1024)
                                    Text(
                                        text = "$mbDownloaded ميجابايت من $mbTotal ميجابايت",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is DownloadState.Completed -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "تم تنزيل وتثبيت قاعدة البيانات بنجاح!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "يمكنك الآن تصفح كامل المصحف والمعجم والمشتقات دون الحاجة إلى إنترنت.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            mainViewModel.refreshDbStatus()
                                            onNavigateBack()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("finish_setup_button")
                                    ) {
                                        Text("العودة إلى التطبيق")
                                    }
                                }
                            }
                            is DownloadState.Error -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "تعذر تنزيل قاعدة البيانات",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { viewModel.startDownload() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("retry_download_button")
                                    ) {
                                        Text("إعادة المحاولة")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Import button list with animateItem - balanced animation for progress states
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (downloadState is DownloadState.Progress) 0.dp else 56.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (downloadState !is DownloadState.Progress) {
                    item {
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .testTag("import_db_button")
                        ) {
                            Text("📂 استيراد قاعدة البيانات من الذاكرة")
                        }
                    }
                }
                // Placeholder item to ensure animateItem usage when in progress - keeps lazy structure stable
                if (downloadState is DownloadState.Progress) {
                    item {
                        Spacer(
                            modifier = Modifier
                                .height(0.dp)
                                .animateItem()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    mainViewModel.refreshDbStatus()
                    onNavigateBack()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("المتابعة باستخدام بيانات المعاينة السريعة")
            }
        }
    }

    if (showImportDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = "استيراد قاعدة البيانات", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "اختر ملف قاعدة البيانات (quran_words.db) من ذاكرة الجهاز. يجب أن يكون حجم الملف حوالي 118 ميجابايت. سيتم نسخ الملف إلى مسار التطبيق بنفس آلية التنزيل وسيتم استبدال القاعدة الحالية إن وجدت.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showImportDialog = false
                        importPicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text("اختيار الملف")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

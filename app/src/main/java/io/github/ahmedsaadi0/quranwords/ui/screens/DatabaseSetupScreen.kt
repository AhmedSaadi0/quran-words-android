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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    setupViewModel: DatabaseSetupViewModel
) {
    val downloadState by setupViewModel.downloadState.collectAsState()
    val latestRelease by setupViewModel.latestRelease.collectAsState()
    val installedVersion by setupViewModel.installedVersion.collectAsState()
    val isCheckingUpdate by setupViewModel.isCheckingUpdate.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            setupViewModel.importDatabase(uri)
        }
    }

    LaunchedEffect(Unit) {
        setupViewModel.checkForUpdate()
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
        val isBusy = downloadState is DownloadState.Progress ||
            downloadState is DownloadState.Extracting
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("db_setup_screen"),
            contentPadding = PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon
            item {
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
            }

            item {
                Text(
                    text = "قاعدة بيانات كلمات القرآن",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Text(
                    text = "تحتوي على الإحصاء الكامل للقرآن الكريم: 77,429 موضع كلمة، 1,642 جذراً لغوياً، 5,273 مصدراً، 16,245 مشتقاً، ومعاجم لسان العرب والصحاح ومقاييس اللغة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            item {
                VersionStatusCard(
                    installedCode = installedVersion.versionCode,
                    installedName = installedVersion.versionName,
                    latestRelease = latestRelease,
                    isChecking = isCheckingUpdate,
                    showNotes = showNotes,
                    onToggleNotes = { showNotes = !showNotes },
                    onCheckClick = { setupViewModel.checkForUpdate() }
                )
            }

            item {
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
                    // مفتاح ثابت لكل نوع حالة: يمنع إعادة حركة التلاشي مع كل نبضة
                    // تقدم (Progress جديد كل نسبة)، ويبقيها للانتقالات الحقيقية فقط.
                    val stateKey = when (downloadState) {
                        is DownloadState.Idle -> 0
                        is DownloadState.Progress -> 1
                        is DownloadState.Extracting -> 2
                        is DownloadState.Completed -> 3
                        is DownloadState.Error -> 4
                    }
                    Crossfade(
                        targetState = stateKey,
                        animationSpec = tween(durationMillis = AppMotion.DurationMedium),
                        label = "downloadStateCrossfade"
                    ) {
                        when (val state = downloadState) {
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
                                        onClick = { setupViewModel.startDownload() },
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
                                val isExtractPhase =
                                    state.phase == io.github.ahmedsaadi0.quranwords.data.remote.DownloadPhase.EXTRACT
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isExtractPhase) "جاري فك الضغط: ${state.percentage}%"
                                            else "جاري التنزيل: ${state.percentage}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (state.speedKbps > 0 && !isExtractPhase) {
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
                                        text = if (isExtractPhase) "فك الضغط داخل الجهاز..."
                                        else if (mbTotal > 0) "$mbDownloaded ميجابايت من $mbTotal ميجابايت"
                                        else "$mbDownloaded ميجابايت",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is DownloadState.Extracting -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "جاري فك الضغط: ${state.percentage}%",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LinearProgressIndicator(
                                        progress = { state.percentage / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                            is DownloadState.Completed -> {
                                val pendingUpdate = latestRelease?.takeIf {
                                    it.versionCode > installedVersion.versionCode &&
                                        installedVersion.versionCode > 0
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (pendingUpdate != null) {
                                        Text(
                                            text = "⬆️ يتوفر تحديث جديد ${pendingUpdate.versionName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "حجم التنزيل: ${formatVersionSize(pendingUpdate.compressedSize)}" +
                                                " • يتم فك الضغط والتثبيت تلقائيًا",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { setupViewModel.startDownload() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("start_update_download_button")
                                        ) {
                                            Text("تنزيل التحديث الآن")
                                        }
                                    } else {
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
                                    }
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
                                        onClick = { setupViewModel.startDownload() },
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
            }

            if (!isBusy) {
                item {
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_db_button")
                    ) {
                        Text("📂 استيراد قاعدة البيانات من الذاكرة")
                    }
                }
            }

            item {
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

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (showImportDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = "استيراد قاعدة البيانات", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "اختر ملف قاعدة البيانات (quran_words.db أو quran_words.db.zip) من ذاكرة الجهاز. الملف المضغوط أسرع في النقل وسيتم فك ضغطه تلقائيًا داخل التطبيق.",
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

@Composable
private fun VersionStatusCard(
    installedCode: Int,
    installedName: String,
    latestRelease: io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo?,
    isChecking: Boolean,
    showNotes: Boolean,
    onToggleNotes: () -> Unit,
    onCheckClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("db_version_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حالة الإصدار",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onCheckClick,
                    enabled = !isChecking,
                    modifier = Modifier.testTag("check_update_button")
                ) {
                    Text(if (isChecking) "جاري الفحص..." else "فحص التحديث")
                }
            }
            Text(
                text = if (installedCode > 0) "المثبتة: $installedName (رمز $installedCode)"
                else "المثبتة: غير معروفة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (latestRelease != null) {
                val isNew = latestRelease.versionCode > installedCode && installedCode > 0
                Text(
                    text = buildString {
                        append("الأحدث: ${latestRelease.versionName} (رمز ${latestRelease.versionCode})")
                        if (isNew) append(" • تحديث متوفر")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isNew) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNew) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "حجم التنزيل: ${formatVersionSize(latestRelease.compressedSize)}" +
                        (if (latestRelease.uncompressedSize > 0)
                            " • بعد الفك: ${formatVersionSize(latestRelease.uncompressedSize)}" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (latestRelease.releaseNotesAr.isNotBlank()) {
                    TextButton(
                        onClick = onToggleNotes,
                        modifier = Modifier.testTag("toggle_release_notes_btn")
                    ) {
                        Text(if (showNotes) "إخفاء ملاحظات الإصدار" else "عرض ملاحظات الإصدار")
                    }
                    if (showNotes) {
                        Text(
                            text = latestRelease.releaseNotesAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("release_notes_text")
                        )
                    }
                }
            } else if (!isChecking) {
                Text(
                    text = "تعذر جلب معلومات الإصدار — تحقق من الاتصال ثم أعد الفحص.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatVersionSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024 * 1024)
    if (mb >= 1) return "$mb ميجابايت"
    return "${bytes / 1024} ك.ب"
}

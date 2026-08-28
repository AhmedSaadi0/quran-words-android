package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.QuranRepositoryImpl
import com.example.domain.model.Ayah
import com.example.domain.model.WordToken
import com.example.ui.theme.AppMotion
import com.example.ui.theme.Emerald700
import com.example.ui.theme.QuranGold
import com.example.ui.theme.ShapeMedium
import com.example.ui.theme.ShapeSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorphologyBottomSheet(
    word: WordToken,
    ayah: Ayah?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNavigateToRoot: (rootId: Int, rootText: String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("morphology_bottom_sheet")
    ) {
        // Content appears instantly with sheet slide (no double animation) - fixes lag feeling
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prominent Word Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = word.text,
                    fontSize = 36.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                if (word.translation.isNotBlank()) {
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (ayah != null) {
                    Text(
                        text = "الآية ${ayah.ayah}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Root Card & Action — M3 ShapeMedium 16dp + balanced 250ms
            if (!word.rootText.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeMedium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
                        .clickable {
                            onNavigateToRoot(word.rootId ?: 0, word.rootText)
                            onDismiss()
                        }
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = AppMotion.DurationMedium,
                                easing = AppMotion.EasingStandard
                            )
                        )
                        .testTag("open_root_${word.rootText}"),
                    shape = ShapeMedium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الجذر اللغوي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "[ ${word.rootText.toCharArray().joinToString(" ")} ]",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                onNavigateToRoot(word.rootId ?: 0, word.rootText)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = ShapeSmall
                        ) {
                            Text(
                                text = "تفاصيل الجذر والمشتقات",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // AI Summary Card - replaces morphological analysis as requested
            AiSummarySection(word = word)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AiSummarySection(word: WordToken) {
    val context = LocalContext.current
    val repository = remember(context) { QuranRepositoryImpl(context) }
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var aiModel by remember { mutableStateOf<String?>(null) }
    var aiDate by remember { mutableStateOf<String?>(null) }
    var isLoading by remember(word.rootId) { mutableStateOf(word.rootId != null) }
    var hasTried by remember(word.rootId) { mutableStateOf(word.rootId == null) }

    LaunchedEffect(word.rootId) {
        val rootId = word.rootId
        if (rootId != null && rootId > 0) {
            isLoading = true
            hasTried = true
            try {
                val detail = repository.getRootDetail(rootId)
                aiSummary = detail?.aiSummary
                aiModel = detail?.aiModel
                aiDate = detail?.aiGeneratedAt
            } catch (_: Exception) {
            }
            isLoading = false
        } else {
            hasTried = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .clip(ShapeMedium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "الملخص الذكي للجذر",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
            !aiSummary.isNullOrBlank() -> {
                Text(
                    text = aiSummary!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                // Model + date chips - stacked vertically to prevent wrapping, with time included
                if (!aiModel.isNullOrBlank() || !aiDate.isNullOrBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        aiModel?.let { model ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
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
                        aiDate?.let { rawDate ->
                            val dateTimeFormatted = try {
                                val cleaned = rawDate.replace("T", " ")
                                if (cleaned.length >= 16) cleaned.substring(0, 16) else cleaned
                            } catch (_: Exception) { rawDate }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
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
            hasTried -> {
                Text(
                    text = if (word.rootId == null) "هذه الكلمة غير مرتبطة بجذر لغوي" else "لا يوجد ملخص ذكي متاح لهذا الجذر حالياً",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PropertyRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

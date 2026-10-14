package com.quranwords.ui.roots.detail.components.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranwords.R
import com.quranwords.ui.theme.AppMotion
import com.quranwords.ui.theme.ShapeSmall

@Composable
fun AiSectionCard(
    title: String,
    body: String,
    refs: List<String>,
    refsLabel: String,
    fontSize: Float,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTagSuffix: String
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_section_$testTagSuffix"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = ShapeSmall,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column {
            AiSectionHeader(
                title = title,
                expanded = expanded,
                onToggle = onToggle,
                testTagSuffix = testTagSuffix
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)
                ) + fadeIn(
                    animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard)
                ) + fadeOut(
                    animationSpec = tween(AppMotion.DurationShort, easing = AppMotion.EasingStandard)
                )
            ) {
                AiSectionBody(
                    body = body,
                    refs = refs,
                    refsLabel = refsLabel,
                    fontSize = fontSize,
                    testTagSuffix = testTagSuffix
                )
            }
        }
    }
}

@Composable
private fun AiSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTagSuffix: String
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EasingStandard),
        label = "ai_chevron_$testTagSuffix"
    )
    val toggleDescription = stringResource(
        if (expanded) R.string.ai_section_collapse else R.string.ai_section_expand
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = toggleDescription, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("ai_section_${testTagSuffix}_toggle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = toggleDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevronRotation)
        )
    }
}

@Composable
private fun AiSectionBody(
    body: String,
    refs: List<String>,
    refsLabel: String,
    fontSize: Float,
    testTagSuffix: String
) {
    CitedBodyText(
        body = body,
        refs = refs,
        refsLabel = refsLabel,
        fontSize = fontSize,
        testTagPrefix = "ai_ref_$testTagSuffix",
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
    )
}

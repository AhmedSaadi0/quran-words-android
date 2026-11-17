package com.quranwords.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R
import com.quranwords.ui.theme.ShapeSmall

@Composable
fun PageSeparator(
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    val isOdd = (pageNumber and 1) != 0
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Odd pages stay on physical Right; even pages stay on physical Left regardless of UI locale
    val placeNumberAtStart = if (isRtl) isOdd else !isOdd

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (placeNumberAtStart) {
            PageNumberBadge(pageNumber = pageNumber)
            PageSeparatorDivider(modifier = Modifier.weight(1f))
        } else {
            PageSeparatorDivider(modifier = Modifier.weight(1f))
            PageNumberBadge(pageNumber = pageNumber)
        }
    }
}

@Composable
private fun PageNumberBadge(
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ShapeSmall)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.page_label, pageNumber),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PageSeparatorDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}
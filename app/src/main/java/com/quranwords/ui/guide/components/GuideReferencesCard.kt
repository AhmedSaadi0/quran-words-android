package com.quranwords.ui.guide.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranwords.R

/**
 * References card for the morphology guide: AI root-summary sources from
 * ROOT_SUMMARY_METHODOLOGY.md (v2.1), grouped by the five semantic sections.
 * All text comes from string resources (AR/EN); nothing is hardcoded.
 */
@Composable
fun GuideReferencesCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("guide_refs_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.guide_refs_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.guide_refs_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GuideReferenceSection(
                titleRes = R.string.guide_refs_hissi_title,
                bodyRes = R.string.guide_refs_hissi_body,
                testTag = "guide_refs_hissi"
            )
            GuideReferenceSection(
                titleRes = R.string.guide_refs_tatawwur_title,
                bodyRes = R.string.guide_refs_tatawwur_body,
                testTag = "guide_refs_tatawwur"
            )
            GuideReferenceSection(
                titleRes = R.string.guide_refs_tashrih_title,
                bodyRes = R.string.guide_refs_tashrih_body,
                testTag = "guide_refs_tashrih"
            )
            GuideReferenceSection(
                titleRes = R.string.guide_refs_shabaka_title,
                bodyRes = R.string.guide_refs_shabaka_body,
                testTag = "guide_refs_shabaka"
            )
            GuideReferenceSection(
                titleRes = R.string.guide_refs_furuq_title,
                bodyRes = R.string.guide_refs_furuq_body,
                testTag = "guide_refs_furuq"
            )
        }
    }
}

@Composable
private fun GuideReferenceSection(
    titleRes: Int,
    bodyRes: Int,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package io.github.ahmedsaadi0.quranwords.ui.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ahmedsaadi0.quranwords.R
import io.github.ahmedsaadi0.quranwords.ui.components.DbUpdateBanner
import io.github.ahmedsaadi0.quranwords.ui.theme.AppMotion
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeMedium
import io.github.ahmedsaadi0.quranwords.ui.theme.ShapeSmall

/** Database-missing banner (Decision 13: 💾 → vector). */
@Composable
fun DbSetupBanner(
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ShapeMedium)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AppMotion.DurationMedium,
                    easing = AppMotion.EasingStandard
                )
            )
            .testTag("db_setup_banner"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.home_db_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.home_db_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onNavigateToSetup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = ShapeSmall,
                    modifier = Modifier.testTag("download_db_btn")
                ) {
                    Text(stringResource(R.string.home_db_action))
                }
            }
        }
    }
}

/** Update-available banner (wraps the shared DbUpdateBanner component). */
@Composable
fun HomeUpdateBanner(
    info: io.github.ahmedsaadi0.quranwords.domain.model.DbReleaseInfo,
    onUpdateClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DbUpdateBanner(
        info = info,
        installedName = "",
        onUpdateClick = onUpdateClick,
        onDismissClick = onDismissClick,
        modifier = modifier
    )
}
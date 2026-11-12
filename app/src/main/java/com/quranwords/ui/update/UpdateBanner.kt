package com.quranwords.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranwords.R

internal object UpdateBannerTags {
    const val ROOT = "update_banner_root"
    const val UPDATE_ACTION = "update_banner_action_update"
    const val RESTART_ACTION = "update_banner_action_restart"
    const val DISMISS_ACTION = "update_banner_action_dismiss"
    const val PROGRESS = "update_banner_progress"
}

@Composable
internal fun UpdateAvailableBanner(
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = Icons.Filled.SystemUpdate, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.update_available_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.update_available_message),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FilledTonalButton(
                onClick = onUpdateClick,
                modifier = Modifier
                    .height(48.dp)
                    .testTag(UpdateBannerTags.UPDATE_ACTION),
            ) {
                Text(text = stringResource(R.string.update_banner_update_now))
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(UpdateBannerTags.DISMISS_ACTION),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_update_banner_close),
                )
            }
        }
    }
}

@Composable
internal fun UpdateDownloadingBanner(
    percent: Int,
    modifier: Modifier = Modifier,
) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag(UpdateBannerTags.PROGRESS))
                Text(
                    text = stringResource(R.string.update_banner_downloading, percent),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LinearProgressIndicator(
                progress = { percent.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun UpdateRestartBanner(
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.update_banner_restart_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.update_banner_restart_message),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .height(48.dp)
                    .testTag(UpdateBannerTags.RESTART_ACTION),
            ) {
                Text(text = stringResource(R.string.update_banner_restart_now))
            }
        }
    }
}

@Composable
internal fun UpdateFailedBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BannerSurface(modifier = modifier.testTag(UpdateBannerTags.ROOT)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.update_banner_failed_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(UpdateBannerTags.DISMISS_ACTION),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_update_banner_close),
                )
            }
        }
    }
}

@Composable
private fun BannerSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

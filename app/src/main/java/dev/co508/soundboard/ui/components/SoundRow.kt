package dev.co508.soundboard.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.co508.soundboard.R
import dev.co508.soundboard.ui.SoundRowState

/**
 * One full-width rounded row: name on the left, volume percent and a
 * play/pause toggle on the right.
 *
 * Tapping the body toggles playback too — the toggle button is the affordance,
 * but the whole card is a far easier target on a phone. Long-pressing opens the
 * options sheet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundRow(
    row: SoundRowState,
    onToggle: () -> Unit,
    onVolumeClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(CORNER_RADIUS),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (row.isPlaying) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onToggle, onLongClick = onLongPress),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.sound.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.isUnavailable) {
                    Text(
                        text = stringResource(R.string.unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(R.string.unavailable_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // The percent doubles as the button that opens the volume dial.
            TextButton(onClick = onVolumeClick) {
                Text(
                    text = stringResource(R.string.volume_percent, row.sound.volumePercent),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            FilledTonalIconButton(
                onClick = onToggle,
                colors =
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (row.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription =
                        stringResource(if (row.isPlaying) R.string.pause else R.string.play),
                )
            }
        }
    }
}

private val CORNER_RADIUS = 24.dp

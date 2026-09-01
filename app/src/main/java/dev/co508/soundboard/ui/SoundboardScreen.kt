package dev.co508.soundboard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.co508.soundboard.R
import dev.co508.soundboard.audio.PlaybackService
import dev.co508.soundboard.data.Sound
import dev.co508.soundboard.ui.components.DrawerScaffold
import dev.co508.soundboard.ui.components.RenameDialog
import dev.co508.soundboard.ui.components.ReorderableSoundList
import dev.co508.soundboard.ui.components.SoundRow
import dev.co508.soundboard.ui.components.VolumeDialDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundboardScreen(
    onOpenDrawer: () -> Unit,
    viewModel: SoundboardViewModel = viewModel(factory = SoundboardViewModel.Factory),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val anyPlaying by viewModel.anyPlaying.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Which sound each transient piece of UI is about, or null when closed.
    var volumeTarget by remember { mutableStateOf<Sound?>(null) }
    var optionsTarget by remember { mutableStateOf<Sound?>(null) }
    var deleteTarget by remember { mutableStateOf<Sound?>(null) }
    var renameTarget by remember { mutableStateOf<Sound?>(null) }

    // Rearrange mode swaps the whole list for a drag-reorderable one; see
    // `ReorderableSoundList` for why it's a separate row rather than teaching
    // long-press to mean two different things.
    var rearranging by remember { mutableStateOf(false) }

    // ACTION_OPEN_DOCUMENT, not GET_CONTENT: only the former yields a URI whose
    // read permission can be persisted across reboots. The "multiple" variant
    // sets EXTRA_ALLOW_MULTIPLE so a whole folder of ambience can be added at
    // once; it returns an empty list when the user backs out.
    val pickSounds =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            viewModel.add(uris)
        }

    DrawerScaffold(
        titleRes = if (rearranging) R.string.rearrange_title else R.string.app_name,
        onOpenDrawer = onOpenDrawer,
        actions = {
            if (rows.isNotEmpty()) {
                IconButton(onClick = { rearranging = !rearranging }) {
                    Icon(
                        imageVector = if (rearranging) Icons.Filled.Done else Icons.AutoMirrored.Filled.Sort,
                        contentDescription =
                            stringResource(if (rearranging) R.string.rearrange_done else R.string.rearrange_start),
                    )
                }
            }
            if (anyPlaying) {
                IconButton(onClick = viewModel::pauseAll) {
                    Icon(Icons.Filled.StopCircle, contentDescription = stringResource(R.string.stop_all))
                }
            }
        },
        floatingActionButton = {
            if (!rearranging) {
                FloatingActionButton(onClick = { pickSounds.launch(arrayOf(AUDIO_MIME_TYPE)) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_sound))
                }
            }
        },
    ) { padding ->
        if (rows.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else if (rearranging) {
            ReorderableSoundList(
                rows = rows,
                onReordered = viewModel::commitOrder,
                onEdit = { renameTarget = it },
                onDelete = { deleteTarget = it },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(rows, key = { it.sound.id }) { row ->
                    SoundRow(
                        row = row,
                        onToggle = {
                            val wasPlaying = row.isPlaying
                            viewModel.toggle(row.sound)
                            // Promote to a foreground service so the mix keeps
                            // running once the app is backgrounded.
                            if (!wasPlaying) PlaybackService.start(context)
                        },
                        onVolumeClick = { volumeTarget = row.sound },
                        onLongPress = { optionsTarget = row.sound },
                    )
                }
            }
        }
    }

    volumeTarget?.let { sound ->
        VolumeDialDialog(
            sound = sound,
            onPreview = { viewModel.previewVolume(sound.id, it) },
            onCommit = { viewModel.commitVolume(sound.id, it) },
            onDismiss = { volumeTarget = null },
        )
    }

    optionsTarget?.let { sound ->
        ModalBottomSheet(onDismissRequest = { optionsTarget = null }) {
            ListItem(headlineContent = { Text(sound.name) })
            ListItem(
                headlineContent = { Text(stringResource(R.string.edit)) },
                leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            optionsTarget = null
                            renameTarget = sound
                        },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete)) },
                leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            optionsTarget = null
                            deleteTarget = sound
                        },
            )
        }
    }

    renameTarget?.let { sound ->
        RenameDialog(
            sound = sound,
            onRename = { viewModel.rename(sound.id, it) },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { sound ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_title, sound.name)) },
            text = { Text(stringResource(R.string.delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(sound.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp),
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private const val AUDIO_MIME_TYPE = "audio/*"

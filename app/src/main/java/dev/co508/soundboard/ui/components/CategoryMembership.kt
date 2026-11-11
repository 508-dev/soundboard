package dev.co508.soundboard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.co508.soundboard.R
import dev.co508.soundboard.data.Category
import dev.co508.soundboard.data.Sound

/**
 * Checklist of every category, for one sound. Reached by long-pressing a row.
 *
 * Ticks are held locally and written once on Save — the same preview/commit
 * split the volume dial uses, so ticking four boxes is one DataStore write
 * rather than four. [initialSelection] is a snapshot taken when the dialog
 * opened; a category deleted underneath it simply isn't in [categories]
 * any more and drops out of the committed set.
 */
@Composable
fun SoundCategoriesDialog(
    sound: Sound,
    categories: List<Category>,
    initialSelection: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onCreateCategory: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(sound.id) { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sound_categories_title)) },
        text = {
            if (categories.isEmpty()) {
                Text(stringResource(R.string.sound_categories_none))
            } else {
                Column(
                    modifier = Modifier.heightIn(max = LIST_MAX_HEIGHT).verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = sound.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    categories.forEach { category ->
                        CheckRow(
                            label = category.name,
                            checked = category.id in selected,
                            onToggle = {
                                selected =
                                    if (category.id in selected) selected - category.id else selected + category.id
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (categories.isEmpty()) {
                TextButton(onClick = {
                    onDismiss()
                    onCreateCategory()
                }) { Text(stringResource(R.string.category_new)) }
            } else {
                TextButton(onClick = {
                    onConfirm(selected)
                    onDismiss()
                }) { Text(stringResource(R.string.rename_save)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Multi-select of sounds already on the board that aren't in this category yet.
 *
 * The counterpart to the per-sound checklist: filling a new "Space" category
 * from thirty existing sounds one long-press at a time would be absurd.
 * [candidates] is snapshotted when the dialog opens, and
 * `SoundLibrary.withSoundsInCategory` drops ids that stopped existing, so a
 * stale pick degrades to a smaller add rather than a dangling reference.
 */
@Composable
fun AddFromLibraryDialog(
    candidates: List<Sound>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_from_library_title)) },
        text = {
            if (candidates.isEmpty()) {
                Text(stringResource(R.string.add_from_library_empty))
            } else {
                Column(
                    modifier = Modifier.heightIn(max = LIST_MAX_HEIGHT).verticalScroll(rememberScrollState()),
                ) {
                    candidates.forEach { sound ->
                        CheckRow(
                            label = sound.name,
                            checked = sound.id in selected,
                            onToggle = {
                                selected = if (sound.id in selected) selected - sound.id else selected + sound.id
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(candidates.map { it.id }.filter { it in selected })
                    onDismiss()
                },
                enabled = selected.isNotEmpty(),
            ) { Text(stringResource(R.string.add_from_library_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** A checkbox whose whole row is the tap target, as elsewhere in this app. */
@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickableItem(onToggle),
    )
}

private val LIST_MAX_HEIGHT = 320.dp

package dev.co508.soundboard.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PlaylistRemove
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
import androidx.compose.runtime.LaunchedEffect
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
import dev.co508.soundboard.data.Category
import dev.co508.soundboard.data.Sound
import dev.co508.soundboard.ui.components.AddFromLibraryDialog
import dev.co508.soundboard.ui.components.CategoryOptionsSheet
import dev.co508.soundboard.ui.components.CategoryPickerSheet
import dev.co508.soundboard.ui.components.CategoryTitle
import dev.co508.soundboard.ui.components.DrawerScaffold
import dev.co508.soundboard.ui.components.NameDialog
import dev.co508.soundboard.ui.components.ReorderableSoundList
import dev.co508.soundboard.ui.components.SoundCategoriesDialog
import dev.co508.soundboard.ui.components.SoundRow
import dev.co508.soundboard.ui.components.VolumeDialDialog
import dev.co508.soundboard.ui.components.clickableItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundboardScreen(
    onOpenDrawer: () -> Unit,
    viewModel: SoundboardViewModel = viewModel(factory = SoundboardViewModel.Factory),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val anyPlaying by viewModel.anyPlaying.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val allSoundsCount by viewModel.allSoundsCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Which sound each transient piece of UI is about, or null when closed.
    var volumeTarget by remember { mutableStateOf<Sound?>(null) }
    var optionsTarget by remember { mutableStateOf<Sound?>(null) }
    var deleteTarget by remember { mutableStateOf<Sound?>(null) }
    var renameTarget by remember { mutableStateOf<Sound?>(null) }
    var categoriesTarget by remember { mutableStateOf<Sound?>(null) }

    // Category chrome: the switcher, its long-press options, and the dialogs
    // they open.
    var pickerOpen by remember { mutableStateOf(false) }
    var categoryOptions by remember { mutableStateOf<Category?>(null) }
    var categoryRenameTarget by remember { mutableStateOf<Category?>(null) }
    var categoryDeleteTarget by remember { mutableStateOf<Category?>(null) }
    var creatingCategory by remember { mutableStateOf(false) }

    // Non-null when the create dialog was opened from a sound's checklist: the
    // new category gets that sound, and the board stays put. Creating from the
    // switcher instead switches to the new category, because there the point
    // was to go and fill it.
    var creatingCategoryFor by remember { mutableStateOf<Sound?>(null) }
    var addFromLibrary by remember { mutableStateOf(false) }

    // Rearrange mode swaps the whole list for a drag-reorderable one; see
    // `ReorderableSoundList` for why it's a separate row rather than teaching
    // long-press to mean two different things.
    var rearranging by remember { mutableStateOf(false) }

    // Deleting the last sound from rearrange mode would otherwise strand the
    // user: the Done toggle only shows while there are rows, and the add FAB
    // is hidden while rearranging. Drop back to the board instead. Switching
    // to an empty category has the same effect, which is also what we want —
    // there is nothing to rearrange there.
    LaunchedEffect(rows.isEmpty()) {
        if (rows.isEmpty()) rearranging = false
    }

    // "Add from library" is about one category; switching away from it (or
    // deleting it) makes the open dialog meaningless.
    LaunchedEffect(selectedCategoryId) { addFromLibrary = false }

    // ACTION_OPEN_DOCUMENT, not GET_CONTENT: only the former yields a URI whose
    // read permission can be persisted across reboots. The "multiple" variant
    // sets EXTRA_ALLOW_MULTIPLE so a whole folder of ambience can be added at
    // once; it returns an empty list when the user backs out. Picked files land
    // in the visible category, if any — see `SoundboardViewModel.add`.
    val pickSounds =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            viewModel.add(uris)
        }

    DrawerScaffold(
        onOpenDrawer = onOpenDrawer,
        title = {
            if (rearranging) {
                Text(stringResource(R.string.rearrange_title))
            } else {
                CategoryTitle(
                    name = selectedCategory?.name ?: stringResource(R.string.category_all),
                    onClick = { pickerOpen = true },
                )
            }
        },
        actions = {
            if (selectedCategoryId != null && !rearranging) {
                IconButton(onClick = { addFromLibrary = true }) {
                    Icon(
                        Icons.Filled.LibraryAdd,
                        contentDescription = stringResource(R.string.add_from_library),
                    )
                }
            }
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
            EmptyState(
                inCategory = selectedCategoryId != null,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else if (rearranging) {
            ReorderableSoundList(
                rows = rows,
                onReordered = viewModel::commitOrder,
                onEdit = { renameTarget = it },
                onDelete = { sound ->
                    val categoryId = selectedCategoryId
                    if (categoryId == null) {
                        deleteTarget = sound
                    } else {
                        viewModel.removeFromCategory(categoryId, sound.id)
                    }
                },
                categoryScoped = selectedCategoryId != null,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = BOARD_CONTENT_PADDING,
                        top = BOARD_CONTENT_PADDING,
                        end = BOARD_CONTENT_PADDING,
                        bottom = BOARD_CONTENT_PADDING + FAB_SIZE + FAB_BOTTOM_MARGIN,
                    ),
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

    if (pickerOpen) {
        CategoryPickerSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            allSoundsCount = allSoundsCount,
            onSelect = {
                viewModel.selectCategory(it)
                pickerOpen = false
            },
            onCreate = {
                pickerOpen = false
                creatingCategory = true
            },
            onManage = {
                pickerOpen = false
                categoryOptions = it
            },
            onDismiss = { pickerOpen = false },
        )
    }

    categoryOptions?.let { category ->
        CategoryOptionsSheet(
            category = category,
            onRename = {
                categoryOptions = null
                categoryRenameTarget = category
            },
            onDelete = {
                categoryOptions = null
                categoryDeleteTarget = category
            },
            onDismiss = { categoryOptions = null },
        )
    }

    if (creatingCategory) {
        NameDialog(
            key = CREATE_CATEGORY_KEY,
            initialName = "",
            titleRes = R.string.category_new_title,
            confirmRes = R.string.add_from_library_confirm,
            onConfirm = { viewModel.createCategory(it, creatingCategoryFor?.id) },
            onDismiss = {
                creatingCategory = false
                creatingCategoryFor = null
            },
        )
    }

    categoryRenameTarget?.let { category ->
        NameDialog(
            key = category.id,
            initialName = category.name,
            titleRes = R.string.category_rename_title,
            onConfirm = { viewModel.renameCategory(category.id, it) },
            onDismiss = { categoryRenameTarget = null },
        )
    }

    categoryDeleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryDeleteTarget = null },
            title = { Text(stringResource(R.string.category_delete_title, category.name)) },
            text = { Text(stringResource(R.string.category_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category.id)
                    categoryDeleteTarget = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { categoryDeleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    selectedCategoryId?.takeIf { addFromLibrary }?.let { categoryId ->
        AddFromLibraryDialog(
            // Re-snapshotted when the category's contents change, so sounds
            // just added drop out of the candidate list rather than offering a
            // second, no-op add.
            candidates = remember(categoryId, rows.size) { viewModel.soundsNotIn(categoryId) },
            onConfirm = { viewModel.addToCategory(categoryId, it) },
            onDismiss = { addFromLibrary = false },
        )
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
        val categoryId = selectedCategoryId
        ModalBottomSheet(onDismissRequest = { optionsTarget = null }) {
            ListItem(headlineContent = { Text(sound.name) })
            ListItem(
                headlineContent = { Text(stringResource(R.string.edit)) },
                leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                modifier =
                    Modifier.clickableItem {
                        optionsTarget = null
                        renameTarget = sound
                    },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.sound_categories)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                modifier =
                    Modifier.clickableItem {
                        optionsTarget = null
                        categoriesTarget = sound
                    },
            )
            // Inside a category there are two different removals, and conflating
            // them is the easy mistake here: taking a sound out of this grouping
            // is cheap and reversible, deleting it is neither.
            if (categoryId != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.remove_from_category)) },
                    leadingContent = { Icon(Icons.Filled.PlaylistRemove, contentDescription = null) },
                    modifier =
                        Modifier.clickableItem {
                            optionsTarget = null
                            viewModel.removeFromCategory(categoryId, sound.id)
                        },
                )
            }
            ListItem(
                headlineContent = {
                    Text(stringResource(if (categoryId == null) R.string.delete else R.string.delete_sound))
                },
                leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                modifier =
                    Modifier.clickableItem {
                        optionsTarget = null
                        deleteTarget = sound
                    },
            )
        }
    }

    categoriesTarget?.let { sound ->
        SoundCategoriesDialog(
            sound = sound,
            categories = categories.map { it.category },
            initialSelection = remember(sound.id) { viewModel.categoryIdsOf(sound.id) },
            onConfirm = { viewModel.setSoundCategories(sound.id, it) },
            onCreateCategory = {
                creatingCategoryFor = sound
                creatingCategory = true
            },
            onDismiss = { categoriesTarget = null },
        )
    }

    renameTarget?.let { sound ->
        NameDialog(
            key = sound.id,
            initialName = sound.name,
            titleRes = R.string.rename_title,
            onConfirm = { viewModel.rename(sound.id, it) },
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
private fun EmptyState(
    inCategory: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp),
    ) {
        Text(
            text = stringResource(if (inCategory) R.string.empty_category_title else R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(if (inCategory) R.string.empty_category_body else R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private const val AUDIO_MIME_TYPE = "audio/*"
private const val CREATE_CATEGORY_KEY = "create-category"
private val BOARD_CONTENT_PADDING = 16.dp
private val FAB_SIZE = 56.dp
private val FAB_BOTTOM_MARGIN = 16.dp

package dev.co508.soundboard.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.co508.soundboard.R
import dev.co508.soundboard.data.Category
import dev.co508.soundboard.ui.CategoryState

/**
 * The top bar's title: the current category's name plus a caret, tappable to
 * open [CategoryPickerSheet].
 *
 * This replaces the app name in the bar. Showing where you are beats showing
 * what app you are in — the launcher and the About page already say that — and
 * "508.dev Soundboard" plus a caret plus the bar's two action icons does not
 * fit a narrow phone without ellipsizing the title to nothing.
 */
@Composable
fun CategoryTitle(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(TITLE_CORNER_RADIUS))
                .clickable(onClick = onClick, onClickLabel = stringResource(R.string.category_switch))
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = stringResource(R.string.category_switch),
        )
    }
}

/**
 * The category switcher: "All sounds", every category, and a way to add one.
 *
 * A sheet rather than a dropdown menu because its rows need a long-press —
 * `DropdownMenuItem` owns its own click and can't carry one — and because the
 * app already uses long-press-opens-a-sheet for a row's options, so category
 * management reads the same way as sound management. A long list scrolls.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<CategoryState>,
    selectedCategoryId: String?,
    allSoundsCount: Int,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onManage: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.category_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            PickerRow(
                icon = Icons.Filled.GraphicEq,
                name = stringResource(R.string.category_all),
                count = allSoundsCount,
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) },
                onLongClick = null,
            )
            categories.forEach { state ->
                PickerRow(
                    icon = Icons.AutoMirrored.Filled.Label,
                    name = state.category.name,
                    count = state.soundCount,
                    selected = selectedCategoryId == state.category.id,
                    onClick = { onSelect(state.category.id) },
                    onLongClick = { onManage(state.category) },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.category_new)) },
                leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.clickableItem(onCreate),
            )
            if (categories.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.category_manage_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Rename/delete for one category, opened by long-pressing it in the picker.
 *
 * Deleting a category never deletes sounds, which the confirmation copy says
 * outright — the same promise the sound delete dialog makes about files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryOptionsSheet(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(headlineContent = { Text(category.name) })
        ListItem(
            headlineContent = { Text(stringResource(R.string.category_rename)) },
            leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
            modifier = Modifier.clickableItem(onRename),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.category_delete)) },
            leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
            modifier = Modifier.clickableItem(onDelete),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickerRow(
    icon: ImageVector,
    name: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    ListItem(
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(pluralStringResource(R.plurals.category_sound_count, count, count))
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.category_selected),
                )
            }
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    )
}

private val TITLE_CORNER_RADIUS = 8.dp

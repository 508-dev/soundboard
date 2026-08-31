package dev.co508.soundboard.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.co508.soundboard.R
import dev.co508.soundboard.data.Sound
import dev.co508.soundboard.ui.SoundRowState
import kotlinx.coroutines.launch

/**
 * The board as a drag-reorderable list, used while rearrange mode is active.
 *
 * Deliberately not the same row as [SoundRow]: there is no tap-to-play or
 * long-press-to-delete here, only a drag handle and an explicit delete icon, so
 * dragging never has to be disambiguated from another gesture on the same row.
 *
 * [rows] is the source of truth; [onReordered] is called once, with the whole
 * board's ids in their new order, when a drag or a sort shortcut completes —
 * matching the existing preview/commit split the volume dial uses, so each is
 * one DataStore write, not dozens.
 */
@Composable
fun ReorderableSoundList(
    rows: List<SoundRowState>,
    onReordered: (List<String>) -> Unit,
    onDelete: (Sound) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Kept here rather than hoisted: rearrange mode is a fresh "table view"
    // each time it opens, starting at the top of the board, and every action
    // that needs to scroll (a swap, a sort chip) lives in this component.
    val listState = rememberLazyListState()

    // Local working order so a drag can reshuffle rows immediately, without
    // waiting for a DataStore round-trip. Resynced from `rows` whenever nothing
    // is being dragged, so persisted changes from elsewhere still show up.
    var localOrder by remember { mutableStateOf(rows.map { it.sound.id }) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(rows) {
        if (draggingId == null) localOrder = rows.map { it.sound.id }
    }

    val byId = rows.associateBy { it.sound.id }
    val orderedRows = localOrder.mapNotNull { byId[it] }

    // LazyColumn anchors its scroll position to the first visible item's *key*
    // when the item list changes (Foundation's
    // `updateScrollPositionIfTheFirstItemWasMoved`): whatever row was on top
    // stays on top, even if it moved. Both edits below have to beat that
    // anchor — a drag swap that moves the anchored row, and a sort that wants
    // the top of the new order. An explicit `scrollToItem` clears the stored
    // anchor key (`requestPositionAndForgetLastKnownKey`), so requesting the
    // desired position in the same frame as the order change wins. Anything
    // later (a DataStore round-trip, an animation) lands after a measure has
    // re-populated the key and the anchor restore fires instead — that was
    // both the "viewport drags along with the first row" and the "sort jumps
    // to a random place" bugs.
    fun pinScrollPosition() {
        val index = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        scope.launch { listState.scrollToItem(index, offset) }
    }

    /**
     * Applies a one-shot sort: the working order changes and the list snaps to
     * the top in the same frame (see above), then the new order is persisted
     * once — the same as a completed drag, so the user can drag again from
     * whatever order a chip produces. "Playing" is the one sort that couldn't
     * be a pure `SoundLibrary` function, since playback status is live engine
     * state; it's computed here from [SoundRowState], which already carries it.
     */
    fun <T : Comparable<T>> sortBy(
        descending: Boolean = false,
        selector: (SoundRowState) -> T,
    ) {
        if (draggingId != null) return
        val sorted =
            (if (descending) rows.sortedByDescending(selector) else rows.sortedBy(selector))
                .map { it.sound.id }
        localOrder = sorted
        onReordered(sorted)
        // Snap rather than animate: the contents are simultaneously reordering,
        // so animating through the old order reads as chaos.
        scope.launch { listState.scrollToItem(0) }
    }

    Column(modifier = modifier) {
        SortChipsRow(
            onSortByName = { sortBy { it.sound.name.lowercase() } },
            onSortByVolume = { sortBy(descending = true) { it.sound.volumePercent } },
            onSortByPlaying = { sortBy(descending = true) { if (it.isPlaying) 1 else 0 } },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(orderedRows, key = { it.sound.id }) { row ->
                val isDragging = row.sound.id == draggingId

                ReorderableSoundRow(
                    row = row,
                    onDelete = { onDelete(row.sound) },
                    onDragStart = {
                        draggingId = row.sound.id
                        dragOffset = 0f
                    },
                    onDrag = { deltaY ->
                        dragOffset += deltaY

                        val draggedInfo =
                            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == row.sound.id }
                        val draggedCenter = draggedInfo?.let { it.offset + it.size / 2f + dragOffset }

                        val target =
                            draggedCenter?.let { center ->
                                listState.layoutInfo.visibleItemsInfo.firstOrNull { other ->
                                    other.key != row.sound.id &&
                                        center >= other.offset &&
                                        center <= other.offset + other.size
                                }
                            }

                        if (draggedInfo != null && target != null) {
                            val fromIndex = localOrder.indexOf(row.sound.id)
                            val toIndex = localOrder.indexOf(target.key as String)
                            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                // Keep the row's on-screen position continuous across the
                                // swap: its slot just moved from draggedInfo.offset to
                                // roughly target.offset, so shrink the finger-relative
                                // offset by exactly that amount.
                                dragOffset += (draggedInfo.offset - target.offset).toFloat()
                                localOrder =
                                    localOrder.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                                // Without this, a swap involving the anchored (first
                                // visible) row — e.g. dragging the board's first row
                                // down — makes LazyColumn scroll the whole viewport to
                                // follow it. The pin is position-wise a no-op; it only
                                // disarms the anchor.
                                pinScrollPosition()
                            }
                        }
                    },
                    onDragEnd = {
                        draggingId = null
                        dragOffset = 0f
                        onReordered(localOrder)
                    },
                    modifier =
                        Modifier
                            // Animating the dragged item's own slot fights the manual
                            // translationY below — the two disagree about where it is
                            // mid-swap, which is what read as "jumps back to its start
                            // position before landing". Only the settling rows animate;
                            // the dragged one is 1:1 with the finger the whole time.
                            .let { if (isDragging) it else it.animateItem() }
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                )
            }
        }
    }
}

/** One-shot sort actions shown at the top of [ReorderableSoundList]. */
@Composable
private fun SortChipsRow(
    onSortByName: () -> Unit,
    onSortByVolume: () -> Unit,
    onSortByPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.sort_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        AssistChip(onClick = onSortByName, label = { Text(stringResource(R.string.sort_by_name)) })
        AssistChip(onClick = onSortByVolume, label = { Text(stringResource(R.string.sort_by_volume)) })
        AssistChip(onClick = onSortByPlaying, label = { Text(stringResource(R.string.sort_by_playing)) })
    }
}

@Composable
private fun ReorderableSoundRow(
    row: SoundRowState,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        // Distinct from both SoundRow colors (surfaceVariant idle, primaryContainer
        // playing) so rearrange mode reads as a different surface, not a subdued
        // version of the normal list.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = stringResource(R.string.reorder_handle, row.sound.name),
                modifier =
                    Modifier
                        .size(48.dp)
                        .pointerInput(row.sound.id) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = onDragEnd,
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                },
                            )
                        },
            )
            Text(
                text = row.sound.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

package dev.co508.soundboard.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.co508.soundboard.R
import dev.co508.soundboard.data.Sound

/**
 * Modal text field for renaming one sound.
 *
 * The field opens focused with its text fully selected — same "ready to
 * retype" convention as a system rename dialog — so a quick rename is one tap
 * plus typing, not a manual select-all first. [onRename] only fires with a
 * trimmed, non-blank result; [SoundLibrary.renamed] enforces the same rule
 * server-side, but checking here too means the Save button can simply be
 * disabled instead of silently doing nothing.
 */
@Composable
fun RenameDialog(
    sound: Sound,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by
        remember(sound.id) {
            mutableStateOf(TextFieldValue(sound.name, selection = TextRange(0, sound.name.length)))
        }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(sound.id) { focusRequester.requestFocus() }

    fun commit() {
        val trimmed = text.text.trim()
        if (trimmed.isNotEmpty()) onRename(trimmed)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.rename_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = ::commit, enabled = text.text.isNotBlank()) {
                Text(stringResource(R.string.rename_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

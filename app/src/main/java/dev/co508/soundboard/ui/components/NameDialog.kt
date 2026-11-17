package dev.co508.soundboard.ui.components

import androidx.annotation.StringRes
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

/**
 * Modal text field for naming one thing — a sound or a category.
 *
 * The field opens focused with its text fully selected — same "ready to
 * retype" convention as a system rename dialog — so a quick rename is one tap
 * plus typing, not a manual select-all first. [onConfirm] only fires with a
 * trimmed, non-blank result; the `SoundLibrary` mutators enforce the same rule,
 * but checking here too means the confirm button can simply be disabled instead
 * of silently doing nothing.
 *
 * [key] is what the field's contents are remembered against: pass the id of the
 * thing being named so reopening the dialog for a *different* target starts
 * from that target's name rather than the previous one's edits. A creation
 * dialog, which has no id, can pass any stable constant.
 */
@Composable
fun NameDialog(
    key: Any,
    initialName: String,
    @StringRes titleRes: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    @StringRes confirmRes: Int = R.string.rename_save,
    @StringRes labelRes: Int = R.string.rename_label,
) {
    var text by
        remember(key) {
            mutableStateOf(TextFieldValue(initialName, selection = TextRange(0, initialName.length)))
        }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(key) { focusRequester.requestFocus() }

    fun commit() {
        val trimmed = text.text.trim()
        if (trimmed.isNotEmpty()) onConfirm(trimmed)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(labelRes)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = ::commit, enabled = text.text.isNotBlank()) {
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

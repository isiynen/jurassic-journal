package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Numeric input dialog with inline validation: out-of-range input shows an
 * error and disables OK rather than being silently clamped.
 */
@Composable
fun NumberInputDialog(
    title: String,
    current: Int,
    min: Int,
    max: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialText = current.toString()
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(0, initialText.length)))
    }
    val parsed  = fieldValue.text.toIntOrNull()
    val isValid = parsed != null && parsed in min..max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = {
            OutlinedTextField(
                value       = fieldValue,
                onValueChange = { new -> if (new.text.all { it.isDigit() }) fieldValue = new },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (isValid) onConfirm(parsed!!) }),
                singleLine   = true,
                isError      = !isValid,
                supportingText = { Text("$min – $max") },
            )
        },
        confirmButton = { TextButton(onClick = { if (isValid) onConfirm(parsed!!) }, enabled = isValid) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** [NumberInputDialog] for Long-ranged values (wallet amounts). */
@Composable
fun LongInputDialog(
    title: String,
    current: Long,
    min: Long = 0L,
    max: Long = 2_100_000_000L,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialText = current.toString()
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(0, initialText.length)))
    }
    val parsed  = fieldValue.text.toLongOrNull()
    val isValid = parsed != null && parsed in min..max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = {
            OutlinedTextField(
                value       = fieldValue,
                onValueChange = { new -> if (new.text.all { it.isDigit() }) fieldValue = new },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (isValid) onConfirm(parsed!!) }),
                singleLine   = true,
                isError      = !isValid,
                supportingText = { Text("$min – $max") },
            )
        },
        confirmButton = { TextButton(onClick = { if (isValid) onConfirm(parsed!!) }, enabled = isValid) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Single-line name prompt used for profiles and teams. */
@Composable
fun NameInputDialog(
    title: String,
    initial: String = "",
    confirmLabel: String,
    fieldLabel: String = "Name",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(fieldLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * `−  value  +` control. Buttons repeat while held; tapping the value opens
 * [NumberInputDialog] for direct entry. Every stepper in the app is this one.
 */
@Composable
fun NumberStepper(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dialogTitle: String = "Enter value",
    valueText: (Int) -> String = { it.toString() },
    valueStyle: TextStyle = MaterialTheme.typography.titleMedium,
    valueModifier: Modifier = Modifier.padding(horizontal = 8.dp),
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NumberInputDialog(
            title     = dialogTitle,
            current   = value,
            min       = min,
            max       = max,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        RepeatingButton(
            onClick  = { if (value > min) onValueChange(value - 1) },
            enabled  = enabled && value > min,
            modifier = Modifier.size(32.dp),
        ) { Text("−", style = MaterialTheme.typography.titleMedium) }
        Text(
            text       = valueText(value),
            style      = valueStyle,
            fontWeight = FontWeight.Bold,
            color      = if (enabled) MaterialTheme.colorScheme.onSurface
                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign  = TextAlign.Center,
            modifier   = Modifier
                .clickable(enabled = enabled) { showDialog = true }
                .then(valueModifier),
        )
        RepeatingButton(
            onClick  = { if (value < max) onValueChange(value + 1) },
            enabled  = enabled && value < max,
            modifier = Modifier.size(32.dp),
        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
    }
}

/** [NumberStepper] with a small caption above it; used for the Current/Target pairs on calculator screens. */
@Composable
fun LabeledStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dialogTitle: String = "Enter $label",
    valueText: (Int) -> String = { it.toString() },
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(4.dp))
        NumberStepper(
            value         = value,
            min           = min,
            max           = max,
            onValueChange = onValueChange,
            enabled       = enabled,
            dialogTitle   = dialogTitle,
            valueText     = valueText,
            valueStyle    = valueStyle,
        )
    }
}

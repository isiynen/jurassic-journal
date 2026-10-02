package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sufficienteffort.jurassicjournal.ui.theme.CoinGold

/** Tap-to-edit card row: label on the left, formatted value on the right. */
@Composable
fun ValueRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    JJCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                value,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = valueColor,
            )
        }
    }
}

/** Wallet coins; shared across all calculators for the active profile. */
@Composable
fun CoinsOnHandRow(value: Long, onValueChange: (Long) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        LongInputDialog(
            title     = "Coins on Hand",
            current   = value,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
    ValueRow(
        label      = "Coins on Hand",
        value      = "%,d coins".format(value),
        valueColor = CoinGold,
        onClick    = { showDialog = true },
    )
}

/** DNA held for one dino, capped at the rarity's inventory limit. */
@Composable
fun DnaOnHandRow(
    value: Int,
    maxDna: Int,
    onValueChange: (Int) -> Unit,
    label: String = "DNA on Hand",
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        NumberInputDialog(
            title     = label,
            current   = value,
            min       = 0,
            max       = maxDna,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
    ValueRow(
        label   = label,
        value   = "%,d DNA".format(value),
        onClick = { showDialog = true },
    )
}

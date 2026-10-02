package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Standard 16dp side margin used by every card-in-a-list on the app's screens. */
fun Modifier.cardMargin(vertical: Dp = 4.dp): Modifier =
    padding(horizontal = 16.dp, vertical = vertical).fillMaxWidth()

/**
 * The app's one card style: 12dp corners, surface container, 2dp elevation.
 * Pass [onClick] for a ripple-clipped clickable card.
 */
@Composable
fun JJCard(
    modifier: Modifier = Modifier.cardMargin(),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 2.dp,
    cornerRadius: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevationSpec = CardDefaults.cardElevation(defaultElevation = elevation)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevationSpec,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevationSpec,
            content = content,
        )
    }
}

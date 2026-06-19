package com.jurassicjournal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = JWAAmber,
    onPrimary = JWACharcoal,
    primaryContainer = JWADarkGreen,
    onPrimaryContainer = JWAOffWhite,
    secondary = JWAMidGreen,
    onSecondary = JWAOffWhite,
    background = JWADarkSurface,
    onBackground = JWAOffWhite,
    surface = JWACharcoal,
    onSurface = JWAOffWhite,
    surfaceVariant = JWADarkGreen,
    onSurfaceVariant = JWAOffWhite,
)

@Composable
fun JurassicJournalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

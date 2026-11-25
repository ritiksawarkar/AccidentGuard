package com.accidentguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = M3Primary,
    onPrimary = M3OnPrimary,
    primaryContainer = M3PrimaryContainer,
    onPrimaryContainer = M3OnPrimaryContainer,
    secondary = M3Secondary,
    onSecondary = M3OnSecondary,
    secondaryContainer = M3SecondaryContainer,
    onSecondaryContainer = M3OnSecondaryContainer,
    background = M3Background,
    surface = M3Surface,
    onBackground = M3OnBackground,
    onSurface = M3OnSurface,
    surfaceVariant = M3SurfaceVariant,
    onSurfaceVariant = M3OnSurfaceVariant,
    outline = M3Outline,
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueVariant,
    onPrimary = SurfaceCard,
    secondary = SecondaryTeal,
    background = TextPrimary,
    surface = Color(0xFF1E1E1E),
    onBackground = SurfaceCard,
    onSurface = SurfaceCard
)

@Composable
fun AccidentGuardTheme(
    darkTheme: Boolean = false, // Force light theme by default
    content: @Composable () -> Unit
) {
    // Always use LightColorScheme for AccidentGuard to ensure a professional emergency tech look
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

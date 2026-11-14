package com.example.multipromptchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightSurface,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightAccentDeep,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSecondarySoft,
    onSecondaryContainer = LightSecondary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightError,
    onError = LightSurface,
    errorContainer = LightErrorSoft,
    onErrorContainer = LightError,
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBackground,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkAccentDeep,
    secondary = DarkSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSecondarySoft,
    onSecondaryContainer = DarkSecondary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError,
    onError = DarkBackground,
    errorContainer = DarkErrorSoft,
    onErrorContainer = DarkError,
)

/**
 * Single source of truth for colours. Nothing outside this file hard-codes a colour,
 * so light/dark (and a future brand re-skin) only touch this file.
 */
@Composable
fun MultipromptChatTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HarmonyTypography,
        shapes = HarmonyShapes,
        content = content,
    )
}

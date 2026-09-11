package com.mindfulscreen.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blue500,
    onPrimary = LightSurface,
    secondary = Teal,
    background = LightBg,
    surface = LightSurface,
    error = Red,
)

private val DarkColors = darkColorScheme(
    primary = Blue500,
    secondary = Teal,
    background = DarkBg,
    surface = DarkSurface,
    error = Red,
)

@Composable
fun MindfulScreenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

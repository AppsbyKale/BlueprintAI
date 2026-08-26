package com.example.blueprintai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkGrayscaleColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = LightGray,
    onSecondary = Black,
    tertiary = Gray,
    onTertiary = White,
    background = Black,
    onBackground = OffWhite,
    surface = DarkGray,
    onSurface = OffWhite,
    surfaceVariant = Gray,
    onSurfaceVariant = OffWhite,
    outline = LightGray
)

@Composable
fun BlueprintAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkGrayscaleColorScheme,
        typography = Typography,
        content = content
    )
}

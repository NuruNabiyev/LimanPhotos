package com.limanphotos.limandoc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import theme.LightTheme

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF05486E),
    onPrimary = LightTheme.gray_14,
    primaryContainer = LightTheme.blue_12,
    onPrimaryContainer = LightTheme.blue_1,
    secondary = LightTheme.gray_6, // Secondary color, icons, status
    onSecondary = LightTheme.gray_14, // Text on a secondary color
    secondaryContainer = LightTheme.blue_11, // Container for secondary color
    onSecondaryContainer = LightTheme.blue_2, // Text on a container with secondary color
    tertiary = LightTheme.green_4, // Tertiary color, icons, buttons
    onTertiary = LightTheme.gray_14, // Text on a tertiary color
    tertiaryContainer = LightTheme.green_11, // Container for tertiary color
    onTertiaryContainer = LightTheme.green_1, // Text on a container with tertiary color
    error = LightTheme.red_4, // Error color
    onError = LightTheme.gray_14, // Text on an error color
    errorContainer = LightTheme.red_10, // Container for error color
    onErrorContainer = LightTheme.red_1, // Text on a container with error color
    background = LightTheme.gray_14, // Main background
    onBackground = LightTheme.gray_1, // Text on the main background
    surface = LightTheme.gray_13, // Surface color (cards, modals)
    onSurface = LightTheme.gray_1, // Text on the surface
    surfaceVariant = LightTheme.gray_10, // Color option for surface
    onSurfaceVariant = LightTheme.gray_3, // Text on the surface variant
    outline = LightTheme.gray_12, // Outline color (borders, dividers)
    inverseOnSurface = LightTheme.gray_14, // Inverse text on the surface
    inverseSurface = LightTheme.gray_1, // Inverse surface color
    inversePrimary = LightTheme.blue_3, // Inverse primary color
    surfaceTint = LightTheme.blue_5, // Tint for surface

    surfaceContainer = LightTheme.gray_13,
    surfaceDim = LightTheme.gray_12,
    surfaceBright = LightTheme.gray_14,
    surfaceContainerLowest = LightTheme.gray_14,
    surfaceContainerLow = LightTheme.gray_13,
    surfaceContainerHigh = LightTheme.gray_12,
    surfaceContainerHighest = LightTheme.gray_11,

    outlineVariant = LightTheme.gray_9, // Color option for outline
    scrim = LightTheme.gray_1 // Scrim color for dialogs and modals
)

@Composable
internal fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(0.dp)
        ),
        content = { Surface(content = content) }
    )
}

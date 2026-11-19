package io.github.dijksys.quipu.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = QuipuGreen,
    onPrimary = QuipuSurface,
    primaryContainer = QuipuGreenContainer,
    onPrimaryContainer = QuipuGreenDark,
    inversePrimary = QuipuMint,
    secondary = QuipuSand,
    onSecondary = QuipuSurface,
    secondaryContainer = QuipuSandContainer,
    onSecondaryContainer = QuipuInk,
    tertiary = ColorTeal,
    onTertiary = QuipuSurface,
    tertiaryContainer = ColorTealContainer,
    onTertiaryContainer = QuipuGreenDark,
    background = QuipuPaper,
    onBackground = QuipuInk,
    surface = QuipuSurface,
    onSurface = QuipuInk,
    surfaceVariant = QuipuSurfaceMuted,
    onSurfaceVariant = QuipuInkMuted,
    surfaceTint = QuipuGreen,
    inverseSurface = QuipuInk,
    inverseOnSurface = QuipuPaper,
    error = QuipuError,
    onError = QuipuSurface,
    errorContainer = QuipuErrorContainer,
    onErrorContainer = QuipuError,
    outline = QuipuOutline,
    outlineVariant = QuipuOutlineSoft,
    scrim = QuipuInk,
    surfaceBright = QuipuSurface,
    surfaceDim = QuipuSurfaceMuted,
    surfaceContainerLowest = QuipuSurface,
    surfaceContainerLow = QuipuPaper,
    surfaceContainer = QuipuSurfaceMuted,
    surfaceContainerHigh = QuipuSurfaceMuted,
    surfaceContainerHighest = QuipuOutlineSoft,
)

private val DarkColorScheme = darkColorScheme(
    primary = QuipuMint,
    onPrimary = QuipuGreenDark,
    primaryContainer = QuipuGreenContainerDark,
    onPrimaryContainer = QuipuMint,
    inversePrimary = QuipuGreen,
    secondary = QuipuSandDark,
    onSecondary = QuipuDarkBackground,
    secondaryContainer = QuipuSandContainerDark,
    onSecondaryContainer = QuipuSandDark,
    tertiary = ColorTealDark,
    onTertiary = QuipuDarkBackground,
    tertiaryContainer = ColorTealContainerDark,
    onTertiaryContainer = ColorTealDark,
    background = QuipuDarkBackground,
    onBackground = QuipuDarkInk,
    surface = QuipuDarkSurface,
    onSurface = QuipuDarkInk,
    surfaceVariant = QuipuDarkSurfaceMuted,
    onSurfaceVariant = QuipuDarkInkMuted,
    surfaceTint = QuipuMint,
    inverseSurface = QuipuDarkInk,
    inverseOnSurface = QuipuDarkBackground,
    error = QuipuErrorDark,
    onError = QuipuDarkBackground,
    errorContainer = QuipuErrorContainerDark,
    onErrorContainer = QuipuErrorDark,
    outline = QuipuDarkOutline,
    outlineVariant = QuipuDarkOutlineSoft,
    scrim = QuipuDarkBackground,
    surfaceBright = QuipuDarkSurfaceMuted,
    surfaceDim = QuipuDarkBackground,
    surfaceContainerLowest = QuipuDarkBackground,
    surfaceContainerLow = QuipuDarkSurface,
    surfaceContainer = QuipuDarkSurfaceMuted,
    surfaceContainerHigh = QuipuDarkSurfaceMuted,
    surfaceContainerHighest = QuipuDarkOutlineSoft,
)

@Composable
fun QuipuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = QuipuTypography,
        shapes = QuipuShapes,
        content = content,
    )
}

package com.packapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.packapp.ui.DesignMode
import com.packapp.ui.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme

private val MinimalColors = lightColorScheme(
    primary = SeedGreen,
    onPrimary = WarmGray,
    primaryContainer = SoftGreen,
    onPrimaryContainer = Ink,
    secondary = Moss,
    surface = WarmGray,
    surfaceVariant = Fog,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
    outlineVariant = Stone
)

private val SportColors = lightColorScheme(
    primary = SpeedBlue,
    onPrimary = WarmGray,
    primaryContainer = SkyMist,
    onPrimaryContainer = Ink,
    secondary = PulseOrange,
    surface = WarmGray,
    surfaceVariant = SkyMist,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
    outlineVariant = Stone
)

private val MinimalDarkColors = darkColorScheme(
    primary = SoftGreen,
    onPrimary = Ink,
    primaryContainer = SeedGreen,
    onPrimaryContainer = WarmGray,
    secondary = Moss,
    surface = Ink,
    surfaceVariant = InkElevated,
    onSurface = WarmGray,
    onSurfaceVariant = Fog,
    outlineVariant = InkOutline
)

private val SportDarkColors = darkColorScheme(
    primary = SpeedBlueLight,
    onPrimary = Ink,
    primaryContainer = SpeedBlue,
    onPrimaryContainer = WarmGray,
    secondary = PulseOrange,
    surface = Ink,
    surfaceVariant = InkElevated,
    onSurface = WarmGray,
    onSurfaceVariant = Fog,
    outlineVariant = InkOutline
)

@Composable
fun PackPilotTheme(
    designMode: DesignMode,
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colors = when {
        useDark && designMode == DesignMode.MINIMAL -> MinimalDarkColors
        useDark && designMode == DesignMode.SPORTLICH -> SportDarkColors
        !useDark && designMode == DesignMode.MINIMAL -> MinimalColors
        else -> SportColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

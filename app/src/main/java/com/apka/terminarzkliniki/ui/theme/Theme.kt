package com.apka.terminarzkliniki.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = CyanOnPrimaryDark,
    primaryContainer = CyanPrimaryContainerDark,
    onPrimaryContainer = CyanOnPrimaryContainerDark,

    secondary = CyanSecondaryDark,
    onSecondary = CyanOnSecondaryDark,

    secondaryContainer = CyanSecondaryContainerDark,
    onSecondaryContainer = CyanOnSecondaryContainerDark,

    tertiary = CyanTertiaryDark,
    onTertiary = CyanOnTertiaryDark,

    tertiaryContainer = CyanTertiaryContainerDark,
    onTertiaryContainer = CyanOnTertiaryContainerDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,

    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = CyanOnPrimaryLight,
    primaryContainer = CyanPrimaryContainerLight,
    onPrimaryContainer = CyanOnPrimaryContainerLight,

    secondary = CyanSecondaryLight,
    onSecondary = CyanOnSecondaryLight,

    secondaryContainer = CyanSecondaryContainerLight,
    onSecondaryContainer = CyanOnSecondaryContainerLight,

    tertiary = CyanTertiaryLight,
    onTertiary = CyanOnTertiaryLight,

    tertiaryContainer = CyanTertiaryContainerLight,
    onTertiaryContainer = CyanOnTertiaryContainerLight,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,

    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

@Composable
fun TerminarzKlinikiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Ustawione na false
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

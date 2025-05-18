package com.example.cubespeed.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Enum class representing the available theme types in the app
 */
enum class AppThemeType {
    BLUE, LIGHT, DARK
}

// Blue theme (light)
private val BlueColorScheme = lightColorScheme(
    primary = TimerScreenPrimaryBackground,  // Using TimerScreen background color
    secondary = TimerScreenAccent,    // Using TimerScreen accent color
    tertiary = BlueTertiary,
    background = BlueBackground,
    surface = BlueSurface,
    onPrimary = BlueOnPrimary,
    onSecondary = BlueOnSecondary,
    onTertiary = BlueOnTertiary,
    onPrimaryContainer = TimerScreenBackground,
    onBackground = BlueOnBackground,
    onSurface = BlueOnSurface
)

// Light theme
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnSecondary,
    onTertiary = LightOnTertiary,
    onPrimaryContainer = LightOnPrimaryContainer,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface
)

// Dark theme
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnSecondary,
    onTertiary = DarkOnTertiary,
    onBackground = DarkOnBackground,
    onPrimaryContainer = DarkOnPrimaryContainer,
    onSurface = DarkOnSurface
)

// Composition local for theme preference
val LocalThemePreference = staticCompositionLocalOf<AppThemeType> { AppThemeType.BLUE }

/**
 * Get the current theme preference from the composition local
 */
val currentAppTheme: AppThemeType
    @Composable
    @ReadOnlyComposable
    get() = LocalThemePreference.current

/**
 * Check if the current theme is dark
 */
val isAppInDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalThemePreference.current == AppThemeType.DARK

/**
 * Check if the current theme is light
 */
val isAppInLightTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalThemePreference.current != AppThemeType.DARK

/**
 * Get the appropriate button text color based on the current theme
 */
val dialogButtonTextColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (LocalThemePreference.current) {
        AppThemeType.DARK -> Color.White
        else -> Color.Black
    }

/**
 * Main theme for the CubeSpeed app
 */
@Composable
fun CubeSpeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false by default to use our defined colors
    themeType: AppThemeType = AppThemeType.BLUE,
    content: @Composable () -> Unit
) {
    // Use the provided theme type parameter instead of the composition local value
    val appThemeType = themeType

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (appThemeType == AppThemeType.DARK) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        appThemeType == AppThemeType.DARK -> DarkColorScheme
        appThemeType == AppThemeType.LIGHT -> LightColorScheme
        else -> BlueColorScheme // Default to blue theme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

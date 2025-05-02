package com.example.cubespeed.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    primary = TimerScreenBackground,  // Using TimerScreen background color
    secondary = TimerScreenAccent,    // Using TimerScreen accent color
    tertiary = BlueTertiary,
    background = BlueBackground,
    surface = BlueSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,

    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

// Light theme
private val LightColorScheme = lightColorScheme(
    primary = Color.White,
    secondary = Color.Gray,
    tertiary = Color.Gray,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

// Dark theme
private val DarkColorScheme = darkColorScheme(
    primary = Color.Black,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = Color.Black,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
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

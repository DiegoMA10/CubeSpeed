package com.example.cubespeed.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Blue theme colors
val BlueGradientStart = Color(0xFF1E88E5) // Medium blue
val BlueGradientEnd = Color(0xFF0D47A1) // Dark blue
val BluePrimary = Color(0xFF1976D2)
val BlueSecondary = Color(0xFF2196F3)
val BlueTertiary = Color(0xFF03A9F4)
val BlueBackground = Color(0xFFF5F5F5)
val BlueSurface = Color(0xFFFFFFFF)

// Red theme colors
val RedGradientStart = Color(0xFFE53935) // Medium red
val RedGradientEnd = Color(0xFFB71C1C) // Dark red
val RedPrimary = Color(0xFFD32F2F)
val RedSecondary = Color(0xFFE57373)
val RedTertiary = Color(0xFFFF8A80)
val RedBackground = Color(0xFFF5F5F5)
val RedSurface = Color(0xFFFFFFFF)

// Dark theme colors
val DarkPrimary = Color(0xFF3F51B5)
val DarkSecondary = Color(0xFF5C6BC0)
val DarkTertiary = Color(0xFF7986CB)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)

// Create horizontal gradient brushes for the top bar
val BlueGradient = Brush.horizontalGradient(
    colors = listOf(BlueGradientStart, BlueGradientEnd)
)

val RedGradient = Brush.horizontalGradient(
    colors = listOf(RedGradientStart, RedGradientEnd)
)

// Timer screen colors
val TimerTextColor = Color.White
val MonospacedTextColor = Color.White
val BackgroundDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFF5F5F5)
val NavBarBackground = Color(0x99000000) // Semi-transparent black
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFFC107)

// Blue theme specific colors for TimerScreen
val TimerScreenBackground = Color(0xFF3F51B5)
val TimerScreenAccent = Color(0xFF5A57FF)

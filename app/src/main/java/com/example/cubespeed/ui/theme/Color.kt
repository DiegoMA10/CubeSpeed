package com.example.cubespeed.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Blue gradient colors for top bar
val BlueGradientStart = Color(0xFF1E88E5) // Medium blue
val BlueGradientEnd = Color(0xFF0D47A1) // Dark blue

// Create a horizontal gradient brush for the top bar
val BlueGradient = Brush.horizontalGradient(
    colors = listOf(BlueGradientStart, BlueGradientEnd)
)

// Timer screen colors
val TimerTextColor = Color.White
val MonospacedTextColor = Color.White
val BackgroundDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFF5F5F5)
val NavBarBackground = Color(0x99000000) // Semi-transparent black
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFFC107)

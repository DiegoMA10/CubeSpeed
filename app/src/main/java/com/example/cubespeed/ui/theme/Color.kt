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
val BlueOnPrimary = Color.White
val BlueOnSecondary = Color.White
val BlueOnTertiary = Color.White
val BlueOnBackground = Color(0xFF1C1B1F)
val BlueOnSurface = Color(0xFF1C1B1F)

// Red theme colors
val RedGradientStart = Color(0xFFE53935) // Medium red
val RedGradientEnd = Color(0xFFB71C1C) // Dark red
val RedPrimary = Color(0xFFD32F2F)
val RedSecondary = Color(0xFFE57373)
val RedTertiary = Color(0xFFFF8A80)
val RedBackground = Color(0xFFF5F5F5)
val RedSurface = Color(0xFFFFFFFF)

// Light theme colors
val LightPrimary = Color.White
val LightSecondary = Color(0xFFF3F3F3)
val LightTertiary = Color.White
val LightBackground = Color.White
val LightSurface = Color.White
val LightOnPrimary = Color.Black
val LightOnSecondary = Color.Black
val LightOnTertiary = Color.Black
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnSurface = Color(0xFF1C1B1F)
val LightOnPrimaryContainer = Color.White

// Dark theme colors
val DarkPrimary = Color.Black
val DarkSecondary = Color.DarkGray
val DarkTertiary = Color(0xFF7986CB)
val DarkBackground = Color.Black
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnPrimary = Color.White
val DarkOnSecondary = Color.White
val DarkOnTertiary = Color.White
val DarkOnBackground = Color.White
val DarkOnSurface = Color.White
val DarkOnPrimaryContainer = Color.Black

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
val TimerScreenBackground = Color(0xFF2f74ff)
val TimerScreenPrimaryBackground = Color(0xFF0000FF)
val TimerScreenAccent = Color(0xFF003eff)

// Additional Blue theme color
val BlueOnPrimaryContainer = TimerScreenBackground

// Chart colors
val ChartAo5LineColor = Color(0xFFFF5252)
val ChartAo12LineColorLight = Color(0xFF6650a4)
val ChartAo12LineColorDark = DarkTertiary
val ChartBestPointColor = Color(0xFFFFEB3B)
val ChartGridLineColorDark = Color.White.copy(alpha = 0.3f)
val ChartGridLineColorLight = Color.Black.copy(alpha = 0.3f)
val ChartAxisLabelColorDark = Color.White
val ChartAxisLabelColorLight = Color.Black
val ChartSolveLineColorDark = Color.White
val ChartSolveLineColorLight = Color.Black

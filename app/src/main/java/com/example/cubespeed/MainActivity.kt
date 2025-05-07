package com.example.cubespeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.cubespeed.navigation.NavigationWrapper
import com.example.cubespeed.navigation.Route
import com.example.cubespeed.ui.theme.AppThemeType
import com.example.cubespeed.ui.theme.CubeSpeedTheme
import com.example.cubespeed.ui.theme.LocalThemePreference
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.core.graphics.toColorInt
import android.view.WindowManager


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Get initial theme preference (only used for CompositionLocalProvider)
        val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
        val themeOrdinal = sharedPrefs.getInt("theme_type", AppThemeType.BLUE.ordinal)
        val themeType = AppThemeType.entries[themeOrdinal]

        // Navigation bar and status bar colors will be set using accompanist-systemuicontroller

        setContent {
            // Load theme preference from SharedPreferences
            var currentTheme by remember {
                val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
                val themeOrdinal = sharedPrefs.getInt("theme_type", AppThemeType.BLUE.ordinal)
                mutableStateOf(AppThemeType.entries[themeOrdinal])
            }

            // State to track if timer is running
            var isTimerRunning by remember { mutableStateOf(false) }

            // Listen for theme preference changes
            val themeChangeListener = { newTheme: AppThemeType ->
                currentTheme = newTheme
                val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
                with(sharedPrefs.edit()) {
                    putInt("theme_type", newTheme.ordinal)
                    apply()
                }

                // Navigation bar and status bar colors are now handled by the SideEffect
            }

            // Provide the theme preference to the composition
            CompositionLocalProvider(LocalThemePreference provides currentTheme) {
                // Set status bar and navigation bar colors based on the current theme
                val systemUiController = rememberSystemUiController()
                val isLightTheme = currentTheme != AppThemeType.DARK

                SideEffect {
                    // Set status bar color based on theme
                    val statusBarColor = when (currentTheme) {
                        AppThemeType.BLUE -> Color(0xFF2f74ff) // BluePrimary
                        AppThemeType.LIGHT -> Color.White // LightPrimary
                        AppThemeType.DARK -> Color.Black // DarkPrimary
                    }

                    // Set status bar color and icons
                    systemUiController.setStatusBarColor(
                        color = statusBarColor,
                        darkIcons = isLightTheme
                    )

                    // Set navigation bar color to black (always) with light icons
                    systemUiController.setNavigationBarColor(
                        color = Color.Black,
                        darkIcons = false
                    )
                }

                // SideEffect to keep screen on when timer is running
                SideEffect {
                    if (isTimerRunning) {
                        // Keep screen on when timer is running
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        // Allow the screen to turn off when the timer is not running
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                CubeSpeedTheme(themeType = currentTheme) {
                    // Check if the user is already authenticated
                    val auth = Firebase.auth
                    val startDestination = if (auth.currentUser != null) {
                        Route.MainTabs.route
                    } else {
                        Route.Login.route
                    }

                    val navController = rememberNavController()

                    // Scaffold at the top level
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars)
                    ) { innerPadding ->
                        // NavigationWrapper inside the Scaffold content
                        NavigationWrapper(
                            modifier = Modifier.padding(innerPadding),
                            navController = navController,
                            startDestination = startDestination,
                            onThemeChanged = themeChangeListener,
                            onTimerRunningChange = { running -> isTimerRunning = running }
                        )
                    }
                }
            }
        }
    }
}

// FixedSizeAnimatedVisibility has been moved to ui.components.AnimationComponents

package com.example.cubespeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cubespeed.navigation.NavigationWrapper
import com.example.cubespeed.navigation.Route
import com.example.cubespeed.navigation.bottomNavItems
import com.example.cubespeed.ui.theme.AppThemeType
import com.example.cubespeed.ui.theme.CubeSpeedTheme
import com.example.cubespeed.ui.theme.LocalThemePreference
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Get initial theme preference (only used for CompositionLocalProvider)
        val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
        val themeOrdinal = sharedPrefs.getInt("theme_type", AppThemeType.BLUE.ordinal)
        val themeType = AppThemeType.values()[themeOrdinal]

        // Set navigation bar color to black (always)
        window.navigationBarColor = android.graphics.Color.BLACK

        // Always use light navigation bar icons for better visibility on black background
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false
        }

        setContent {
            // Load theme preference from SharedPreferences
            var currentTheme by remember {
                val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
                val themeOrdinal = sharedPrefs.getInt("theme_type", AppThemeType.BLUE.ordinal)
                mutableStateOf(AppThemeType.values()[themeOrdinal])
            }

            // Listen for theme preference changes
            val themeChangeListener = { newTheme: AppThemeType ->
                currentTheme = newTheme
                val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
                with(sharedPrefs.edit()) {
                    putInt("theme_type", newTheme.ordinal)
                    apply()
                }

                // Set navigation bar color to black (always)
                window.navigationBarColor = android.graphics.Color.BLACK

                // Status bar color and icons are now handled by the SideEffect
            }

            // Provide the theme preference to the composition
            CompositionLocalProvider(LocalThemePreference provides currentTheme) {
                // Set status bar color based on current theme
                SideEffect {
                    // Set status bar color based on theme
                    window.statusBarColor = when (currentTheme) {
                        AppThemeType.BLUE -> android.graphics.Color.parseColor("#1976D2") // BluePrimary
                        AppThemeType.LIGHT -> android.graphics.Color.WHITE // LightPrimary
                        AppThemeType.DARK -> android.graphics.Color.parseColor("#FFFFFF") // DarkPrimary
                    }

                    // Update the status bar icons based on the theme
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    // Use dark icons for light themes (BLUE, LIGHT), light icons for dark theme
                    insetsController.isAppearanceLightStatusBars = currentTheme != AppThemeType.DARK
                }

                CubeSpeedTheme(themeType = currentTheme) {
                    // Check if user is already authenticated
                    val auth = Firebase.auth
                    val startDestination = if (auth.currentUser != null) {
                        Route.Timer.route
                    } else {
                        Route.Login.route
                    }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Check if we're in an authenticated route that should show the bottom navigation
                    val showBottomNav = currentDestination?.hierarchy?.any { destination ->
                        bottomNavItems.any { it.route == destination.route }
                    } ?: false

                    // Scaffold at the top level
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars),
                        bottomBar = {
                            if (showBottomNav) {
                                // Surface for applying shape rounded to the bar
                                Surface(

                                    modifier = Modifier.navigationBarsPadding().background(MaterialTheme.colorScheme.primary),
                                    tonalElevation = 4.dp,
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                ) {
                                    NavigationBar(
                                        modifier = Modifier
                                            .fillMaxWidth()

                                            .height(40.dp),
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ) {
                                        bottomNavItems.forEach { item ->
                                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                            NavigationBarItem(
                                                icon = { Icon(item.icon, contentDescription = item.title) },
                                                selected = selected,
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                                                    selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                                                    indicatorColor = MaterialTheme.colorScheme.secondary,
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                                                ),

                                                onClick = {
                                                    navController.navigate(item.route) {
                                                        // Pop up to the start destination of the graph to
                                                        // avoid building up a large stack of destinations
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        // Avoid multiple copies of the same destination when
                                                        // reselecting the same item
                                                        launchSingleTop = true
                                                        // Restore state when reselecting a previously selected item
                                                        restoreState = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        // NavigationWrapper inside the Scaffold content
                        NavigationWrapper(
                            modifier = Modifier.padding(innerPadding),
                            navController = navController,
                            startDestination = startDestination,
                            onThemeChanged = themeChangeListener
                        )
                    }
                }
            }
        }
    }
}

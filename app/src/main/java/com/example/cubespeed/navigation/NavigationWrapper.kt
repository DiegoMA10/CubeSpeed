package com.example.cubespeed.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cubespeed.ui.screens.login.LoginScreen
import com.example.cubespeed.ui.screens.register.RegisterScreen
import com.example.cubespeed.ui.screens.timer.TimerScreen
import com.example.cubespeed.ui.screens.history.HistoryScreen
import com.example.cubespeed.ui.screens.statistics.StatisticsScreen
import com.example.cubespeed.ui.screens.settings.SettingsScreen
import com.example.cubespeed.ui.theme.AppThemeType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Sealed class representing all possible navigation routes in the app
 */
sealed class Route(val route: String) {
    object Login : Route("login")
    object Register : Route("register")
    object Home : Route("home")
    object Timer : Route("timer")
    object History : Route("history")
    object Statistics : Route("statistics")
    object Settings : Route("settings")
}

/**
 * Data class representing a bottom navigation item
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

/**
 * List of bottom navigation items
 */
val bottomNavItems = listOf(
    BottomNavItem(
        route = Route.Timer.route,
        title = "Timer",
        icon = Icons.Filled.Timer
    ),
    BottomNavItem(
        route = Route.History.route,
        title = "History",
        icon = Icons.Filled.History
    ),
    BottomNavItem(
        route = Route.Statistics.route,
        title = "Statistics",
        icon = Icons.Outlined.BarChart
    ),
    BottomNavItem(
        route = Route.Settings.route,
        title = "Settings",
        icon = Icons.Filled.Settings
    )
)

/**
 * Main navigation component that manages navigation between screens
 * @param navController The navigation controller to use for navigation
 * @param startDestination The starting destination route
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Route.Login.route,
    onThemeChanged: (AppThemeType) -> Unit = {}
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Route.Register.route) },
                onLoginSuccess = { navController.navigate(Route.Timer.route) {
                    // Clear back stack when user logs in
                    popUpTo(Route.Login.route) { inclusive = true }
                }}
            )
        }

        composable(Route.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Route.Login.route) },
                onRegisterSuccess = { navController.navigate(Route.Timer.route) {
                    // Clear back stack when user registers
                    popUpTo(Route.Login.route) { inclusive = true }
                }}
            )
        }

        composable(Route.Home.route) {
            // Empty home route
        }

        composable(Route.Timer.route) {
            TimerScreen()
        }

        composable(Route.History.route) {
            HistoryScreen()
        }

        composable(Route.Statistics.route) {
            StatisticsScreen()
        }

        composable(Route.Settings.route) {
            SettingsScreen(
                onLogout = { navController.navigate(Route.Login.route) {
                    // Clear back stack when user logs out
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }},
                onThemeChanged = onThemeChanged
            )
        }
    }
}

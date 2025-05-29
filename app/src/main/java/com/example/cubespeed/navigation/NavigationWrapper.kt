package com.example.cubespeed.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Code
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cubespeed.ui.screens.algorithms.OLLAlgorithmsScreen
import com.example.cubespeed.ui.screens.algorithms.PLLAlgorithmsScreen
import com.example.cubespeed.ui.screens.login.LoginScreen
import com.example.cubespeed.ui.screens.main.MainTabsScreen
import com.example.cubespeed.ui.screens.register.RegisterScreen
import com.example.cubespeed.ui.theme.AppThemeType

/**
 * Sealed class representing all possible navigation routes in the app
 */
sealed class Route(val route: String) {
    object Login : Route("login")
    object Register : Route("register")
    object Home : Route("home")
    object MainTabs : Route("main_tabs")
    object Timer : Route("timer")
    object History : Route("history")
    object Statistics : Route("statistics")
    object Settings : Route("settings")
    object Algorithms : Route("algorithms")
    object OLLAlgorithms : Route("oll_algorithms")
    object PLLAlgorithms : Route("pll_algorithms")
    object Game : Route("game")
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
        route = Route.Algorithms.route,
        title = "Algorithms",
        icon = Icons.Outlined.Code
    )
)

/**
 * Main navigation component that manages navigation between screens
 * @param navController The navigation controller to use for navigation
 * @param startDestination The starting destination route
 * @param onThemeChanged Callback for when the theme is changed
 * @param onTimerRunningChange Callback for when the timer running state changes
 * @param initialTabIndex Initial tab to show in MainTabsScreen (0 for Timer, 1 for History, 2 for Statistics, 3 for Algorithms)
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Route.Login.route,
    onThemeChanged: (AppThemeType) -> Unit = {},
    onTimerRunningChange: (Boolean) -> Unit = {},
    initialTabIndex: Int = 0 // Default to Timer tab
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Route.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Route.MainTabs.route) {
                        // Clear back stack when user logs in
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Route.Login.route) },
                onRegisterSuccess = {
                    navController.navigate(Route.MainTabs.route) {
                        // Clear back stack when user registers
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.MainTabs.route) {
            // Use the new MainTabsScreen composable
            MainTabsScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Route.Login.route) {
                        // Clear back stack when user logs out
                        popUpTo(Route.MainTabs.route) { inclusive = true }
                    }
                },
                onThemeChanged = onThemeChanged,
                onTimerRunningChange = onTimerRunningChange,
                initialTab = initialTabIndex // Pass the initial tab index
            )
        }

        // OLL Algorithms screen
        composable(Route.OLLAlgorithms.route) {
            OLLAlgorithmsScreen(navController)
        }

        // PLL Algorithms screen
        composable(Route.PLLAlgorithms.route) {
            PLLAlgorithmsScreen(navController)
        }

        // Game screen that launches Unity
        composable(Route.Game.route) {
            com.example.cubespeed.ui.screens.game.GameScreen(navController)
        }
    }
}

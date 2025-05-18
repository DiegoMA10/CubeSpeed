package com.example.cubespeed.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cubespeed.ui.screens.algorithms.OLLAlgorithmsScreen
import com.example.cubespeed.ui.screens.algorithms.PLLAlgorithmsScreen
import com.example.cubespeed.ui.screens.login.LoginScreen
import com.example.cubespeed.ui.screens.register.RegisterScreen
import com.example.cubespeed.ui.screens.main.MainTabsScreen
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
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Route.Login.route,
    onThemeChanged: (AppThemeType) -> Unit = {},
    onTimerRunningChange: (Boolean) -> Unit = {}
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
                onTimerRunningChange = onTimerRunningChange
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

        // Game screen (placeholder for now)
        composable(Route.Game.route) {
            // This will be implemented later
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Game Coming Soon!",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}

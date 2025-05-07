package com.example.cubespeed.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import kotlin.math.absoluteValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch

/**
 * A composable that implements horizontal paging between main screens.
 * This provides a smooth paging experience between the main app screens.
 */
@Composable
fun MainScreenPager(
    navController: NavController,
    currentRoute: String,
    modifier: Modifier = Modifier,
    onPageChange: (Int) -> Unit = {},
    pageContent: @Composable (Int) -> Unit
) {
    val routes = listOf(
        Route.Timer.route,
        Route.History.route,
        Route.Statistics.route,
        Route.Settings.route
    )

    val pagerState = rememberPagerState { routes.size }
    val coroutineScope = rememberCoroutineScope()

    // Track whether the user is currently swiping
    var isUserSwiping by remember { mutableStateOf(false) }

    // Track how far the user has swiped
    var swipeProgress by remember { mutableStateOf(0f) }

    // Sincronizar cuando la pestaña cambia desde fuera (bottom nav)
    LaunchedEffect(currentRoute) {
        // Find the index of the current route in the routes list
        val index = routes.indexOf(currentRoute)
        if (index != -1 && index != pagerState.currentPage) {
            // Animate to the new page
            pagerState.animateScrollToPage(index)
        }
    }

    // Custom fling behavior that prevents automatic page transitions during swipes



    // Handle page changes from user swiping
    LaunchedEffect(pagerState.currentPage) {
        // Only update navigation if the page change was from user interaction
        // and not from our own LaunchedEffect above
        if (routes.contains(currentRoute) && pagerState.currentPage != routes.indexOf(currentRoute)) {
            // Get the route for the current page
            val newRoute = routes[pagerState.currentPage]
            // Notify parent about page change
            onPageChange(pagerState.currentPage)
            // Update navigation
            navController.navigate(newRoute) {
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
    }

    Box(
        modifier = modifier.fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            ).graphicsLayer { alpha = 1f }
    ) {
        if (routes.contains(currentRoute)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp,
                contentPadding = PaddingValues(0.dp),
                key = { it },
                userScrollEnabled = true,

            ) { page ->
                pageContent(page)
            }
        } else {
            // Rutas fuera de los tabs principales
            pageContent(routes.indexOf(currentRoute).takeIf { it != -1 } ?: 0)
        }
    }
}

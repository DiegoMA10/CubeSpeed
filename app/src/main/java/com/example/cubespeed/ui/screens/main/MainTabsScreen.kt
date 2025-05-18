package com.example.cubespeed.ui.screens.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.navigation.Route
import com.example.cubespeed.navigation.bottomNavItems
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.components.CubeTopBar
import com.example.cubespeed.ui.components.FixedSizeAnimatedVisibility
import com.example.cubespeed.ui.screens.history.HistoryScreen
import com.example.cubespeed.ui.screens.settings.SettingsScreen
import com.example.cubespeed.ui.screens.statistics.StatisticsScreen
import com.example.cubespeed.ui.screens.algorithms.AlgorithmsScreen
import com.example.cubespeed.ui.screens.timer.dialogs.CubeSelectionDialog
import com.example.cubespeed.ui.screens.timer.dialogs.TagInputDialog
import com.example.cubespeed.ui.screens.timer.TimerScreenRefactored
import com.example.cubespeed.ui.theme.AppThemeType
import kotlinx.coroutines.launch

/**
 * Main screen that contains the bottom navigation and pager for the main tabs
 */
@Composable
fun MainTabsScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onThemeChanged: (AppThemeType) -> Unit,
    onTimerRunningChange: (Boolean) -> Unit
) {
    // State to track if timer is running
    var isTimerRunning by remember { mutableStateOf(false) }
    val showWithDelay = remember { mutableStateOf(true) }
    var isFirstLaunch = remember { mutableStateOf(true) }

    // State to track if settings screen is visible
    var showSettings by remember { mutableStateOf(false) }

    // State for selected cube type and tag
    var selectedCubeType by remember { mutableStateOf(AppState.selectedCubeType) }
    var selectedTag by remember { mutableStateOf(AppState.selectedTag) }

    // State for showing dialogs
    var showCubeSelectionDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // List of cube types
    val cubeTypes = CubeType.getAllDisplayNames()

    // Repository for Firebase operations
    val repository = remember { FirebaseRepository() }

    // Update parent when timer running state changes
    LaunchedEffect(isTimerRunning) {
        onTimerRunningChange(isTimerRunning)
    }

    // Animation effect for bottom navigation visibility


    val pages = listOf(
        Route.Timer,
        Route.History,
        Route.Statistics,
        Route.Algorithms
    )

    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()


    // Show bottom navigation by default, hide only when timer is running
    // This is now handled by showWithDelay

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Only show CubeTopBar when timer is not running
                FixedSizeAnimatedVisibility(
                    visible = !isTimerRunning,
                    enter = fadeIn(animationSpec = tween(durationMillis = 400)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 400))
                ) {
                    CubeTopBar(
                        title = selectedCubeType,
                        subtitle = selectedTag,
                        onSettingsClick = {
                            // Show settings screen
                            showSettings = true
                        },
                        onOptionsClick = { showTagDialog = true },
                        onCubeClick = { showCubeSelectionDialog = true }
                    )
                }
            },
            bottomBar = {
                FixedSizeAnimatedVisibility(
                    visible = isTimerRunning.not(),
                    enter = fadeIn(animationSpec = tween(durationMillis = 400)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 400))
                ) {
                    Surface(
                        modifier = Modifier.navigationBarsPadding(),
                        tonalElevation = 4.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = pagerState.currentPage == when (item.route) {
                                    Route.Timer.route -> 0
                                    Route.History.route -> 1
                                    Route.Statistics.route -> 2
                                    Route.Algorithms.route -> 3
                                    else -> 0
                                }

                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.secondary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                    ),
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                when (item.route) {
                                                    Route.Timer.route -> 0
                                                    Route.History.route -> 1
                                                    Route.Statistics.route -> 2
                                                    Route.Algorithms.route -> 3
                                                    else -> 0
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .graphicsLayer { alpha = 1f }
            ) {
                HorizontalPager(

                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    pageSpacing = 0.dp,
                    contentPadding = PaddingValues(0.dp),
                    userScrollEnabled = !isTimerRunning,
                    key = { it }, // Use page index as key to maintain state
                    beyondViewportPageCount = 3 // Keep all pages in memory
                ) { page ->
                    when (page) {
                        0 -> TimerScreenRefactored(
                            onTimerRunningChange = { running ->
                                isTimerRunning = running
                            }
                        )

                        1 -> HistoryScreen()
                        2 -> StatisticsScreen()
                        3 -> AlgorithmsScreen(navController)
                    }
                }
            }
        }

        // Settings Screen (shown as a modal when settings icon is clicked)
        if (showSettings) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    // Top bar with back button
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showSettings = false }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Settings content
                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(
                            onLogout = onLogout,
                            onThemeChanged = onThemeChanged
                        )
                    }
                }
            }
        }

        // Cube Selection Dialog
        if (showCubeSelectionDialog) {
            CubeSelectionDialog(
                cubeTypes = cubeTypes,
                onCubeSelected = {
                    selectedCubeType = it
                    AppState.selectedCubeType = it
                    showCubeSelectionDialog = false
                },
                onDismiss = { showCubeSelectionDialog = false }
            )
        }

        // Tag Dialog
        if (showTagDialog) {
            TagInputDialog(
                currentTag = selectedTag,
                onTagConfirmed = {
                    selectedTag = it
                    AppState.selectedTag = it
                    showTagDialog = false
                },
                onDismiss = { showTagDialog = false }
            )
        }
    }
}

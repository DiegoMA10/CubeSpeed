package com.example.cubespeed.ui.screens.timer.components

import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.cubespeed.ui.components.FixedSizeAnimatedVisibility
import com.example.cubespeed.ui.screens.timer.dialogs.CommentDialog
import com.example.cubespeed.ui.screens.timer.viewmodels.TimerViewModel
import com.example.cubespeed.state.AppState
import kotlinx.coroutines.delay

/**
 * A composable that displays a timer with controls for starting, stopping, and managing solves.
 *
 * @param viewModel The ViewModel that handles the timer's state and logic
 * @param onTimerRunningChange Callback for when the timer's running state changes
 */
@Composable
fun TimerComponent(
    viewModel: TimerViewModel,
    onTimerRunningChange: (Boolean) -> Unit = {}
) {
    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // State for showing dialogs
    var showCommentDialog by remember { mutableStateOf(false) }

    // State for showing UI elements with delay
    val showWithDelay = remember { mutableStateOf(true) }

    // Movement threshold in pixels - movements smaller than this won't count
    val movementThreshold = 10f

    // Horizontal movement threshold - higher threshold to only detect actual scrolling
    val horizontalPagingThreshold = 15f

    // Get the current context
    val context = LocalContext.current

    // DisposableEffect to acquire and release wake lock when timer is running
    DisposableEffect(viewModel.isRunning) {
        // Get the PowerManager service
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // Create a wake lock
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CubeSpeed:TimerWakeLock"
        )

        // Acquire the wake lock if the timer is running
        if (viewModel.isRunning) {
            if (!wakeLock.isHeld) {
                wakeLock.acquire() // 30 minutes timeout as a safety measure
            }
        }

        // Release the wake lock when the effect is disposed
        onDispose {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    // Animated scale factor for the timer
    val timerScale by animateFloatAsState(
        targetValue = if (viewModel.isRunning) 1.125f else 1f, // 12.5% larger when running
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timerScale"
    )

    // Timer color
    val timerColor = MaterialTheme.colorScheme.onPrimary

    // Effect to notify parent when timer running state changes
    LaunchedEffect(viewModel.isRunning) {
        onTimerRunningChange(viewModel.isRunning)
    }

    // Effect for first launch and animation
    LaunchedEffect(viewModel.isRunning) {
        if (viewModel.isFirstLaunch) {
            // On first launch, showWithDelay is already true (set in the declaration)
            viewModel.isFirstLaunch = false
        } else if (!viewModel.isRunning) {
            showWithDelay.value = false
            delay(500)
            showWithDelay.value = true
        } else {
            showWithDelay.value = false
        }
    }

    // Timer effect
    LaunchedEffect(viewModel.isRunning) {
        if (viewModel.isRunning) {
            while (viewModel.isRunning) {
                viewModel.updateTimer()
                delay(10) // Update every 10ms for smooth display
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) Capa de toque completa (full-screen touch layer)
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { // Using Unit as key to prevent reinitialization during operations
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for at least one pointer event
                            val event = awaitPointerEvent()

                            // Ignore all events when in cooldown
                            if (viewModel.isInCooldown) {
                                continue
                            }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    // Check if this is a multi-touch event (more than one pointer)
                                    if (event.changes.size > 1) {
                                        // Ignore multi-touch events to prevent timer getting stuck
                                        // Reset all flags to prevent the timer from getting stuck
                                        viewModel.resetMovementFlags()
                                        continue
                                    }

                                    if (!viewModel.isRunning) {
                                        // Store the previous solve state before resetting
                                        viewModel.storePreviousState()

                                        // Reset the movement tracking flag
                                        viewModel.hasMovedWhilePressed = false

                                        // Store the initial position
                                        val position = event.changes.first().position
                                        viewModel.setInitialPressPosition(position.x, position.y)

                                        // When timer is not running and screen is pressed:
                                        // 1. Set isScreenPressed to true
                                        // 2. Reset timer to 0.00 but don't start it
                                        // 3. Keep the control buttons visible (don't call resetTimerState)
                                        viewModel.isScreenPressed = true

                                        // Manually reset state variables without hiding controls
                                        viewModel.elapsedTime = 0L // Ensure timer shows 0.00
                                        viewModel.isDNF = false
                                        viewModel.hasAddedTwoSeconds = false
                                        viewModel.originalTime = 0L

                                        // Only reset completedSolve, but keep showControls as is
                                        if (viewModel.completedSolve != null) {
                                            viewModel.completedSolve = null
                                        }
                                    } else {
                                        // When running, stop on first touch
                                        viewModel.stopTimer()

                                        // Set cooldown to prevent immediate restart
                                        viewModel.setCooldown(1000) // 1000ms cooldown for touch event
                                    }
                                }

                                PointerEventType.Move -> {
                                    // If the screen is pressed and the timer is not running,
                                    // check if the pager is actually scrolling
                                    if (viewModel.isScreenPressed && !viewModel.isRunning) {
                                        // Check if the pager is actually scrolling (detected by MainTabsScreen)
                                        if (AppState.isPagerScrolling) {
                                            // The pager is actually scrolling, so mark as paging
                                            viewModel.isPaging = true
                                            viewModel.hasMovedWhilePressed = true

                                            // Restore previous state immediately when paging starts
                                            viewModel.restorePreviousState()
                                        }
                                        // No fallback to distance-based detection as per user request
                                    }
                                }

                                PointerEventType.Release -> {
                                    if (viewModel.isScreenPressed && !viewModel.isRunning) {
                                        // Set isScreenPressed to false
                                        viewModel.isScreenPressed = false

                                        // Only check if the pager is actually scrolling (detected by MainTabsScreen)
                                        if (AppState.isPagerScrolling) {
                                            // If the pager is actually scrolling, don't start the timer
                                            // and restore previous state
                                            viewModel.restorePreviousState()
                                        } else {
                                            // Start the timer if the pager is not scrolling
                                            // Ignore distance-based detection as per user request
                                            viewModel.startTimer()
                                            viewModel.setCooldown(70)
                                        }
                                    }

                                    // Always reset flags
                                    viewModel.resetMovementFlags()
                                }

                                else -> {
                                    // Check if this is a Cancel event by name
                                    if (event.type.toString() == "Cancel") {
                                        // Always reset all flags on Cancel to prevent the timer from getting stuck
                                        viewModel.resetMovementFlags()
                                    }
                                }
                            }
                        }
                    }
                }
        )

        // 2) Contenido "arriba" (scramble)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .zIndex(5f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Scramble bar with animation
            FixedSizeAnimatedVisibility(
                visible = showWithDelay.value,
                modifier = Modifier.offset(y = (-24).dp),
                enter = slideInVertically(
                    initialOffsetY = { -it + 175 },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                ResponsiveScrambleBar(
                    scramble = viewModel.scramble,
                    isLoading = viewModel.isScrambleLoading,
                    onEdit = { newScramble ->
                        // Update the scramble when edited
                        viewModel.updateScramble(newScramble)
                    },
                    onShuffle = {
                        // Only update the scramble if not in cooldown
                        if (!viewModel.isInCooldown) {
                            viewModel.generateNewScramble()
                        }
                    },
                    onScrambleClick = {
                        // Check if we're in cooldown period
                        if (!viewModel.isInCooldown) {
                            // Reset timer state and completedSolve
                            viewModel.resetTimerState(resetCompletedSolve = true)

                            viewModel.startTimer()
                        }
                    }
                )
            }
        }

        // 3) Contenido "abajo" (stats)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
                .zIndex(5f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Get the current screen configuration
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val screenHeight = configuration.screenHeightDp

            // Check if we're in landscape mode (width > height)
            val isLandscape = screenWidth > screenHeight

            // Statistics component with animation
            FixedSizeAnimatedVisibility(
                visible = showWithDelay.value,
                enter = slideInVertically(
                    initialOffsetY = { if (isLandscape) it - 100 else it - 200 },
                    animationSpec = tween(durationMillis = 600)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                ResponsiveStatisticsComponent(
                    selectedCubeType = viewModel.selectedCubeType,
                    selectedTag = viewModel.selectedTag,
                    refreshTrigger = viewModel.statsRefreshTrigger,
                    scramble = viewModel.scramble,
                    isLoading = viewModel.isScrambleLoading
                )
            }
        }

        // 4) Timer + iconos, sobre todo (centered and above everything)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(10f)       // por encima de todo
                .padding(16.dp),   // opcional
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Get the current screen configuration for the timer display
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp
                val screenHeight = configuration.screenHeightDp

                // Check if we're in landscape mode (width > height)
                val isLandscape = screenWidth > screenHeight

                // Padding depends on orientation - less vertical padding in landscape
                val verticalPadding = if (isLandscape) 2.dp else 2.dp

                // Use the TimerDisplay component
                TimerDisplayComponent(
                    elapsedTime = viewModel.elapsedTime,
                    timerColor = timerColor,
                    timerScale = timerScale,
                    hasAddedTwoSeconds = viewModel.hasAddedTwoSeconds,
                    isDNF = viewModel.isDNF,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = verticalPadding)
                )

                // Control icons that appear when timer stops
                val showIcons = viewModel.showControls && !viewModel.isRunning && viewModel.elapsedTime >= 0
                if (showIcons) {
                    // Use the TimerIconsComponent
                    TimerIconsComponent(
                        isLandscape = isLandscape,
                        hasAddedTwoSeconds = viewModel.hasAddedTwoSeconds,
                        isDNF = viewModel.isDNF,
                        isScreenPressed = viewModel.isScreenPressed,
                        onDeleteClick = { viewModel.deleteSolve() },
                        onDNFClick = { viewModel.toggleDNF() },
                        onPlusTwoClick = { viewModel.togglePlusTwo() },
                        onCommentClick = {
                            // Show a dialog to add a comment
                            if (viewModel.completedSolve != null) {
                                // Show comment dialog
                                showCommentDialog = true
                            }
                        }
                    )
                } else {
                    // Spacer size depends on orientation
                    val spacerSize = if (isLandscape) 40.dp else 48.dp
                    Spacer(modifier = Modifier.size(spacerSize))
                }
            }
        }
    }

    // Comment Dialog
    if (showCommentDialog) {
        CommentDialog(
            commentText = viewModel.commentText,
            onCommentChanged = { viewModel.updateComment(it) },
            onDismiss = { showCommentDialog = false },
            onSave = { /* Save is handled by onCommentChanged */ }
        )
    }
}

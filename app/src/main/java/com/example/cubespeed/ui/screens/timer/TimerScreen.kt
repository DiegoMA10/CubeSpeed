package com.example.cubespeed.ui.screens.timer

import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.repository.SolveStatistics
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.components.FixedSizeAnimatedVisibility
import com.example.cubespeed.ui.screens.history.ScrambleVisualization
import com.example.cubespeed.ui.theme.StatisticsTextStyle
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Create a single instance of FirebaseRepository to be used throughout the app
val firebaseRepository = FirebaseRepository()

@Composable
fun TimerScreen(
    onTimerRunningChange: (Boolean) -> Unit = {}
) {
    // Use shared state for the selected cube type and tag
    var selectedCubeType by remember { mutableStateOf(AppState.selectedCubeType) }
    var selectedTag by remember { mutableStateOf(AppState.selectedTag) }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // State for the previous solve
    var previousSolve by remember { mutableStateOf<Solve?>(null) }

    // State for showing dialogs
    var showCubeSelectionDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // Shared timer trigger state
    var timerTrigger by remember { mutableStateOf(false) }

    // State to track if timer is running
    var isTimerRunning by remember { mutableStateOf(false) }

    // Effect to notify parent when timer running state changes
    LaunchedEffect(isTimerRunning) {
        onTimerRunningChange(isTimerRunning)
    }

    // Get the current context
    val context = LocalContext.current

    // DisposableEffect to acquire and release wake lock when timer is running
    DisposableEffect(isTimerRunning) {
        // Get the PowerManager service
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // Create a wake lock
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CubeSpeed:TimerWakeLock"
        )

        // Acquire the wake lock if the timer is running
        if (isTimerRunning) {
            if (!wakeLock.isHeld) {
                wakeLock.acquire() // 30 minutes timeout as a safety measure
                println("[DEBUG_LOG] Wake lock acquired")
            }
        }

        // Release the wake lock when the effect is disposed
        onDispose {
            if (wakeLock.isHeld) {
                wakeLock.release()
                println("[DEBUG_LOG] Wake lock released")
            }
        }
    }

    // State for statistics refresh trigger
    var statsRefreshTrigger by remember { mutableLongStateOf(0L) }

    // Observe changes to AppState and update local state
    LaunchedEffect(AppState.selectedCubeType, AppState.selectedTag) {
        selectedCubeType = AppState.selectedCubeType
        selectedTag = AppState.selectedTag
        // Increment stats refresh trigger to update statistics when cube type or tag changes
        statsRefreshTrigger += 1
    }

    // List of available cube types
    val cubeTypes = CubeType.getAllDisplayNames()

    // Remember the Timer composable to reuse it instead of creating a new one each time
    val timer = remember {
        @Composable {
            Timer(
                externalTrigger = timerTrigger,
                onExternalTriggerHandled = { timerTrigger = false },
                onTimerRunningChange = { isRunning -> isTimerRunning = isRunning },
                selectedCubeType = selectedCubeType,
                selectedTag = selectedTag,
                previousSolve = previousSolve,
                onSolveComplete = { solve ->
                    // Save to Firebase in the background and update with the ID
                    coroutineScope.launch {
                        println("[DEBUG_LOG] TimerScreen: Saving solve with ID: ${solve.id}, Status: ${solve.status.name}")

                        // Check if this is a new solve or an update to an existing one
                        if (solve.id.isEmpty()) {
                            // This is a new solve, save it and update previousSolve with the ID
                            val solveId = firebaseRepository.saveSolve(solve)
                            println("[DEBUG_LOG] TimerScreen: Received ID from Firebase: $solveId")
                            if (solveId.isNotEmpty()) {
                                // Update the solve with the ID from Firebase
                                val updatedSolve = solve.copy(id = solveId)
                                println("[DEBUG_LOG] TimerScreen: Updated solve with ID: ${updatedSolve.id}")
                                // Save the previous solve when a new one starts
                                previousSolve = updatedSolve
                            } else {
                                // If save failed, still keep the solve in memory
                                println("[DEBUG_LOG] TimerScreen: Save failed, keeping original solve")
                                previousSolve = solve
                            }
                        } else {
                            // This is an update to an existing solve, just save it
                            println("[DEBUG_LOG] TimerScreen: Updating existing solve with ID: ${solve.id}")
                            firebaseRepository.saveSolve(solve)
                            // No need to update previousSolve as it already has the correct ID
                        }

                        // Increment stats refresh trigger to update statistics
                        statsRefreshTrigger += 1

                        // Increment history refresh trigger to update history screen
                        AppState.historyRefreshTrigger += 1
                        println("[DEBUG_LOG] TimerScreen: Incremented historyRefreshTrigger to ${AppState.historyRefreshTrigger}")
                    }
                },
                onSolveUpdated = { updatedSolve ->
                    // This callback will be called when a solve is updated with an ID
                    // Update the Timer's completedSolve with the ID
                    if (updatedSolve.id.isNotEmpty()) {
                        println("[DEBUG_LOG] TimerScreen: onSolveUpdated called with ID: ${updatedSolve.id}")
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        // Scaffold te aplica automáticamente padding en content por la bottomBar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp), // Removed bottom padding as it's handled by Scaffold's innerPadding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // CubeTopBar is now in MainTabsScreen

            // Use the remembered Timer composable
            timer()

            // Statistics card at the bottom
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

@Composable
fun Timer(
    externalTrigger: Boolean = false,
    onExternalTriggerHandled: () -> Unit = {},
    onTimerRunningChange: (Boolean) -> Unit = {},
    selectedCubeType: String = CubeType.CUBE_3X3.displayName,
    selectedTag: String = "normal",
    previousSolve: Solve? = null,
    onSolveComplete: (Solve) -> Unit = {},
    onSolveUpdated: ((Solve) -> Unit)? = null
) {
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var elapsedTime by rememberSaveable { mutableLongStateOf(0L) }
    var startTime by remember { mutableLongStateOf(0L) }
    var showControls by rememberSaveable { mutableStateOf(false) }
    val showWithDelay = remember { mutableStateOf(true) }

    // State to track if the screen is being pressed
    var isScreenPressed by remember { mutableStateOf(false) }

    // State to track if the user has moved their finger while holding down
    var hasMovedWhilePressed by remember { mutableStateOf(false) }

    // State to track if paging is in progress
    var isPaging by remember { mutableStateOf(false) }

    // State to track the initial position when the user presses down
    var initialPressX by remember { mutableStateOf(0f) }
    var initialPressY by remember { mutableStateOf(0f) }

    // Movement threshold in pixels - movements smaller than this won't count
    val movementThreshold = 10f

    // Horizontal movement threshold - smaller threshold for detecting paging
    val horizontalPagingThreshold = 3f

    // State to store the previous solve state before pressing
    var previousElapsedTime by remember { mutableLongStateOf(0L) }
    var previousIsDNF by remember { mutableStateOf(false) }
    var previousHasAddedTwoSeconds by remember { mutableStateOf(false) }
    var previousCompletedSolve by remember { mutableStateOf<Solve?>(null) }
    var previousShowControls by remember { mutableStateOf(false) }
    var previousScramble by remember { mutableStateOf("") }
    var previousSelectedCubeType by remember { mutableStateOf(selectedCubeType) }
    var previousSelectedTag by remember { mutableStateOf(selectedTag) }
    var previousCommentText by remember { mutableStateOf("") }

    // State to track the cube type when a solve starts
    var solveCubeType by remember { mutableStateOf(selectedCubeType) }

    // Cooldown state to prevent spam clicking
    var isInCooldown by remember { mutableStateOf(false) }

    // State for comment dialog
    var showCommentDialog by rememberSaveable { mutableStateOf(false) }
    var commentText by rememberSaveable { mutableStateOf(AppState.commentText) }

    // Observe changes to AppState.commentText and update local state
    LaunchedEffect(AppState.commentText) {
        commentText = AppState.commentText
    }

    // Coroutine scope for async operations
    val timerCoroutineScope = rememberCoroutineScope()

    // Function to calculate font size based on text length will be used instead of a state variable

    // Use the file-level calculateFontSize function

    var timerColor by remember { mutableStateOf(Color.White) }
    var isDNF by rememberSaveable { mutableStateOf(false) }
    var originalTime by rememberSaveable { mutableLongStateOf(0L) }
    var hasAddedTwoSeconds by rememberSaveable { mutableStateOf(false) }
    var scramble by rememberSaveable { mutableStateOf("") }
    var statsRefreshTrigger by remember { mutableLongStateOf(0L) }

    // State to track if a scramble is being generated
    var isScrambleLoading by remember { mutableStateOf(false) }

    // Debug log when isScrambleLoading changes
    LaunchedEffect(isScrambleLoading) {
        println("[DEBUG_LOG] isScrambleLoading changed to: $isScrambleLoading")
    }

    // Generate initial scramble when component is first created
    LaunchedEffect(Unit) {
        if (scramble.isEmpty()) {
            println("[DEBUG_LOG] Generating initial scramble")
            isScrambleLoading = true
            // Use a coroutine to avoid blocking the UI thread
            timerCoroutineScope.launch {
                try {
                    val newScramble = generateScramble(CubeType.fromDisplayName(selectedCubeType))
                    println("[DEBUG_LOG] Initial scramble generated: ${newScramble.take(20)}...")
                    scramble = newScramble
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Error generating initial scramble: ${e.message}")
                    // Generate a simple fallback scramble if the real one fails
                    scramble = "R U R' U'"
                } finally {
                    isScrambleLoading = false
                    println("[DEBUG_LOG] Finished initial scramble generation")
                }
            }
        }
    }

    // Animated scale factor for the timer
    val timerScale by animateFloatAsState(
        targetValue = if (isRunning) 1.125f else 1f, // 12.5% larger when running
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "timerScale"
    )

    // State to track if we have a completed solve that needs to be saved
    var completedSolve by rememberSaveable { mutableStateOf<Solve?>(null) }

    // Function to reset timer state to initial values
    fun resetTimerState(
        resetCompletedSolve: Boolean = false,
        generateNewScramble: Boolean = false
    ) {
        elapsedTime = 0L
        showControls = false
        isDNF = false
        hasAddedTwoSeconds = false
        originalTime = 0L

        if (resetCompletedSolve && completedSolve != null) {
            completedSolve = null
        }

        if (generateNewScramble) {
            println("[DEBUG_LOG] Starting scramble generation in resetTimerState")
            isScrambleLoading = true
            timerCoroutineScope.launch {
                // Generate the scramble in a coroutine to avoid blocking the UI
                println("[DEBUG_LOG] Generating scramble for cube type: $selectedCubeType")
                try {
                    val newScramble = generateScramble(CubeType.fromDisplayName(selectedCubeType))
                    println("[DEBUG_LOG] Scramble generated: ${newScramble.take(20)}...")
                    scramble = newScramble
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Error generating scramble in resetTimerState: ${e.message}")
                    // Generate a simple fallback scramble if the real one fails
                    scramble = "R U R' U'"
                } finally {
                    isScrambleLoading = false
                    println("[DEBUG_LOG] Finished scramble generation in resetTimerState")
                }
            }
        }
    }

    // Function to set cooldown state and clear it after a delay
    fun setCooldown(durationMillis: Long = 1000) {
        println("[DEBUG_LOG] Setting cooldown for $durationMillis ms")
        isInCooldown = true

        // Launch a coroutine to clear the cooldown after a delay
        timerCoroutineScope.launch {
            delay(durationMillis)
            isInCooldown = false
            println("[DEBUG_LOG] Cooldown ended after $durationMillis ms")
        }
    }

    // Function to start the timer
    fun startTimer(resetExternalTrigger: Boolean = false) {
        // Capture the current cube type when the timer starts
        solveCubeType = selectedCubeType
        println("[DEBUG_LOG] Timer started with cube type: $solveCubeType")

        isRunning = true
        onTimerRunningChange(true)

        if (resetExternalTrigger) {
            onExternalTriggerHandled()
        }
    }


    // Track if this is the first time the app is launched
    var isFirstLaunch = remember { mutableStateOf(true) }

    // Track if this is the first cube type change
    var isFirstCubeTypeChange = remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        if (isFirstLaunch.value) {
            // On first launch, showWithDelay is already true (set in the declaration)
            isFirstLaunch.value = false
        } else if (!isRunning) {
            showWithDelay.value = false
            delay(500)
            showWithDelay.value = true
        } else {
            showWithDelay.value = false
        }
    }


    // Handle external trigger
    LaunchedEffect(externalTrigger) {
        if (externalTrigger) {
            // Ignore external trigger if in cooldown
            if (isInCooldown) {
                onExternalTriggerHandled()
                return@LaunchedEffect
            }

            if (isRunning) {
                isRunning = false
                showControls = true
                // Notify parent about timer state change
                onTimerRunningChange(false)

                // Set cooldown to prevent immediate restart

            } else {
                if (elapsedTime > 0) {
                    // Reset if already stopped
                    resetTimerState()
                } else {
                    // We should NOT save the solve when starting the timer
                    // Reset timer state and completedSolve
                    resetTimerState(resetCompletedSolve = true)

                    // Start timer
                    startTimer()
                }
            }
            onExternalTriggerHandled()
        }
    }

    // Format time as SS.ms, MM:SS.ms, or HH h MM:SS.ms


    // Timer effect
    val timerPrimaryColor = MaterialTheme.colorScheme.onPrimary

    // Update timer color when theme changes
    LaunchedEffect(timerPrimaryColor) {
        timerColor = timerPrimaryColor
    }

    LaunchedEffect(isRunning) {
        // Notify parent about timer state change
        onTimerRunningChange(isRunning)

        if (isRunning) {
            startTime = System.currentTimeMillis() - elapsedTime
            // Reset color to white when running
            timerColor = timerPrimaryColor
            while (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                delay(10) // Update every 10ms for smooth display
            }
            // Show controls when timer stops
            showControls = true
        }
    }


    // Effect to reset timer when cube type changes
    LaunchedEffect(selectedCubeType) {
        println("[DEBUG_LOG] Cube type changed to: $selectedCubeType")

        if (isFirstCubeTypeChange.value) {
            // Skip generating a new scramble on the first launch
            // because we already have one from the initial state
            println("[DEBUG_LOG] First cube type change, skipping scramble generation")
            isFirstCubeTypeChange.value = false
        } else if (!isRunning) {
            // Reset timer state when cube type changes
            println("[DEBUG_LOG] Timer not running, calling resetTimerState")
            resetTimerState(resetCompletedSolve = true, generateNewScramble = true)

            // Reset all previous state variables to prevent bugs when paging after changing cube types
            previousElapsedTime = 0L
            previousIsDNF = false
            previousHasAddedTwoSeconds = false
            previousCompletedSolve = null
            previousShowControls = false
            previousScramble = ""
            previousSelectedCubeType = selectedCubeType
            previousSelectedTag = selectedTag
            previousCommentText = ""
            println("[DEBUG_LOG] Reset all previous state variables after cube type change")
        } else {
            // Even if timer is running, update the scramble for the next solve
            println("[DEBUG_LOG] Timer running, generating scramble directly")

            // Reset all previous state variables to prevent bugs when paging after changing cube types
            // Even when the timer is running, we need to reset these to prevent issues
            previousElapsedTime = 0L
            previousIsDNF = false
            previousHasAddedTwoSeconds = false
            previousCompletedSolve = null
            previousShowControls = false
            previousScramble = ""
            previousSelectedCubeType = selectedCubeType
            previousSelectedTag = selectedTag
            previousCommentText = ""
            println("[DEBUG_LOG] Reset all previous state variables after cube type change (timer running)")

            isScrambleLoading = true
            // Use a coroutine to avoid blocking the UI
            timerCoroutineScope.launch {
                println("[DEBUG_LOG] Generating scramble for cube type: $selectedCubeType in LaunchedEffect")
                try {
                    val newScramble = generateScramble(CubeType.fromDisplayName(selectedCubeType))
                    println("[DEBUG_LOG] Scramble generated in LaunchedEffect: ${newScramble.take(20)}...")
                    scramble = newScramble
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Error generating scramble in LaunchedEffect: ${e.message}")
                    // Generate a simple fallback scramble if the real one fails
                    scramble = "R U R' U'"
                } finally {
                    isScrambleLoading = false
                    println("[DEBUG_LOG] Finished scramble generation in LaunchedEffect")
                }
            }
        }
    }

    // Effect to update completedSolve when previousSolve changes
    LaunchedEffect(previousSolve) {
        // Only update if we have a completedSolve and previousSolve has a valid ID
        if (completedSolve != null && previousSolve != null && previousSolve.id.isNotEmpty()) {
            // Check if completedSolve needs to be updated with the ID from previousSolve
            if (completedSolve!!.id.isEmpty() || completedSolve!!.id != previousSolve.id) {
                println("[DEBUG_LOG] Updating completedSolve with ID from previousSolve: ${previousSolve.id}")
                completedSolve = completedSolve!!.copy(id = previousSolve.id)
            }
        }
    }

    // Effect to reset timer when selectedTag changes
    LaunchedEffect(selectedTag) {
        // Reset timer state when tag changes
        if (isRunning) {
            isRunning = false
            onTimerRunningChange(false)
        }

        // Reset timer state and generate new scramble
        resetTimerState(resetCompletedSolve = true, generateNewScramble = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) Zona clicable general (Scramble + Stats + zona vacía)
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { // Using Unit as key to prevent reinitialization during operations
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for at least one pointer event
                            val event = awaitPointerEvent()

                            // Ignore all events when in cooldown
                            if (isInCooldown) {
                                println("[DEBUG_LOG] IGNORADO POR COOLDOWN - Event type: ${event.type}")
                                continue
                            }

                            when (event.type) {
                                PointerEventType.Press -> {
                                    // Check if this is a multi-touch event (more than one pointer)
                                    if (event.changes.size > 1) {
                                        // Ignore multi-touch events to prevent timer getting stuck
                                        println("[DEBUG_LOG] Ignoring multi-touch event")
                                        // Reset all flags to prevent the timer from getting stuck
                                        isPaging = false
                                        isScreenPressed = false
                                        hasMovedWhilePressed = false
                                        println("[DEBUG_LOG] Reset flags due to multi-touch: isPaging=$isPaging, isScreenPressed=$isScreenPressed")
                                        continue
                                    }

                                    if (!isRunning) {

                                        // Store the previous solve state before resetting
                                        previousElapsedTime = elapsedTime
                                        previousIsDNF = isDNF
                                        previousHasAddedTwoSeconds = hasAddedTwoSeconds
                                        previousCompletedSolve = completedSolve
                                        previousShowControls = showControls
                                        previousScramble = scramble
                                        previousSelectedCubeType = selectedCubeType
                                        previousSelectedTag = selectedTag
                                        previousCommentText = commentText
                                        println(
                                            "[DEBUG_LOG] Stored previous state: cube=$selectedCubeType, tag=$selectedTag, comment=$commentText, scramble=${
                                                scramble.take(
                                                    20
                                                )
                                            }..."
                                        )

                                        // Reset the movement tracking flag
                                        hasMovedWhilePressed = false

                                        // Store the initial position
                                        val position = event.changes.first().position
                                        initialPressX = position.x
                                        initialPressY = position.y

                                        // When timer is not running and screen is pressed:
                                        // 1. Set isScreenPressed to true
                                        // 2. Reset timer to 0.00 but don't start it
                                        // 3. Keep the control buttons visible (don't call resetTimerState)
                                        isScreenPressed = true

                                        // Manually reset state variables without hiding controls
                                        elapsedTime = 0L // Ensure timer shows 0.00
                                        isDNF = false
                                        hasAddedTwoSeconds = false
                                        originalTime = 0L

                                        // Only reset completedSolve, but keep showControls as is
                                        if (completedSolve != null) {
                                            completedSolve = null
                                        }
                                    } else {

                                        // When running, stop on first touch (same as before)
                                        isRunning = false
                                        showControls = true
                                        onTimerRunningChange(false)

                                        // Set cooldown to prevent immediate restart
                                        setCooldown(1000) // 1000ms cooldown for touch event

                                        // Create a Solve object when the timer stops
                                        val solveStatus =
                                            if (isDNF) SolveStatus.DNF else if (hasAddedTwoSeconds) SolveStatus.PLUS2 else SolveStatus.OK

                                        // Only create a new solve if we don't already have one
                                        if (completedSolve == null) {
                                            // Always use the current top bar values for cube type and tag
                                            // Always use the comment from AppState
                                            val cubeTypeToUse = AppState.selectedCubeType
                                            val tagToUse = AppState.selectedTag
                                            val scrambleToUse = if (isPaging) previousScramble else scramble
                                            val commentsToUse = AppState.commentText

                                            val cubeType = CubeType.fromDisplayName(cubeTypeToUse)
                                            println("[DEBUG_LOG] Creating solve with cube type: ${cubeType.name} from AppState.selectedCubeType: $cubeTypeToUse")
                                            println("[DEBUG_LOG] Using tag: $tagToUse, comments: $commentsToUse")

                                            completedSolve = Solve(
                                                id = "",  // Empty ID for new solves
                                                cube = cubeType,
                                                tagId = tagToUse,
                                                timestamp = Timestamp.now(),
                                                time = elapsedTime,
                                                scramble = scrambleToUse,
                                                status = solveStatus,
                                                comments = commentsToUse
                                            )

                                            // Save the solve immediately
                                            onSolveComplete(completedSolve!!)

                                            // Reset comment text for next solve
                                            AppState.commentText = ""
                                            commentText = ""
                                            println("[DEBUG_LOG] Reset comment text for next solve")
                                        }

                                        // Increment stats refresh trigger to update statistics
                                        statsRefreshTrigger += 1

                                        // Generate a new scramble for the next solve
                                        // Use the cube type from when the solve started, not the currently selected one
                                        isScrambleLoading = true
                                        timerCoroutineScope.launch {
                                            println("[DEBUG_LOG] Generating scramble after solve completion for cube type: $solveCubeType")
                                            try {
                                                val newScramble =
                                                    generateScramble(CubeType.fromDisplayName(solveCubeType))
                                                println("[DEBUG_LOG] Scramble generated after solve: ${newScramble.take(20)}...")
                                                scramble = newScramble
                                            } catch (e: Exception) {
                                                println("[DEBUG_LOG] Error generating scramble after solve: ${e.message}")
                                                // Generate a simple fallback scramble if the real one fails
                                                scramble = "R U R' U'"
                                            } finally {
                                                isScrambleLoading = false
                                                println("[DEBUG_LOG] Finished scramble generation after solve")
                                            }
                                        }
                                    }

                                }

                                PointerEventType.Move -> {
                                    // If the screen is pressed and the timer is not running,
                                    // check if the movement exceeds the threshold
                                    if (isScreenPressed && !isRunning) {
                                        val position = event.changes.first().position
                                        val deltaX = kotlin.math.abs(position.x - initialPressX)
                                        val deltaY = kotlin.math.abs(position.y - initialPressY)

                                        // Check for horizontal movement (likely paging)
                                        if (deltaX > horizontalPagingThreshold && deltaX > deltaY * 1.5f) {
                                            // This is likely a paging gesture (horizontal swipe)
                                            isPaging = true
                                            hasMovedWhilePressed = true

                                            // Restore previous state immediately when paging starts
                                            elapsedTime = previousElapsedTime
                                            isDNF = previousIsDNF
                                            hasAddedTwoSeconds = previousHasAddedTwoSeconds
                                            completedSolve = previousCompletedSolve
                                            showControls = previousShowControls
                                            scramble = previousScramble
                                            // Don't update the actual selectedCubeType/selectedTag variables as they are parameters
                                            // but make sure they're preserved in the completedSolve if it's created
                                            commentText = previousCommentText

                                            println("[DEBUG_LOG] Paging detected: horizontal movement $deltaX pixels (threshold: $horizontalPagingThreshold)")
                                            println(
                                                "[DEBUG_LOG] Restored previous state: cube=${previousSelectedCubeType}, tag=${previousSelectedTag}, comment=${previousCommentText}, scramble=${
                                                    previousScramble.take(
                                                        20
                                                    )
                                                }..."
                                            )
                                        }
                                    }
                                }

                                PointerEventType.Release -> {
                                    println("[DEBUG_LOG] Release: isPaging=$isPaging, isScreenPressed=$isScreenPressed, isRunning=$isRunning")

                                    if (isScreenPressed && !isRunning) {
                                        // Set isScreenPressed to false
                                        isScreenPressed = false

                                        if (isPaging) {
                                            // If paging was detected, don't start the timer
                                            println("[DEBUG_LOG] Release during paging - ignoring timer start")
                                            // Este bloque NO inicia el timer, ni hace más...
                                        } else {
                                            // Start the timer if paging was not detected
                                            // This should always happen when the user holds and releases their finger
                                            // without paging, even when there's no previous timer
                                            println("[DEBUG_LOG] Starting timer after release")
                                            startTimer(resetExternalTrigger = true)
                                            setCooldown(80)
                                        }
                                    }

                                    // SIEMPRE, pase lo que pase, fuera del if:
                                    println("[DEBUG_LOG] Resetting flags: isPaging=$isPaging, hasMovedWhilePressed=$hasMovedWhilePressed")
                                    isPaging = false
                                    hasMovedWhilePressed = false
                                    isScreenPressed = false
                                }

                                else -> {
                                    // Check if this is a Cancel event by name
                                    if (event.type.toString() == "Cancel") {
                                        println("[DEBUG_LOG] Detected Cancel event: isPaging=$isPaging, isScreenPressed=$isScreenPressed, isRunning=$isRunning")

                                        // Always reset all flags on Cancel to prevent the timer from getting stuck
                                        isPaging = false
                                        isScreenPressed = false
                                        hasMovedWhilePressed = false
                                        println("[DEBUG_LOG] Reset flags due to Cancel event")
                                    } else {
                                        println("[DEBUG_LOG] Ignoring other event type: ${event.type}")
                                    }
                                }
                            }
                        }
                    }
                }
        )

        // 2) Contenido de la UI, por encima de esa capa táctil
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            // a) Scramble + estadísticas solo si NO está corriendo
            FixedSizeAnimatedVisibility(
                visible = showWithDelay.value,
                modifier = Modifier.offset(y = (-24).dp),
                enter = slideInVertically(
                    initialOffsetY = { -it + 175 },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                ScrambleBar(
                    initialScramble = scramble, // Pass the Timer's scramble as initialScramble
                    isLoading = isScrambleLoading, // Pass the loading state
                    onEdit = { /* Edit logic */ },
                    onShuffle = {
                        // Only update the scramble if not in cooldown
                        if (!isInCooldown) {
                            // Update the Timer's scramble when the ScrambleBar's scramble changes
                            println("[DEBUG_LOG] Shuffle button clicked, generating new scramble")
                            isScrambleLoading = true
                            timerCoroutineScope.launch {
                                println("[DEBUG_LOG] Generating scramble for cube type: $selectedCubeType in onShuffle")
                                try {
                                    val newScramble = generateScramble(CubeType.fromDisplayName(selectedCubeType))
                                    println("[DEBUG_LOG] Scramble generated in onShuffle: ${newScramble.take(20)}...")
                                    scramble = newScramble
                                } catch (e: Exception) {
                                    println("[DEBUG_LOG] Error generating scramble in onShuffle: ${e.message}")
                                    // Generate a simple fallback scramble if the real one fails
                                    scramble = "R U R' U'"
                                } finally {
                                    isScrambleLoading = false
                                    println("[DEBUG_LOG] Finished scramble generation in onShuffle")
                                }
                            }
                        } else {
                            println("[DEBUG_LOG] Shuffle button clicked but ignored due to cooldown")
                        }
                    },
                    onScrambleClick = {
                        // Check if we're in cooldown period
                        if (!isInCooldown) {
                            // Reset timer state and completedSolve
                            resetTimerState(resetCompletedSolve = true)

                            isRunning = true
                            onTimerRunningChange(true)
                        }
                    }
                )
            }


            // b) Zona donde va el display del tiempo y controles
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                // Now place the timer display and controls on top
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                    val verticalPadding = if (isLandscape) 2.dp else 8.dp

                    // Use the TimerDisplay component
                    TimerDisplay(
                        elapsedTime = elapsedTime,
                        timerColor = timerColor,
                        timerScale = timerScale,
                        hasAddedTwoSeconds = hasAddedTwoSeconds,
                        isDNF = isDNF,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = verticalPadding)
                    )

                    // Control icons that appear when timer stops
                    if (showControls && !isRunning && elapsedTime >= 0) {
                        // Get the current screen configuration
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp
                        val screenHeight = configuration.screenHeightDp

                        // Check if we're in landscape mode (width > height)
                        val isLandscape = screenWidth > screenHeight

                        // Button and icon sizes depend on orientation
                        val buttonSize = if (isLandscape) 40.dp else 48.dp
                        val iconSize = if (isLandscape) 25.dp else 28.dp

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Icon to remove the attempt
                            IconButton(
                                onClick = {
                                    // If the solve has been saved (has an ID), delete it from Firebase
                                    if (completedSolve != null) {
                                        // Check if we have a valid ID
                                        var solveIdToDelete = completedSolve!!.id

                                        if (solveIdToDelete.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                                            // Use the ID from previousSolve if completedSolve doesn't have one
                                            solveIdToDelete = previousSolve!!.id
                                            println("[DEBUG_LOG] Using ID from previousSolve for delete: $solveIdToDelete")
                                        }

                                        // Log the ID for debugging
                                        println("[DEBUG_LOG] Attempting to delete solve with ID: $solveIdToDelete")

                                        if (solveIdToDelete.isNotEmpty()) {
                                            // Use the coroutine scope defined at the composable level
                                            timerCoroutineScope.launch {
                                                try {
                                                    val success = firebaseRepository.deleteSolve(solveIdToDelete)

                                                    // Log the result for debugging
                                                    println("[DEBUG_LOG] Delete result: $success for ID: $solveIdToDelete")

                                                    // Only update UI if delete was successful
                                                    if (success) {
                                                        // Increment stats refresh trigger to update statistics
                                                        statsRefreshTrigger += 1

                                                        // Update UI on the main thread
                                                        resetTimerState(resetCompletedSolve = true)
                                                        // Note: We can't clear previousSolve here as it's a val in the parent composable
                                                    }
                                                } catch (e: Exception) {
                                                    // Log the error
                                                    println("[DEBUG_LOG] Error deleting solve: ${e.message}")
                                                }
                                            }
                                        } else {
                                            println("[DEBUG_LOG] Cannot delete solve: ID is empty")
                                            // Still reset UI for empty ID
                                            resetTimerState(resetCompletedSolve = true)
                                        }
                                    } else {
                                        println("[DEBUG_LOG] Cannot delete solve: completedSolve is null")
                                        // Still reset UI for null completedSolve
                                        resetTimerState(resetCompletedSolve = true)
                                    }
                                },
                                modifier = Modifier
                                    .size(buttonSize)
                                    .background(Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attempt",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(iconSize)
                                )
                            }

                            // Icon to mark as DNF (Did Not Finish)
                            IconButton(
                                onClick = {
                                    if (isDNF) {
                                        // If already DNF, restore original time
                                        isDNF = false

                                        // Update the completed solve status
                                        if (completedSolve != null) {
                                            // Check if we have a valid ID
                                            if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                                                // Use the ID from previousSolve if completedSolve doesn't have one
                                                println("[DEBUG_LOG] Using ID from previousSolve for DNF toggle: ${previousSolve!!.id}")
                                                completedSolve = completedSolve!!.copy(
                                                    id = previousSolve!!.id,
                                                    status = if (hasAddedTwoSeconds) SolveStatus.PLUS2 else SolveStatus.OK
                                                )
                                            } else {
                                                // Use the existing ID
                                                completedSolve = completedSolve!!.copy(
                                                    status = if (hasAddedTwoSeconds) SolveStatus.PLUS2 else SolveStatus.OK
                                                )
                                            }

                                            // Save the updated solve immediately
                                            if (completedSolve!!.id.isNotEmpty()) {
                                                println("[DEBUG_LOG] Updating solve with ID: ${completedSolve!!.id}")
                                                onSolveComplete(completedSolve!!)

                                                // Increment stats refresh trigger to update statistics
                                                statsRefreshTrigger += 1
                                            } else {
                                                println("[DEBUG_LOG] Cannot update solve: ID is empty")
                                            }
                                        }
                                    } else {
                                        // Mark as DNF, save original time
                                        isDNF = true
                                        if (originalTime == 0L) {
                                            originalTime = elapsedTime
                                        }

                                        // Update the completed solve status
                                        if (completedSolve != null) {
                                            // Check if we have a valid ID
                                            if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                                                // Use the ID from previousSolve if completedSolve doesn't have one
                                                println("[DEBUG_LOG] Using ID from previousSolve for DNF: ${previousSolve!!.id}")
                                                completedSolve = completedSolve!!.copy(
                                                    id = previousSolve!!.id,
                                                    status = SolveStatus.DNF
                                                )
                                            } else {
                                                // Use the existing ID
                                                completedSolve = completedSolve!!.copy(
                                                    status = SolveStatus.DNF
                                                )
                                            }

                                            // Save the updated solve immediately
                                            if (completedSolve!!.id.isNotEmpty()) {
                                                println("[DEBUG_LOG] Updating solve with ID: ${completedSolve!!.id}")
                                                onSolveComplete(completedSolve!!)

                                                // Increment stats refresh trigger to update statistics
                                                statsRefreshTrigger += 1
                                            } else {
                                                println("[DEBUG_LOG] Cannot update solve: ID is empty")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(buttonSize)
                                    .background(Color.Transparent, CircleShape)
                            ) {
                                Text(
                                    text = "DNF",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isLandscape) 12.sp else 14.sp
                                )
                            }

                            // Icon to add/remove 2 seconds
                            IconButton(
                                onClick = {
                                    if (!hasAddedTwoSeconds) {
                                        elapsedTime += 2000 // Add 2 seconds (2000 ms)
                                        hasAddedTwoSeconds = true
                                        if (originalTime == 0L) {
                                            originalTime = elapsedTime - 2000 // Store original time before +2
                                        }

                                        // Update the completed solve
                                        if (completedSolve != null) {
                                            // Check if we have a valid ID
                                            if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                                                // Use the ID from previousSolve if completedSolve doesn't have one
                                                println("[DEBUG_LOG] Using ID from previousSolve for +2: ${previousSolve!!.id}")
                                                completedSolve = completedSolve!!.copy(
                                                    id = previousSolve!!.id,
                                                    time = elapsedTime,
                                                    status = if (!isDNF) SolveStatus.PLUS2 else SolveStatus.DNF
                                                )
                                            } else {
                                                // Use the existing ID
                                                completedSolve = completedSolve!!.copy(
                                                    time = elapsedTime,
                                                    status = if (!isDNF) SolveStatus.PLUS2 else SolveStatus.DNF
                                                )
                                            }

                                            // Save the updated solve immediately
                                            if (completedSolve!!.id.isNotEmpty()) {
                                                println("[DEBUG_LOG] Updating solve with ID: ${completedSolve!!.id}")
                                                onSolveComplete(completedSolve!!)

                                                // Increment stats refresh trigger to update statistics
                                                statsRefreshTrigger += 1
                                            } else {
                                                println("[DEBUG_LOG] Cannot update solve: ID is empty")
                                            }
                                        }
                                    } else {
                                        elapsedTime -= 2000 // Remove 2 seconds (2000 ms)
                                        hasAddedTwoSeconds = false

                                        // Update the completed solve
                                        if (completedSolve != null) {
                                            // Check if we have a valid ID
                                            if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                                                // Use the ID from previousSolve if completedSolve doesn't have one
                                                println("[DEBUG_LOG] Using ID from previousSolve for -2: ${previousSolve!!.id}")
                                                completedSolve = completedSolve!!.copy(
                                                    id = previousSolve!!.id,
                                                    time = elapsedTime,
                                                    status = if (!isDNF) SolveStatus.OK else SolveStatus.DNF
                                                )
                                            } else {
                                                // Use the existing ID
                                                completedSolve = completedSolve!!.copy(
                                                    time = elapsedTime,
                                                    status = if (!isDNF) SolveStatus.OK else SolveStatus.DNF
                                                )
                                            }

                                            // Save the updated solve immediately
                                            if (completedSolve!!.id.isNotEmpty()) {
                                                println("[DEBUG_LOG] Updating solve with ID: ${completedSolve!!.id}")
                                                onSolveComplete(completedSolve!!)

                                                // Increment stats refresh trigger to update statistics
                                                statsRefreshTrigger += 1
                                            } else {
                                                println("[DEBUG_LOG] Cannot update solve: ID is empty")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(buttonSize)
                                    .background(Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (hasAddedTwoSeconds) Icons.Default.Tag else Icons.Default.Add,
                                    contentDescription = if (hasAddedTwoSeconds) "Remove 2 seconds" else "Add 2 seconds",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(iconSize)
                                )
                            }

                            // Icon to add comment
                            IconButton(
                                onClick = {
                                    // Show a dialog to add a comment
                                    if (completedSolve != null) {
                                        // Initialize comment text with current comment (if any)
                                        commentText = completedSolve!!.comments
                                        // Show comment dialog
                                        showCommentDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .size(buttonSize)
                                    .background(Color.Transparent, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Add comment",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    } else {
                        // Get the current screen configuration for the spacer
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp
                        val screenHeight = configuration.screenHeightDp

                        // Check if we're in landscape mode (width > height)
                        val isLandscape = screenWidth > screenHeight

                        // Spacer size depends on orientation
                        val spacerSize = if (isLandscape) 40.dp else 48.dp

                        Spacer(modifier = Modifier.size(spacerSize))
                    }
                }
            }

            FixedSizeAnimatedVisibility(
                visible = showWithDelay.value,

                enter = slideInVertically(
                    initialOffsetY = { it - 200 },
                    animationSpec = tween(durationMillis = 600)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                StatisticsComponent(
                    selectedCubeType = selectedCubeType,
                    selectedTag = selectedTag,
                    refreshTrigger = statsRefreshTrigger,
                    scramble = scramble,
                    isLoading = isScrambleLoading
                )
            }
        }
    }

    // Comment Dialog
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("Add Comment") },
            text = {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Comment") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Update the solve with the comment
                        if (completedSolve != null) {
                            // Check if we have a valid ID
                            if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                                // Use the ID from previousSolve if completedSolve doesn't have one
                                println("[DEBUG_LOG] Using ID from previousSolve for comment: ${previousSolve!!.id}")
                                completedSolve = completedSolve!!.copy(
                                    id = previousSolve!!.id,
                                    comments = commentText
                                )
                            } else {
                                // Use the existing ID
                                completedSolve = completedSolve!!.copy(comments = commentText)
                            }

                            // Save the updated solve immediately
                            if (completedSolve!!.id.isNotEmpty()) {
                                println("[DEBUG_LOG] Updating solve with ID: ${completedSolve!!.id}")
                                onSolveComplete(completedSolve!!)

                                // Increment stats refresh trigger to update statistics
                                statsRefreshTrigger += 1
                            } else {
                                println("[DEBUG_LOG] Cannot update solve: ID is empty")
                            }
                        }

                        // Reset AppState.commentText to empty string for next solve
                        AppState.commentText = ""
                        println("[DEBUG_LOG] Reset AppState.commentText for next solve")

                        // Close the dialog
                        showCommentDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCommentDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


/**
 * Generates a scramble for the current cube type.
 * This function is a wrapper around the ScrambleGenerator class.
 */
fun generateScramble(cubeType: CubeType): String {
    return com.example.cubespeed.model.ScrambleGenerator.getInstance().generateScramble(cubeType)
}

@Composable
fun StatisticsCard(
    modifier: Modifier = Modifier,
    selectedCubeType: String = CubeType.CUBE_3X3.displayName,
    selectedTag: String = "normal",
    refreshTrigger: Long = 0 // Parameter to trigger refresh
) {
    // State for statistics
    var stats by remember { mutableStateOf<SolveStatistics>(SolveStatistics()) }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Effect to update statistics when refreshTrigger changes
    LaunchedEffect(refreshTrigger, selectedCubeType, selectedTag) {
        val cubeType = CubeType.fromDisplayName(selectedCubeType)
        stats = firebaseRepository.getStats(cubeType, selectedTag)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Average: ${formatDouble(stats.average)}", color = MaterialTheme.colorScheme.onPrimary)
            Text("Best: ${formatTime(stats.best)}", color = MaterialTheme.colorScheme.onPrimary)
            Text("Count: ${stats.count}", color = MaterialTheme.colorScheme.onPrimary)
            Text("Deviation: ${formatDouble(stats.deviation)}", color = MaterialTheme.colorScheme.onPrimary)
            Text(
                "Ao5: ${formatDouble(stats.ao5)}  •  Ao12: ${formatDouble(stats.ao12)}  •  Ao50: ${formatDouble(stats.ao50)}  •  Ao100: ${
                    formatDouble(
                        stats.ao100
                    )
                }", color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun StatisticsComponent(
    modifier: Modifier = Modifier,
    selectedCubeType: String,
    selectedTag: String,
    refreshTrigger: Long = 0, // New parameter to trigger refresh
    scramble: String = "", // Parameter for the current scramble
    isLoading: Boolean = false // Parameter to indicate if scramble is loading
) {
    // State for statistics
    var stats by remember { mutableStateOf<SolveStatistics>(SolveStatistics()) }

    // State for count from direct query
    var solveCount by remember { mutableStateOf(0) }

    // State for user ID that will be updated when auth state changes
    var userId by remember { mutableStateOf(Firebase.auth.currentUser?.uid) }

    // Effect to listen for auth state changes and update userId
    DisposableEffect(Unit) {
        // Set up auth state listener
        val authStateListener = { auth: com.google.firebase.auth.FirebaseAuth ->
            // Update userId when auth state changes
            userId = auth.currentUser?.uid
        }

        // Add the auth state listener
        Firebase.auth.addAuthStateListener(authStateListener)

        // Remove the auth state listener when the effect is disposed
        onDispose {
            Firebase.auth.removeAuthStateListener(authStateListener)
        }
    }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Effect to update the count when refreshTrigger changes
    LaunchedEffect(refreshTrigger, selectedCubeType, selectedTag) {
        val cubeType = CubeType.fromDisplayName(selectedCubeType)
        // Query the count directly from Firestore
        solveCount = firebaseRepository.countSolvesByCubeTypeAndTag(cubeType, selectedTag)
    }

    DisposableEffect(userId, selectedCubeType, selectedTag) {
        val cubeType = CubeType.fromDisplayName(selectedCubeType)
        // Create the stats document ID using the same format as in FirebaseRepository and Cloud Function
        val statsDocId = "${cubeType.name}_${selectedTag}"
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val statsRef = db.collection("users")
            .document(userId!!)
            .collection("stats")
            .document(statsDocId)
        val listener = statsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data
                // Map the fields to SolveStatistics
                stats = SolveStatistics(
                    count = (data?.get("count") as? Long)?.toInt() ?: 0,
                    validCount = (data?.get("validCount") as? Long)?.toInt() ?: 0,
                    best = data?.get("best") as? Long ?: 0,
                    average = (data?.get("average") as? Number)?.toDouble() ?: 0.0,
                    deviation = (data?.get("deviation") as? Number)?.toDouble() ?: 0.0,
                    ao5 = (data?.get("ao5") as? Number)?.toDouble() ?: 0.0,
                    ao12 = (data?.get("ao12") as? Number)?.toDouble() ?: 0.0,
                    ao50 = (data?.get("ao50") as? Number)?.toDouble() ?: 0.0,
                    ao100 = (data?.get("ao100") as? Number)?.toDouble() ?: 0.0
                )

                // Also update the count from the query
                coroutineScope.launch {
                    solveCount = firebaseRepository.countSolvesByCubeTypeAndTag(cubeType, selectedTag)
                }
            } else {
                stats = SolveStatistics() // Empty if it doesn't exist
            }
        }
        onDispose { listener.remove() } // Very important
    }


    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)

            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,

        ) {
        // Left side statistics
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.Start,

            ) {

            Text(
                text = "Average: ${formatDouble(stats.average)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Best: ${formatTime(stats.best)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Count: ${stats.count}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Deviation: ${formatDouble(stats.deviation)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Center - Scramble visualization or loading indicator
        Box(
            modifier = Modifier.weight(1f), // O ponle .height(62.dp) calculando la altura preferida
            contentAlignment = Alignment.BottomCenter
        ) {
            if (isLoading) {
                // Show loading indicator when scramble is loading
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else if (scramble.isNotEmpty()) {
                // Show scramble visualization when not loading and scramble is not empty
                ScrambleVisualization(
                    scramble = scramble,
                    cubeType = CubeType.fromDisplayName(selectedCubeType),
                    modifier = Modifier.size(100.dp)
                )
            }
        }


        // Right side averages
        Column(
            modifier = Modifier

                .weight(1f),

            horizontalAlignment = Alignment.End
        ) {

            Text(
                text = "Ao5: ${formatDouble(stats.ao5)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Ao12: ${formatDouble(stats.ao12)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Ao50: ${formatDouble(stats.ao50)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Ao100: ${formatDouble(stats.ao100)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun ScrambleBar(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    initialScramble: String = "", // Use provided initial scramble, empty string as default to avoid unnecessary generation
    isLoading: Boolean = false, // New parameter to indicate if scramble is loading
    onEdit: (current: String) -> Unit = {},
    onShuffle: () -> Unit = {},
    onScrambleClick: () -> Unit = {}
) {
    // Initialize with the provided initialScramble
    var scramble by remember { mutableStateOf(initialScramble) }

    // Update internal scramble state when initialScramble changes
    LaunchedEffect(initialScramble) {
        // Only update if initialScramble is not empty
        if (initialScramble.isNotEmpty()) {
            scramble = initialScramble
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()

    ) {
        Column(
            modifier = Modifier.padding(5.dp)
        ) {

            if (isLoading) {
                // Show loading indicator and text when scramble is loading
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Generando scramble...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Show the scramble text when not loading
                Text(
                    text = scramble,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // No ripple effect
                        ) {
                            onScrambleClick()
                        }
                )
            }

            // Icons at the bottom right
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit icon
                IconButton(
                    onClick = { onEdit(scramble) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar scramble",
                        tint = contentColor
                    )
                }

                // Refresh icon
                IconButton(
                    onClick = {
                        // Just call onShuffle which will update the Timer's scramble
                        // and the ScrambleBar will get the new scramble on recomposition
                        onShuffle()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Nueva scramble",
                        tint = contentColor
                    )
                }
            }
        }
    }
}


@Composable
fun CubeSelectionDialog(
    cubeTypes: List<String>,
    onCubeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Cube Type",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(cubeTypes) { cubeType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCubeSelected(cubeType) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cubeType,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun TagInputDialog(
    currentTag: String,
    onTagConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // State for the list of tags
    var tags by remember { mutableStateOf(listOf<String>()) }

    // State for the new tag input
    var newTagText by remember { mutableStateOf("") }

    // State for the selected tag
    var selectedTag by remember { mutableStateOf(currentTag) }

    // State for showing error message
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // State for showing the add tag input field
    var showAddTagInput by remember { mutableStateOf(false) }

    // State for showing the delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf("") }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Load tags when the dialog is shown
    LaunchedEffect(Unit) {
        tags = firebaseRepository.getTags()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .height(320.dp) // Reduced height for more compact design
            ) {
                // Header with title and add button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tags",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    IconButton(
                        onClick = { showAddTagInput = !showAddTagInput },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Add new tag",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Add tag input field (only shown when edit button is clicked)
                AnimatedVisibility(visible = showAddTagInput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            label = { Text("New Tag") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (newTagText.isNotBlank()) {
                                    coroutineScope.launch {
                                        val success = firebaseRepository.addTag(newTagText)
                                        if (success) {
                                            // Refresh the tag list
                                            tags = firebaseRepository.getTags()
                                            // Clear the input field
                                            newTagText = ""
                                            // Hide the input field
                                            showAddTagInput = false
                                        } else {
                                            showError = true
                                            errorMessage = "Failed to add tag"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add tag",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // List of existing tags
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(tags) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTag = tag
                                    AppState.selectedTag = tag
                                    onTagConfirmed(tag)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp), // Reduced padding for more compact design
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Tag name with selection indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedTag == tag) {
                                    Icon(
                                        imageVector = Icons.Default.Tag,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp) // Slightly smaller icon
                                    )
                                    Spacer(modifier = Modifier.width(6.dp)) // Reduced spacing
                                }

                                Text(
                                    text = tag,
                                    fontSize = 16.sp,
                                    fontWeight = if (selectedTag == tag) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // Delete button (not shown for "normal" tag)
                            if (tag != "normal") {
                                IconButton(
                                    onClick = {
                                        tagToDelete = tag
                                        showDeleteConfirmation = true
                                    },
                                    modifier = Modifier.size(32.dp) // Smaller button for more compact design
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete tag",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp) // Smaller icon
                                    )
                                }
                            }
                        }
                    }
                }

                // Error message
                if (showError) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp) // Reduced padding
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp), // Reduced padding
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Tag") },
                text = { Text("Are you sure you want to delete the tag \"$tagToDelete\"? This will delete all solves with this tag.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = firebaseRepository.removeTag(tagToDelete)
                                if (success) {
                                    // If the deleted tag was selected, select "normal"
                                    if (selectedTag == tagToDelete) {
                                        selectedTag = "normal"
                                        AppState.selectedTag = "normal"
                                        // Apply the selection immediately
                                        onTagConfirmed(selectedTag)
                                        // Dismiss the dialog
                                        onDismiss()
                                    }
                                    // Refresh the tag list
                                    tags = firebaseRepository.getTags()
                                } else {
                                    showError = true
                                    errorMessage = "Failed to delete tag"
                                }
                                showDeleteConfirmation = false
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmation = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Function to calculate font size based on text length and screen orientation
 */
@Composable
fun calculateFontSize(text: String): TextUnit {
    // Get the current screen configuration
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Check if we're in landscape mode (width > height)
    val isLandscape = screenWidth > screenHeight

    // Base size depends on orientation
    val baseSize = if (isLandscape) {
        // In landscape, use a smaller base size
        60.sp
    } else {
        // In portrait, use the original base size
        120.sp
    }

    // Adjust size based on text length
    return when {
        text.length <= 3 -> baseSize
        text.length <= 5 -> baseSize.times(0.85f)
        text.length <= 7 -> baseSize.times(0.7f)
        text.length <= 9 -> baseSize.times(0.6f)
        text.length <= 12 -> baseSize.times(0.5f)
        else -> baseSize.times(0.4f)
    }
}

fun formatTime(timeMillis: Long): String {
    val hours = (timeMillis / 3600000).toInt()
    val minutes = ((timeMillis % 3600000) / 60000).toInt()
    val seconds = ((timeMillis % 60000) / 1000).toInt()
    val millis = ((timeMillis % 1000) / 10).toInt()

    return when {
        hours > 0 -> String.format("%d h %d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%d:%02d.%02d", minutes, seconds, millis)
        else -> String.format("%d.%02d", seconds, millis)
    }
}

fun formatDouble(value: Double): String {
    return when {
        value == 0.0 -> "--"
        value == -1.0 -> "DNF"
        else -> formatTime(value.toLong())
    }
}

/**
 * A reusable composable that displays the timer with proper formatting and styling.
 */
@Composable
fun TimerDisplay(
    elapsedTime: Long,
    timerColor: Color,
    timerScale: Float,
    hasAddedTwoSeconds: Boolean,
    isDNF: Boolean,
    modifier: Modifier = Modifier
) {
    // Check if we should display DNF or the time
    if (isDNF) {
        // Format the time string to calculate the font size
        val timeString = formatTime(elapsedTime)
        val parts = timeString.split(".")
        val mainPart = parts[0]
        val millisPart = if (parts.size > 1) ".${parts[1]}" else ""
        val fullTimeString = mainPart + millisPart + if (hasAddedTwoSeconds) "+" else ""

        // Display DNF but use the timer's font size
        Box(
            modifier = modifier
                .graphicsLayer(
                    scaleX = timerScale,
                    scaleY = timerScale,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                )
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DNF",
                fontSize = calculateFontSize(fullTimeString),
                fontWeight = FontWeight.Bold,
                color = timerColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    } else {
        // Split time into main part and milliseconds
        val timeString = formatTime(elapsedTime)
        val parts = timeString.split(".")
        val mainPart = parts[0]
        val millisPart = if (parts.size > 1) ".${parts[1]}" else ""

        // Wrap both text components in a Box with the graphicsLayer modifier for proper scaling
        Box(
            modifier = modifier
                .graphicsLayer(
                    scaleX = timerScale,
                    scaleY = timerScale,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                )
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(

                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Calculate font size based on the full time string
                val fullTimeString = mainPart + millisPart + if (hasAddedTwoSeconds) "+" else ""
                val calculatedSize = calculateFontSize(fullTimeString)

                // Main part of the time (hours/minutes/seconds)
                Text(
                    text = mainPart,
                    style = TextStyle(
                        fontSize = calculatedSize,
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                        baselineShift = BaselineShift.None
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

                // Milliseconds part with smaller font
                Text(
                    text = millisPart + if (hasAddedTwoSeconds) "+" else "",
                    style = TextStyle(
                        fontSize = calculatedSize.times(0.75f), // Smaller font for milliseconds
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                        baselineShift = BaselineShift(0.082F)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun GreetingPreview() {
    TimerScreen()
}

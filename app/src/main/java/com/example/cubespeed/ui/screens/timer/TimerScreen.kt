package com.example.cubespeed.ui.screens.timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.repository.SolveStatistics
import com.example.cubespeed.ui.theme.StatisticsTextStyle
import com.example.cubespeed.ui.theme.TimerTextColor
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlin.math.min

val userId = Firebase.auth.currentUser?.uid

// Create a single instance of FirebaseRepository to be used throughout the app
val firebaseRepository = FirebaseRepository()

@Composable
fun TimerScreen() {
    // State for the selected cube type and tag
    var selectedCubeType by remember { mutableStateOf(CubeType.CUBE_3X3.displayName) }
    var selectedTag by remember { mutableStateOf("normal") }

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
            .background(Color(0xFF3F51B5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Only show CubeTopBar when timer is not running

            FixedSizeAnimatedVisibility(
                visible = !isTimerRunning,
                enter = fadeIn(animationSpec = tween(durationMillis = 400)),
                exit = fadeOut(animationSpec = tween(durationMillis = 400))
            ) {
                CubeTopBar(
                    title = selectedCubeType,
                    subtitle = selectedTag,
                    onOptionsClick = { showTagDialog = true },
                    onCubeClick = { showCubeSelectionDialog = true }
                )
            }

            // Use the remembered Timer composable
            timer()
        }
    }

    // Cube Selection Dialog
    if (showCubeSelectionDialog) {
        CubeSelectionDialog(
            cubeTypes = cubeTypes,
            onCubeSelected = {
                selectedCubeType = it
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
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var startTime by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(false) }
    val showWithDelay = remember { mutableStateOf(false) }

    // Coroutine scope for async operations
    val timerCoroutineScope = rememberCoroutineScope()

    // Base timer size, will be adjusted based on text length
    var timerSize by remember { mutableStateOf(120.sp) }

    // Function to calculate font size based on text length
    fun calculateFontSize(text: String): TextUnit {
        // Base size for short text (1-3 characters)
        val baseSize = 120.sp

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

    var timerColor by remember { mutableStateOf(Color.White) }
    var isDNF by remember { mutableStateOf(false) }
    var originalTime by remember { mutableLongStateOf(0L) }
    var hasAddedTwoSeconds by remember { mutableStateOf(false) }
    var scramble by remember { mutableStateOf(generateScramble()) }
    var statsRefreshTrigger by remember { mutableLongStateOf(0L) }

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
    var completedSolve by remember { mutableStateOf<Solve?>(null) }


    LaunchedEffect(isRunning) {
        if (!isRunning) {
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
            if (isRunning) {
                isRunning = false
                showControls = true
                // Notify parent about timer state change
                onTimerRunningChange(false)
            } else {
                if (elapsedTime > 0) {
                    // Reset if already stopped
                    elapsedTime = 0
                    showControls = false
                    isDNF = false
                    hasAddedTwoSeconds = false
                } else {
                    // We should NOT save the solve when starting the timer
                    // Just reset completedSolve if it exists
                    if (completedSolve != null) {
                        completedSolve = null
                    }

                    // Start timer
                    isRunning = true
                    showControls = false
                    isDNF = false
                    hasAddedTwoSeconds = false
                    // Notify parent about timer state change
                    onTimerRunningChange(true)
                }
            }
            onExternalTriggerHandled()
        }
    }

    // Format time as SS.ms, MM:SS.ms, or HH h MM:SS.ms


    // Timer effect
    LaunchedEffect(isRunning) {
        // Notify parent about timer state change
        onTimerRunningChange(isRunning)

        if (isRunning) {
            startTime = System.currentTimeMillis() - elapsedTime
            // Reset color to white when running
            timerColor = Color.White
            while (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                delay(10) // Update every 10ms for smooth display
            }
            // Show controls when timer stops
            showControls = true
        }
    }

    // Effect for timer color
    LaunchedEffect(isRunning) {
        timerColor = Color.White
    }

    // Effect to reset timer when cube type changes
    LaunchedEffect(selectedCubeType) {
        if (!isRunning) {
            // Reset timer state when cube type changes
            elapsedTime = 0
            showControls = false
            isDNF = false
            hasAddedTwoSeconds = false
            originalTime = 0
            // Generate new scramble for the new cube type
            scramble = generateScramble()
            // We should NOT save the solve when changing cube type
            // Just reset completedSolve if it exists
            if (completedSolve != null) {
                completedSolve = null
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) Zona clicable general (Scramble + Stats + zona vacía)
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(isRunning) {
                    detectTapGestures { offset ->
                        // Solo cuando esté parado lanzamos trigger
                        if (!isRunning) {
                            // We should NOT save the solve when starting the timer
                            // Just reset completedSolve if it exists
                            if (completedSolve != null) {
                                completedSolve = null
                            }

                            onExternalTriggerHandled()       // resetea externalTrigger
                            isRunning = true
                            onTimerRunningChange(true)
                            elapsedTime = 0L
                            isDNF = false
                            hasAddedTwoSeconds = false
                            originalTime = 0L // Reset original time
                        } else {
                            // When running, click to stop
                            isRunning = false
                            showControls = true
                            onTimerRunningChange(false)

                            // Create a Solve object when the timer stops
                            val solveStatus =
                                if (isDNF) SolveStatus.DNF else if (hasAddedTwoSeconds) SolveStatus.PLUS2 else SolveStatus.OK

                            // Only create a new solve if we don't already have one
                            if (completedSolve == null) {
                                completedSolve = Solve(
                                    id = "",  // Empty ID for new solves
                                    cube = CubeType.fromDisplayName(selectedCubeType),
                                    tagId = selectedTag,
                                    timestamp = Timestamp.now(),
                                    time = elapsedTime,
                                    scramble = scramble,
                                    status = solveStatus,
                                    comments = ""
                                )

                                // Save the solve immediately
                                onSolveComplete(completedSolve!!)
                            }

                            // Increment stats refresh trigger to update statistics
                            statsRefreshTrigger += 1
                        }
                    }
                }
        )

        // 2) Contenido de la UI, por encima de esa capa táctil
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // a) Scramble + estadísticas solo si NO está corriendo
            FixedSizeAnimatedVisibility(
                visible = showWithDelay.value,
                enter = slideInVertically(
                    initialOffsetY = { -it + 175 },
                    animationSpec = tween(durationMillis = 500)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                ScrambleBar(
                    onEdit = { /* Edit logic */ },
                    onShuffle = { scramble = generateScramble() },
                    onScrambleClick = {
                        // We should NOT save the solve when starting the timer
                        // Just reset completedSolve if it exists
                        if (completedSolve != null) {
                            completedSolve = null
                        }

                        isRunning = true
                        onTimerRunningChange(true)
                        elapsedTime = 0L
                        showControls = false
                        isDNF = false
                        hasAddedTwoSeconds = false
                        originalTime = 0L
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
                    Modifier.padding(bottom = 60.dp), horizontalAlignment = Alignment.CenterHorizontally
                    // Move timer higher on screen
                ) {
                    // Check if we should display DNF or the time
                    if (isDNF) {
                        // Display DNF
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp)
                        ) {
                            // Wrap DNF text in a Box with the scale modifier for consistency
                            Box(
                                modifier = Modifier.scale(timerScale).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "DNF",
                                    fontSize = calculateFontSize("DNF"),
                                    fontWeight = FontWeight.Bold,
                                    color = timerColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    } else {
                        // Split time into main part and milliseconds
                        val timeString = formatTime(elapsedTime)
                        val parts = timeString.split(".")
                        val mainPart = parts[0]
                        val millisPart = if (parts.size > 1) ".${parts[1]}" else ""

                        // Timer display with different sizes for main time and milliseconds
                        Row(
                            verticalAlignment = Alignment.Bottom, // Align items to the bottom
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp)
                        ) {
                            // Wrap both text components in a Box with the scale modifier
                            Box(
                                modifier = Modifier.scale(timerScale).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Calculate font size based on the full time string
                                    val fullTimeString = mainPart + millisPart + if (hasAddedTwoSeconds) "+" else ""
                                    val calculatedSize = calculateFontSize(fullTimeString)

                                    // Main part of the time (hours/minutes/seconds)
                                    Text(
                                        text = mainPart,
                                        fontSize = calculatedSize,
                                        fontWeight = FontWeight.Bold,
                                        color = timerColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )

                                    // Milliseconds part with smaller font
                                    Text(
                                        text = millisPart + if (hasAddedTwoSeconds) "+" else "",
                                        fontSize = calculatedSize.times(0.75f), // Smaller font for milliseconds
                                        fontWeight = FontWeight.Bold,
                                        color = timerColor,
                                        modifier = Modifier.padding(bottom = 5.5.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }

                    // Control icons that appear when timer stops
                    if (showControls && !isRunning && elapsedTime > 0) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
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
                                                        elapsedTime = 0
                                                        showControls = false
                                                        isDNF = false
                                                        hasAddedTwoSeconds = false
                                                        completedSolve = null // Clear the completed solve
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
                                            elapsedTime = 0
                                            showControls = false
                                            isDNF = false
                                            hasAddedTwoSeconds = false
                                            completedSolve = null // Clear the completed solve
                                        }
                                    } else {
                                        println("[DEBUG_LOG] Cannot delete solve: completedSolve is null")
                                        // Still reset UI for null completedSolve
                                        elapsedTime = 0
                                        showControls = false
                                        isDNF = false
                                        hasAddedTwoSeconds = false
                                        completedSolve = null // Clear the completed solve
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF5A57FF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attempt",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
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
                                    .size(48.dp)
                                    .background(Color(0xFF5A57FF), CircleShape)
                            ) {
                                Text(
                                    text = "DNF",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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
                                    .size(48.dp)
                                    .background(Color(0xFF5A57FF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (hasAddedTwoSeconds) Icons.Default.Tag else Icons.Default.Add,
                                    contentDescription = if (hasAddedTwoSeconds) "Remove 2 seconds" else "Add 2 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Icon to add comment
                            IconButton(
                                onClick = {
                                    // Show a dialog to add a comment
                                    // For now, we'll just update the completedSolve with an empty comment
                                    // In a real implementation, you would show a dialog to let the user enter a comment
                                    if (completedSolve != null) {
                                        // Check if we have a valid ID
                                        if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                                            // Use the ID from previousSolve if completedSolve doesn't have one
                                            println("[DEBUG_LOG] Using ID from previousSolve for comment: ${previousSolve!!.id}")
                                            completedSolve = completedSolve!!.copy(
                                                id = previousSolve!!.id,
                                                comments = ""
                                            )
                                        } else {
                                            // Use the existing ID
                                            completedSolve = completedSolve!!.copy(comments = "")
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
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF5A57FF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Add comment",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
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
                    refreshTrigger = statsRefreshTrigger
                )
            }
        }
    }
}

@Composable
fun FixedSizeAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable () -> Unit
) {
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(modifier) {
        AnimatedVisibility(
            visible = visible,
            enter = enter,
            exit = exit
        ) {
            Box(
                Modifier
                    .onSizeChanged { contentSize = it }
            ) {
                content()
            }
        }
        if (!visible && contentSize != IntSize.Zero) {
            Spacer(
                Modifier
                    .requiredWidth(with(density) { contentSize.width.toDp() })
                    .requiredHeight(with(density) { contentSize.height.toDp() })
            )
        }
    }
}


@Composable
fun CubeTopBar(
    modifier: Modifier = Modifier,
    title: String = "3x3 Cube",
    subtitle: String = "normal",
    backgroundColor: Color = Color(0xFF5A57FF),
    contentColor: Color = Color.White,
    onSettingsClick: () -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onCubeClick: () -> Unit = {}
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }

            // Cube type + dropdown and tag centered
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onCubeClick)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Cube Type",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            IconButton(onClick = onOptionsClick) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = "Add Tag"
                )
            }
        }
    }
}

fun generateScramble(): String {
    val moves = listOf("R", "L", "U", "D", "F", "B")
    val suffix = listOf("", "'", "2")
    return List(20) { "${moves.random()}${suffix.random()}" }.joinToString(" ")
}

@Composable
fun StatisticsComponent(
    modifier: Modifier = Modifier,
    selectedCubeType: String = CubeType.CUBE_3X3.displayName,
    selectedTag: String = "normal",
    refreshTrigger: Long = 0 // New parameter to trigger refresh
) {
    // State for statistics
    var stats by remember { mutableStateOf<SolveStatistics>(SolveStatistics()) }

    // State for count from direct query
    var solveCount by remember { mutableStateOf(0) }

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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side statistics
        Column(
            modifier = Modifier
                .padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Average: ${formatDouble(stats.average)}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
            Text(
                text = "Best: ${formatTime(stats.best)}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
            Text(
                text = "Count: ${stats.count}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
            Text(
                text = "Deviation: ${if (stats.deviation > 0) String.format("%.2f", stats.deviation / 1000) else "--"}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
        }

        // Right side averages
        Column(
            modifier = Modifier
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ao5: ${formatDouble(stats.ao5)}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
            Text(
                text = "Ao12: ${formatDouble(stats.ao12)}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
            Text(
                text = "Ao50: ${formatDouble(stats.ao50)}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
            Text(
                text = "Ao100: ${formatDouble(stats.ao100)}",
                style = StatisticsTextStyle,
                color = TimerTextColor
            )
        }
    }
}

@Composable
fun ScrambleBar(
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    onEdit: (current: String) -> Unit = {},
    onShuffle: () -> Unit = {},
    onScrambleClick: () -> Unit = {}
) {
    var scramble by remember { mutableStateOf(generateScramble()) }

    Column(
        modifier = modifier
            .fillMaxWidth()

    ) {
        Column(
            modifier = Modifier.padding(5.dp)
        ) {

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
                        scramble = generateScramble()
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
                            tint = Color(0xFF5A57FF)
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
                                .background(Color(0xFF5A57FF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add tag",
                                tint = Color.White,
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
                                        tint = Color(0xFF5A57FF),
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
                                        coroutineScope.launch {
                                            val success = firebaseRepository.removeTag(tag)
                                            if (success) {
                                                // If the deleted tag was selected, select "normal"
                                                if (selectedTag == tag) {
                                                    selectedTag = "normal"
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
                                        }
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
    }
}

fun formatTime(timeMillis: Long): String {
    val hours = (timeMillis / 3600000).toInt()
    val minutes = ((timeMillis % 3600000) / 60000).toInt()
    val seconds = ((timeMillis % 60000) / 1000).toInt()
    val millis = ((timeMillis % 1000) / 10).toInt()

    return when {
        hours > 0 -> String.format("%d h %d:%02d.%02d", hours, minutes, seconds, millis)
        minutes > 0 -> String.format("%d:%02d.%02d", minutes, seconds, millis)
        else -> String.format("%d.%02d", seconds, millis)
    }
}

fun formatDouble(value: Double): String {
    if (value == 0.0) return "--"
    return formatTime(value.toLong())
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun GreetingPreview() {
    TimerScreen()
}

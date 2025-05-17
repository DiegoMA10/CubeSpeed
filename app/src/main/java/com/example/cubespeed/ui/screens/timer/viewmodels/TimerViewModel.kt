package com.example.cubespeed.ui.screens.timer.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Timer screen.
 * Handles business logic and state management for the timer functionality.
 */
class TimerViewModel(
    private val repository: FirebaseRepository = FirebaseRepository(),
    private val coroutineScope: CoroutineScope
) {
    // Timer state
    var isRunning by mutableStateOf(false)
    var elapsedTime by mutableLongStateOf(0L)
    var startTime by mutableLongStateOf(0L)
    var showControls by mutableStateOf(false)
    var isDNF by mutableStateOf(false)
    var originalTime by mutableLongStateOf(0L)
    var hasAddedTwoSeconds by mutableStateOf(false)

    // Scramble state
    var scramble by mutableStateOf("")
    var isScrambleLoading by mutableStateOf(false)

    // Cube type and tag state
    var selectedCubeType by mutableStateOf(AppState.selectedCubeType)
    var selectedTag by mutableStateOf(AppState.selectedTag)
    var solveCubeType by mutableStateOf(selectedCubeType)

    // Solve state
    var completedSolve by mutableStateOf<Solve?>(null)
    var previousSolve by mutableStateOf<Solve?>(null)

    // Comment state
    var commentText by mutableStateOf("")

    // Statistics refresh trigger
    var statsRefreshTrigger by mutableLongStateOf(0L)

    // Cooldown state
    var isInCooldown by mutableStateOf(false)

    // Paging state
    var isPaging by mutableStateOf(false)
    var isScreenPressed by mutableStateOf(false)
    var hasMovedWhilePressed by mutableStateOf(false)
    var initialPressX by mutableStateOf(0f)
    var initialPressY by mutableStateOf(0f)

    // Previous state for paging
    var previousElapsedTime by mutableLongStateOf(0L)
        private set
    var previousIsDNF by mutableStateOf(false)
        private set
    var previousHasAddedTwoSeconds by mutableStateOf(false)
        private set
    var previousCompletedSolve by mutableStateOf<Solve?>(null)
        private set
    var previousShowControls by mutableStateOf(false)
        private set
    var previousScramble by mutableStateOf("")
        private set
    var previousSelectedCubeType by mutableStateOf(selectedCubeType)
        private set
    var previousSelectedTag by mutableStateOf(selectedTag)
        private set
    var previousCommentText by mutableStateOf("")
        private set

    // First launch flags
    var isFirstLaunch by mutableStateOf(true)
    var isFirstCubeTypeChange by mutableStateOf(true)

    init {
        // Generate initial scramble
        generateInitialScramble()
    }

    /**
     * Generates the initial scramble when the ViewModel is created.
     */
    private fun generateInitialScramble() {
        if (scramble.isEmpty()) {
            isScrambleLoading = true
            coroutineScope.launch {
                try {
                    val newScramble = withContext(Dispatchers.Default) {
                        generateScramble(CubeType.fromDisplayName(selectedCubeType))
                    }
                    scramble = newScramble
                } catch (e: Exception) {
                    // Generate a simple fallback scramble if the real one fails
                    scramble = "R U R' U'"
                } finally {
                    isScrambleLoading = false
                }
            }
        }
    }

    /**
     * Updates the selected cube type.
     */
    fun updateSelectedCubeType(cubeType: String) {
        selectedCubeType = cubeType
        AppState.selectedCubeType = cubeType

        if (isFirstCubeTypeChange) {
            isFirstCubeTypeChange = false
        } else if (!isRunning) {
            resetTimerState(resetCompletedSolve = true, generateNewScramble = true)

            // Reset all previous state variables to prevent bugs when paging after changing cube types
            resetPreviousState()
        } else {
            // Even if timer is running, update the scramble for the next solve
            resetPreviousState()

            generateNewScramble()
        }

    }

    /**
     * Updates the selected tag.
     */
    fun updateSelectedTag(tag: String) {
        selectedTag = tag
        AppState.selectedTag = tag

        // Reset timer state when tag changes
        if (isRunning) {
            isRunning = false
        }

        resetTimerState(resetCompletedSolve = true, generateNewScramble = true)

    }

    /**
     * Resets the previous state variables.
     */
    private fun resetPreviousState() {
        previousElapsedTime = 0L
        previousIsDNF = false
        previousHasAddedTwoSeconds = false
        previousCompletedSolve = null
        previousShowControls = false
        previousScramble = ""
        previousSelectedCubeType = selectedCubeType
        previousSelectedTag = selectedTag
        previousCommentText = ""
    }

    /**
     * Resets the timer state.
     */
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
            generateNewScramble()
        }
    }

    /**
     * Generates a new scramble.
     */
    fun generateNewScramble() {
        isScrambleLoading = true
        coroutineScope.launch {
            try {
                val newScramble = withContext(Dispatchers.Default) {
                    generateScramble(CubeType.fromDisplayName(selectedCubeType))
                }
                scramble = newScramble
            } catch (e: Exception) {
                // Generate a simple fallback scramble if the real one fails
                scramble = "R U R' U'"
            } finally {
                isScrambleLoading = false
            }
        }
    }

    /**
     * Generates a scramble for the specified cube type.
     */
    private suspend fun generateScramble(cubeType: CubeType): String {
        return withContext(Dispatchers.Default) {
            com.example.cubespeed.model.ScrambleGenerator.getInstance().generateScramble(cubeType)
        }
    }

    /**
     * Starts the timer.
     */
    fun startTimer() {
        // Capture the current cube type when the timer starts
        solveCubeType = selectedCubeType

        isRunning = true
        startTime = System.currentTimeMillis() - elapsedTime
    }

    /**
     * Stops the timer.
     */
    fun stopTimer() {
        isRunning = false
        showControls = true

        // Create a Solve object when the timer stops
        val solveStatus = if (isDNF) SolveStatus.DNF else if (hasAddedTwoSeconds) SolveStatus.PLUS2 else SolveStatus.OK

        // Only create a new solve if we don't already have one
        if (completedSolve == null) {
            val cubeTypeToUse = AppState.selectedCubeType
            val tagToUse = AppState.selectedTag
            val scrambleToUse = if (isPaging) previousScramble else scramble
            val commentsToUse = AppState.commentText

            val cubeType = CubeType.fromDisplayName(cubeTypeToUse)

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

            // Save the solve
            saveSolve(completedSolve!!)

            // Reset comment text for next solve
            AppState.commentText = ""
            commentText = ""
        }

        // Increment stats refresh trigger to update statistics
        statsRefreshTrigger += 1

        // Generate a new scramble for the next solve
        // Use the cube type from when the solve started, not the currently selected one
        generateNewScrambleAfterSolve()
    }

    /**
     * Generates a new scramble after a solve is completed.
     */
    private fun generateNewScrambleAfterSolve() {
        isScrambleLoading = true
        coroutineScope.launch {
            try {
                val newScramble = withContext(Dispatchers.Default) {
                    generateScramble(CubeType.fromDisplayName(solveCubeType))
                }
                scramble = newScramble
            } catch (e: Exception) {
                // Generate a simple fallback scramble if the real one fails
                scramble = "R U R' U'"
            } finally {
                isScrambleLoading = false
            }
        }
    }

    /**
     * Updates the timer.
     */
    fun updateTimer() {
        if (isRunning) {
            elapsedTime = System.currentTimeMillis() - startTime
        }
    }

    /**
     * Sets a cooldown to prevent spam clicking.
     */
    fun setCooldown(durationMillis: Long = 1000) {
        isInCooldown = true

        coroutineScope.launch {
            kotlinx.coroutines.delay(durationMillis)
            isInCooldown = false
        }
    }

    /**
     * Stores the previous state before pressing.
     */
    fun storePreviousState() {
        previousElapsedTime = elapsedTime
        previousIsDNF = isDNF
        previousHasAddedTwoSeconds = hasAddedTwoSeconds
        previousCompletedSolve = completedSolve
        previousShowControls = showControls
        previousScramble = scramble
        previousSelectedCubeType = selectedCubeType
        previousSelectedTag = selectedTag
        previousCommentText = commentText
    }

    /**
     * Restores the previous state.
     */
    fun restorePreviousState() {
        elapsedTime = previousElapsedTime
        isDNF = previousIsDNF
        hasAddedTwoSeconds = previousHasAddedTwoSeconds
        completedSolve = previousCompletedSolve
        showControls = previousShowControls
        scramble = previousScramble
        commentText = previousCommentText
    }

    /**
     * Resets the movement tracking flags.
     */
    fun resetMovementFlags() {
        isPaging = false
        hasMovedWhilePressed = false
        isScreenPressed = false
    }

    /**
     * Sets the initial press position.
     */
    fun setInitialPressPosition(x: Float, y: Float) {
        initialPressX = x
        initialPressY = y
    }

    /**
     * Checks if the movement exceeds the threshold for paging.
     */
    fun checkForPaging(x: Float, y: Float, horizontalPagingThreshold: Float): Boolean {
        val deltaX = kotlin.math.abs(x - initialPressX)
        val deltaY = kotlin.math.abs(y - initialPressY)

        // Check for horizontal movement (likely paging)
        return deltaX > horizontalPagingThreshold && deltaX > deltaY * 1.5f
    }

    /**
     * Marks the solve as DNF (Did Not Finish).
     */
    fun toggleDNF() {
        if (isDNF) {
            // If already DNF, restore original time
            isDNF = false

            // Update the completed solve status
            updateSolveStatus()
        } else {
            // Mark as DNF, save original time
            isDNF = true
            if (originalTime == 0L) {
                originalTime = elapsedTime
            }

            // Update the completed solve status
            updateSolveStatus()
        }
    }

    /**
     * Toggles the +2 seconds penalty.
     */
    fun togglePlusTwo() {
        if (!hasAddedTwoSeconds) {
            // Store original time before +2 if not already stored
            if (originalTime == 0L) {
                originalTime = elapsedTime
            }

            // Update UI to show +2 seconds
            elapsedTime += 2000 // Add 2 seconds (2000 ms) for UI display
            hasAddedTwoSeconds = true

            // Update the completed solve with original time and PLUS2 status
            updateSolveStatus()
        } else {
            // Remove the +2 seconds from UI display
            elapsedTime -= 2000 // Remove 2 seconds (2000 ms)
            hasAddedTwoSeconds = false

            // Update the completed solve with original time and OK status
            updateSolveStatus()
        }
    }

    /**
     * Updates the solve status.
     */
    private fun updateSolveStatus() {
        if (completedSolve != null) {
            // Determine the time to save
            // For all statuses, use the current elapsed time which already includes +2 seconds if PLUS2
            val timeToSave = elapsedTime

            // Check if we have a valid ID
            if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                // Use the ID from previousSolve if completedSolve doesn't have one
                completedSolve = completedSolve!!.copy(
                    id = previousSolve!!.id,
                    time = timeToSave,
                    status = if (isDNF) SolveStatus.DNF else if (hasAddedTwoSeconds) SolveStatus.PLUS2 else SolveStatus.OK
                )
            } else {
                // Use the existing ID
                completedSolve = completedSolve!!.copy(
                    time = timeToSave,
                    status = if (isDNF) SolveStatus.DNF else if (hasAddedTwoSeconds) SolveStatus.PLUS2 else SolveStatus.OK
                )
            }

            // Save the updated solve immediately
            if (completedSolve!!.id.isNotEmpty()) {
                saveSolve(completedSolve!!)

                // Increment stats refresh trigger to update statistics
                statsRefreshTrigger += 1
            }
        }
    }

    /**
     * Updates the comment for the current solve.
     */
    fun updateComment(comment: String) {
        commentText = comment

        if (completedSolve != null) {
            // Check if we have a valid ID
            if (completedSolve!!.id.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                // Use the ID from previousSolve if completedSolve doesn't have one
                completedSolve = completedSolve!!.copy(
                    id = previousSolve!!.id,
                    comments = comment
                )
            } else {
                // Use the existing ID
                completedSolve = completedSolve!!.copy(comments = comment)
            }

            // Save the updated solve immediately
            if (completedSolve!!.id.isNotEmpty()) {
                saveSolve(completedSolve!!)

                // Increment stats refresh trigger to update statistics
                statsRefreshTrigger += 1
            }
        }
    }

    /**
     * Deletes the current solve.
     */
    fun deleteSolve() {
        if (completedSolve != null) {
            // Check if we have a valid ID
            var solveIdToDelete = completedSolve!!.id

            if (solveIdToDelete.isEmpty() && previousSolve != null && previousSolve!!.id.isNotEmpty()) {
                // Use the ID from previousSolve if completedSolve doesn't have one
                solveIdToDelete = previousSolve!!.id
            }

            if (solveIdToDelete.isNotEmpty()) {
                coroutineScope.launch {
                    try {
                        val success = repository.deleteSolve(solveIdToDelete)

                        // Only update UI if delete was successful
                        if (success) {
                            // Increment stats refresh trigger to update statistics
                            statsRefreshTrigger += 1

                            // Update UI on the main thread
                            resetTimerState(resetCompletedSolve = true)
                        }
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            } else {
                // Still reset UI for empty ID
                resetTimerState(resetCompletedSolve = true)
            }
        } else {
            // Still reset UI for null completedSolve
            resetTimerState(resetCompletedSolve = true)
        }
    }

    /**
     * Saves a solve to the repository.
     */
    private fun saveSolve(solve: Solve) {
        coroutineScope.launch {
            if (solve.id.isEmpty()) {
                // This is a new solve, save it and update previousSolve with the ID
                val solveId = repository.saveSolve(solve)
                if (solveId.isNotEmpty()) {
                    // Update the solve with the ID from Firebase
                    val updatedSolve = solve.copy(id = solveId)
                    // Save the previous solve when a new one starts
                    previousSolve = updatedSolve
                    completedSolve = updatedSolve
                } else {
                    // If save failed, still keep the solve in memory
                    previousSolve = solve
                }
            } else {
                // This is an update to an existing solve, just save it
                repository.saveSolve(solve)
                // No need to update previousSolve as it already has the correct ID
            }

            // Increment stats refresh trigger to update statistics
            statsRefreshTrigger += 1

        }
    }
}

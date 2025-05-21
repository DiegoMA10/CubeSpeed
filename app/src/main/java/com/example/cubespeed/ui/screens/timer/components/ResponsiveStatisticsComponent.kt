package com.example.cubespeed.ui.screens.timer.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.repository.SolveStatistics
import com.example.cubespeed.ui.screens.history.ScrambleVisualization
import com.example.cubespeed.ui.screens.timer.utils.formatDouble
import com.example.cubespeed.ui.screens.timer.utils.formatTime
import com.example.cubespeed.ui.theme.StatisticsTextStyle
import com.example.cubespeed.ui.utils.ScreenUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

/**
 * A responsive composable that displays statistics for the current cube type and tag.
 * The layout adapts based on screen size and orientation.
 *
 * @param selectedCubeType The currently selected cube type
 * @param selectedTag The currently selected tag
 * @param refreshTrigger A trigger to refresh the statistics
 * @param scramble The current scramble
 * @param isLoading Whether the scramble is being loaded
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun ResponsiveStatisticsComponent(
    selectedCubeType: String,
    selectedTag: String,
    refreshTrigger: Long = 0,
    scramble: String = "",
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    // State for statistics
    var stats by remember { mutableStateOf<SolveStatistics>(SolveStatistics()) }

    // State for count from direct query
    var solveCount by remember { mutableStateOf(0) }

    // State for user ID that will be updated when auth state changes
    var userId by remember { mutableStateOf(Firebase.auth.currentUser?.uid) }

    // Repository for Firebase operations
    val repository = remember { FirebaseRepository() }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Check if we're on a tablet or in landscape mode
    val isTablet = ScreenUtils.isTablet()
    val isLandscape = ScreenUtils.isLandscape()

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

    // Effect to update the count when refreshTrigger changes
    LaunchedEffect(refreshTrigger, selectedCubeType, selectedTag) {
        val cubeType = CubeType.fromDisplayName(selectedCubeType)
        // Query the count directly from Firestore
        solveCount = repository.countSolvesByCubeTypeAndTag(cubeType, selectedTag)
    }

    // Effect to listen for changes to the stats document
    DisposableEffect(userId, selectedCubeType, selectedTag) {
        // Store userId in a local variable to avoid smart cast issues
        val currentUserId = userId

        // Only proceed if currentUserId is not null
        if (currentUserId != null) {
            val cubeType = CubeType.fromDisplayName(selectedCubeType)
            // Create the stats document ID using the same format as in FirebaseRepository and Cloud Function
            val statsDocId = "${cubeType.name}_${selectedTag}"
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val statsRef = db.collection("users")
                .document(currentUserId)
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
                        solveCount = repository.countSolvesByCubeTypeAndTag(cubeType, selectedTag)
                    }
                } else {
                    stats = SolveStatistics() // Empty if it doesn't exist
                }
            }

            // Return a function that removes the listener when disposed
            return@DisposableEffect onDispose { 
                listener.remove() // Very important
            }
        }

        // If userId is null, just return an empty onDispose function
        return@DisposableEffect onDispose { }
    }

    // Determine the appropriate layout based on device type and orientation
    if (!isTablet && isLandscape) {
        // For phones in landscape mode, use a special layout with 3 sections
        LandscapeMobileLayout(
            stats = stats,
            scramble = scramble,
            isLoading = isLoading,
            selectedCubeType = selectedCubeType,
            modifier = modifier
        )
    } else {
        // For tablets or phones in portrait mode, use the standard layout
        StandardLayout(
            stats = stats,
            scramble = scramble,
            isLoading = isLoading,
            selectedCubeType = selectedCubeType,
            modifier = modifier
        )
    }
}

/**
 * Standard layout for tablets or phones in portrait mode.
 * This is similar to the original StatisticsComponent layout.
 */
@Composable
private fun StandardLayout(
    stats: SolveStatistics,
    scramble: String,
    isLoading: Boolean,
    selectedCubeType: String,
    modifier: Modifier = Modifier
) {
    // Determine the appropriate size for the cube visualization
    val cubeSize = ScreenUtils.getResponsiveSize(
        tabletSize = 150.dp,
        phoneSize = 100.dp
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side statistics
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.Start
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
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                // Show loading indicator when scramble is loading
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else if (scramble.isNotEmpty()) {
                // Show scramble visualization when not loading and scramble is not empty
                ScrambleVisualization(
                    scramble = scramble,
                    cubeType = CubeType.fromDisplayName(selectedCubeType),
                    modifier = Modifier.size(cubeSize)
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

/**
 * Special layout for phones in landscape mode.
 * This splits the statistics into 3 separate components:
 * 1. Average component at bottom-left
 * 2. Ao stats component at bottom-right
 * 3. Cube SVG above the average component
 * This leaves space in the middle for the timer.
 */
@Composable
private fun LandscapeMobileLayout(
    stats: SolveStatistics,
    scramble: String,
    isLoading: Boolean,
    selectedCubeType: String,
    modifier: Modifier = Modifier
) {
    // Use a smaller cube size for landscape mode on phones
    val cubeSize = 90.dp

    // Use a Box as the root container to allow absolute positioning
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp) // Increased height to accommodate the 3 components
            .padding(horizontal = 8.dp)
    ) {
        // 1. Cube visualization to the right of the average component
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 120.dp), // Position it to the right of the average component
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                // Show loading indicator when scramble is loading
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else if (scramble.isNotEmpty()) {
                // Show scramble visualization when not loading and scramble is not empty
                ScrambleVisualization(
                    scramble = scramble,
                    cubeType = CubeType.fromDisplayName(selectedCubeType),
                    modifier = Modifier.size(cubeSize)
                )
            }
        }

        // 2. Average component at bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp, start = 8.dp)
                .width(120.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Avg: ${formatDouble(stats.average)}",
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
                text = "Dev: ${formatDouble(stats.deviation)}",
                style = StatisticsTextStyle,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // 3. Ao stats component at bottom-right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
                .width(120.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
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

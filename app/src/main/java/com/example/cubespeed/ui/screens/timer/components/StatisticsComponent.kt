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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

/**
 * A composable that displays statistics for the current cube type and tag.
 *
 * @param selectedCubeType The currently selected cube type
 * @param selectedTag The currently selected tag
 * @param refreshTrigger A trigger to refresh the statistics
 * @param scramble The current scramble
 * @param isLoading Whether the scramble is being loaded
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun StatisticsComponent(
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
                    solveCount = repository.countSolvesByCubeTypeAndTag(cubeType, selectedTag)
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
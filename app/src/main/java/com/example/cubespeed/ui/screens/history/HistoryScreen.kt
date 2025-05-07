package com.example.cubespeed.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.navigation.Route
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.components.CubeTopBar
import com.example.cubespeed.ui.screens.timer.CubeSelectionDialog
import com.example.cubespeed.ui.screens.timer.TagInputDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun HistoryScreen(navController: NavController? = null) {
    val repository = remember { FirebaseRepository() }
    var solves by remember { mutableStateOf<List<Solve>>(emptyList()) }

    // Use shared state for the selected cube type and tag
    var selectedCubeType by remember { mutableStateOf(AppState.selectedCubeType) }
    var selectedTag by remember { mutableStateOf(AppState.selectedTag) }

    // State for showing dialogs
    var showCubeSelectionDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // List of cube types
    val cubeTypes = CubeType.getAllDisplayNames()

    // Observe changes to AppState and update local state
    LaunchedEffect(AppState.selectedCubeType, AppState.selectedTag) {
        selectedCubeType = AppState.selectedCubeType
        selectedTag = AppState.selectedTag
    }

    // Fetch solves when the screen is first displayed or when filter changes
    LaunchedEffect(key1 = selectedCubeType, key2 = selectedTag) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            // Get solves directly from Firestore
            val firestore = Firebase.firestore
            val solvesRef = firestore.collection("users")
                .document(userId)
                .collection("timers")

            try {
                val snapshot = solvesRef.get().await()
                val fetchedSolves = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val cubeTypeStr = doc.getString("cube") ?: return@mapNotNull null
                    val cubeType = try { CubeType.valueOf(cubeTypeStr) } catch (e: Exception) { CubeType.CUBE_3X3 }
                    val tagId = doc.getString("tagId") ?: "normal"
                    val timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    val time = doc.getLong("duration") ?: 0L
                    val scramble = doc.getString("scramble") ?: ""
                    val statusStr = doc.getString("status") ?: SolveStatus.OK.name
                    val status = try { SolveStatus.valueOf(statusStr) } catch (e: Exception) { SolveStatus.OK }
                    val comments = doc.getString("comment") ?: ""

                    Solve(
                        id = id,
                        cube = cubeType,
                        tagId = tagId,
                        timestamp = timestamp,
                        time = time,
                        scramble = scramble,
                        status = status,
                        comments = comments
                    )
                }

                // Filter solves by selected cube type and tag
                val filteredSolves = fetchedSolves.filter { solve ->
                    (selectedCubeType == "All" || solve.cube.displayName == selectedCubeType) &&
                    (selectedTag == "All" || solve.tagId == selectedTag)
                }

                solves = filteredSolves.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                // Handle error
                solves = emptyList()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // CubeTopBar is now in MainTabsScreen

        // Display list of solves
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(solves) { solve ->
                SolveItem(solve = solve)
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

@Composable
fun SolveItem(solve: Solve) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Time: ${formatTime(solve.time)}")
            Text(text = "Cube: ${solve.cube.displayName}")
            Text(text = "Date: ${solve.timestamp.toDate()}")
            Text(text = "Status: ${solve.status}")
            if (solve.comments.isNotEmpty()) {
                Text(text = "Comments: ${solve.comments}")
            }
        }
    }
}

// Reuse the formatTime function from TimerScreen
fun formatTime(timeMillis: Long): String {
    val totalSeconds = timeMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = timeMillis % 1000

    return if (minutes > 0) {
        String.format("%d:%02d.%03d", minutes, seconds, millis)
    } else {
        String.format("%d.%03d", seconds, millis)
    }
}

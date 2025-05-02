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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.ui.screens.timer.CubeTopBar
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@Composable
fun HistoryScreen() {
    val repository = remember { FirebaseRepository() }
    var solves by remember { mutableStateOf<List<Solve>>(emptyList()) }

    // Fetch solves when the screen is first displayed
    LaunchedEffect(key1 = true) {
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

                solves = fetchedSolves.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                // Handle error
                solves = emptyList()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Reuse the CubeTopBar from TimerScreen
        CubeTopBar(
            title = "History",
            subtitle = "All solves",
            onSettingsClick = { /* Handle settings click */ },
            onOptionsClick = { /* Handle options click */ },
            onCubeClick = { /* Handle cube click */ }
        )

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

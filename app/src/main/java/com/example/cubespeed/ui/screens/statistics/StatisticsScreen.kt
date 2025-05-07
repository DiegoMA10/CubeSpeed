package com.example.cubespeed.ui.screens.statistics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.navigation.Route
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.repository.SolveStatistics
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.components.CubeTopBar
import com.example.cubespeed.ui.screens.timer.CubeSelectionDialog
import com.example.cubespeed.ui.screens.timer.TagInputDialog
import com.example.cubespeed.ui.theme.StatisticsTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StatisticsScreen(navController: NavController? = null) {
    val repository = remember { FirebaseRepository() }
    // Use shared state for the selected cube type and tag
    var selectedCubeType by remember { mutableStateOf(AppState.selectedCubeType) }
    var selectedTag by remember { mutableStateOf(AppState.selectedTag) }
    var statistics by remember { mutableStateOf<SolveStatistics>(SolveStatistics()) }

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

    // Fetch statistics when the screen is first displayed or when cube type/tag changes
    LaunchedEffect(key1 = selectedCubeType, key2 = selectedTag) {
        withContext(Dispatchers.IO) {
            val cubeType = CubeType.fromDisplayName(selectedCubeType)
            statistics = repository.getStats(cubeType, selectedTag)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // CubeTopBar is now in MainTabsScreen

        // Display statistics
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            StatisticsItem("Total Solves", statistics.count.toString())
            StatisticsItem("Valid Solves", statistics.validCount.toString())
            StatisticsItem("Best", formatTime(statistics.best))
            StatisticsItem("Average", formatDouble(statistics.average))
            StatisticsItem("Standard Deviation", formatDouble(statistics.deviation))
            StatisticsItem("Average of 5", formatDouble(statistics.ao5))
            StatisticsItem("Average of 12", formatDouble(statistics.ao12))
            StatisticsItem("Average of 50", formatDouble(statistics.ao50))
            StatisticsItem("Average of 100", formatDouble(statistics.ao100))
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
fun StatisticsItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = StatisticsTextStyle.copy(fontWeight = FontWeight.Bold))
        Text(text = value, style = StatisticsTextStyle)
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

// Reuse the formatDouble function from TimerScreen
fun formatDouble(value: Double): String {
    return if (value <= 0) {
        "-"
    } else {
        String.format("%.3f", value)
    }
}

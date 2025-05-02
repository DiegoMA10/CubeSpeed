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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.repository.SolveStatistics
import com.example.cubespeed.ui.screens.timer.CubeTopBar
import com.example.cubespeed.ui.theme.StatisticsTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatisticsScreen() {
    val repository = remember { FirebaseRepository() }
    var selectedCubeType by remember { mutableStateOf(CubeType.CUBE_3X3.displayName) }
    var selectedTag by remember { mutableStateOf("normal") }
    var statistics by remember { mutableStateOf<SolveStatistics>(SolveStatistics()) }

    // Fetch statistics when the screen is first displayed or when cube type/tag changes
    LaunchedEffect(key1 = selectedCubeType, key2 = selectedTag) {
        withContext(Dispatchers.IO) {
            val cubeType = CubeType.fromDisplayName(selectedCubeType)
            statistics = repository.getStats(cubeType, selectedTag)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Reuse the CubeTopBar from TimerScreen
        CubeTopBar(
            title = "Statistics",
            subtitle = "$selectedCubeType - $selectedTag",
            onSettingsClick = { /* Handle settings click */ },
            onOptionsClick = { /* Handle options click */ },
            onCubeClick = { /* Handle cube click */ }
        )

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

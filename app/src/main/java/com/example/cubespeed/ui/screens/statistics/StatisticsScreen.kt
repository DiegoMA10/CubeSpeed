package com.example.cubespeed.ui.screens.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.repository.SolveStatistics
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.screens.statistics.chart.TwistyTimerChart
import com.example.cubespeed.ui.screens.timer.CubeSelectionDialog
import com.example.cubespeed.ui.screens.timer.TagInputDialog
import com.example.cubespeed.ui.screens.timer.utils.formatTime
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main screen for displaying statistics and charts for solve times.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController? = null,
    viewModel: StatisticsViewModel = viewModel()
) {
    // State for showing dialogs
    var showCubeSelectionDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // List of cube types
    val cubeTypes = CubeType.getAllDisplayNames()

    // Observe changes to AppState and update ViewModel
    LaunchedEffect(AppState.selectedCubeType, AppState.selectedTag, AppState.historyRefreshTrigger) {
        if (viewModel.selectedCubeType != AppState.selectedCubeType) {
            viewModel.updateCubeType(AppState.selectedCubeType)
        }
        if (viewModel.selectedTag != AppState.selectedTag) {
            viewModel.updateTag(AppState.selectedTag)
        }
        // Refresh when historyRefreshTrigger changes (when solves are modified)
        if (AppState.historyRefreshTrigger > 0) {
            viewModel.loadStatistics()
        }
    }

    // Main content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Loading indicator or content
        Box(modifier = Modifier.fillMaxSize()) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // Content
                StatisticsContent(
                    statistics = viewModel.statistics,
                    recentSolves = viewModel.recentSolves,
                    calculateMovingAverages = viewModel::calculateMovingAverages
                )
            }
        }
    }

    // Cube Selection Dialog
    if (showCubeSelectionDialog) {
        CubeSelectionDialog(
            cubeTypes = cubeTypes,
            onCubeSelected = {
                viewModel.updateCubeType(it)
                AppState.selectedCubeType = it
                showCubeSelectionDialog = false
            },
            onDismiss = { showCubeSelectionDialog = false }
        )
    }

    // Tag Dialog
    if (showTagDialog) {
        TagInputDialog(
            currentTag = viewModel.selectedTag,
            onTagConfirmed = {
                viewModel.updateTag(it)
                AppState.selectedTag = it
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false }
        )
    }
}

/**
 * Filter bar for selecting cube type and tag.
 */
@Composable
fun FilterBar(
    selectedCubeType: String,
    selectedTag: String,
    onCubeTypeClick: () -> Unit,
    onTagClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Filter buttons
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cube type filter
            FilterChip(
                selected = false,
                onClick = onCubeTypeClick,
                label = { Text(selectedCubeType, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Select cube type",
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.padding(end = 8.dp)
            )

            // Tag filter
            FilterChip(
                selected = false,
                onClick = onTagClick,
                label = {
                    Text(
                        text = if (selectedTag.isEmpty()) "No tag" else selectedTag,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = "Select tag",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Main content of the statistics screen.
 */
@Composable
fun StatisticsContent(
    statistics: SolveStatistics,
    recentSolves: List<Solve>,
    calculateMovingAverages: (List<Solve>) -> Map<String, List<Double>>
) {
    // Calculate moving averages
    val movingAverages = calculateMovingAverages(recentSolves)

    // Get the ViewModel to access the Twisty Timer stats
    val viewModel: StatisticsViewModel = viewModel()

    // Use a fixed Column instead of LazyColumn to make the screen non-scrollable
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween // Push chart to top and stats to bottom
    ) {
        // Chart section - make it bigger
        Box(
            modifier = Modifier
                .weight(1f) // Take all available space
                .fillMaxWidth()
        ) {
            SolveTimeChart(recentSolves, movingAverages)
        }

        // Twisty Timer style statistics table at the bottom with margin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp) // Add margin between chart and stats
        ) {
            TwistyTimerStatsTable(viewModel.twistyTimerStats)
        }
    }
}

/**
 * Chart displaying solve times and moving averages.
 * Uses the TwistyTimerChart composable for rendering.
 */
@Composable
fun SolveTimeChart(
    solves: List<Solve>,
    movingAverages: Map<String, List<Double>>
) {
    // Render the chart using the TwistyTimerChart composable
    TwistyTimerChart(
        solves = solves,
        movingAverages = movingAverages,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight() // Fill all available height
    )
}

// Enum for tab selection
enum class MetricsTab {
    Progress, Average, Others
}

/**
 * Card displaying summary metrics with tabs.
 */
@Composable
fun SummaryMetrics(statistics: SolveStatistics) {
    // State for the selected tab
    var selectedTab by remember { mutableStateOf(MetricsTab.Progress) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Tab(
                    selected = selectedTab == MetricsTab.Progress,
                    onClick = { selectedTab = MetricsTab.Progress },
                    text = { Text("Progreso") }
                )
                Tab(
                    selected = selectedTab == MetricsTab.Average,
                    onClick = { selectedTab = MetricsTab.Average },
                    text = { Text("Promedio") }
                )
                Tab(
                    selected = selectedTab == MetricsTab.Others,
                    onClick = { selectedTab = MetricsTab.Others },
                    text = { Text("Otros") }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                MetricsTab.Progress -> ProgressMetricsTable(statistics)
                MetricsTab.Average -> AverageMetricsTable(statistics)
                MetricsTab.Others -> OtherMetricsTable(statistics)
            }
        }
    }
}

/**
 * Table for Progress metrics.
 */
@Composable
fun ProgressMetricsTable(statistics: SolveStatistics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header row
        MetricsTableHeader()

        // Data rows with alternating background colors
        MetricsTableRow(
            label = "Desviación",
            globalValue = formatDouble(statistics.deviation),
            sessionValue = formatDouble(statistics.deviation),
            isEvenRow = false
        )

        MetricsTableRow(
            label = "Ao12",
            globalValue = formatDouble(statistics.ao12),
            sessionValue = formatDouble(statistics.ao12),
            isEvenRow = true
        )

        MetricsTableRow(
            label = "Ao50",
            globalValue = if (statistics.ao50 > 0) formatDouble(statistics.ao50) else "--",
            sessionValue = if (statistics.ao50 > 0) formatDouble(statistics.ao50) else "--",
            isEvenRow = false
        )

        MetricsTableRow(
            label = "Ao100",
            globalValue = if (statistics.ao100 > 0) formatDouble(statistics.ao100) else "--",
            sessionValue = if (statistics.ao100 > 0) formatDouble(statistics.ao100) else "--",
            isEvenRow = true
        )

        MetricsTableRow(
            label = "Mejor",
            globalValue = formatTime(statistics.best),
            sessionValue = formatTime(statistics.best),
            isEvenRow = false
        )

        MetricsTableRow(
            label = "Cuenta",
            globalValue = statistics.count.toString(),
            sessionValue = statistics.count.toString(),
            isEvenRow = true
        )
    }
}

/**
 * Table for Average metrics.
 */
@Composable
fun AverageMetricsTable(statistics: SolveStatistics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header row
        MetricsTableHeader()

        // Data rows with alternating background colors
        MetricsTableRow(
            label = "Global",
            globalValue = formatDouble(statistics.average),
            sessionValue = formatDouble(statistics.average),
            isEvenRow = false
        )

        MetricsTableRow(
            label = "Ao5",
            globalValue = formatDouble(statistics.ao5),
            sessionValue = formatDouble(statistics.ao5),
            isEvenRow = true
        )

        MetricsTableRow(
            label = "Ao12",
            globalValue = formatDouble(statistics.ao12),
            sessionValue = formatDouble(statistics.ao12),
            isEvenRow = false
        )

        MetricsTableRow(
            label = "Ao50",
            globalValue = if (statistics.ao50 > 0) formatDouble(statistics.ao50) else "--",
            sessionValue = if (statistics.ao50 > 0) formatDouble(statistics.ao50) else "--",
            isEvenRow = true
        )

        MetricsTableRow(
            label = "Ao100",
            globalValue = if (statistics.ao100 > 0) formatDouble(statistics.ao100) else "--",
            sessionValue = if (statistics.ao100 > 0) formatDouble(statistics.ao100) else "--",
            isEvenRow = false
        )
    }
}

/**
 * Table for Other metrics.
 */
@Composable
fun OtherMetricsTable(statistics: SolveStatistics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header row
        MetricsTableHeader()

        // Data rows with alternating background colors
        MetricsTableRow(
            label = "Total",
            globalValue = statistics.count.toString(),
            sessionValue = statistics.count.toString(),
            isEvenRow = false
        )

        MetricsTableRow(
            label = "Válidos",
            globalValue = statistics.validCount.toString(),
            sessionValue = statistics.validCount.toString(),
            isEvenRow = true
        )

        MetricsTableRow(
            label = "Mejor",
            globalValue = formatTime(statistics.best),
            sessionValue = formatTime(statistics.best),
            isEvenRow = false
        )

        MetricsTableRow(
            label = "Desviación",
            globalValue = formatDouble(statistics.deviation),
            sessionValue = formatDouble(statistics.deviation),
            isEvenRow = true
        )
    }
}

/**
 * Header row for metrics tables.
 */
@Composable
fun MetricsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Σ",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            text = "Global",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Sesión",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Row for metrics tables.
 */
@Composable
fun MetricsTableRow(
    label: String,
    globalValue: String,
    sessionValue: String,
    isEvenRow: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isEvenRow) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            text = globalValue,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = sessionValue,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Card displaying a single metric.
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1.5f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Card displaying a session.
 */
@Composable
fun SessionCard(
    date: String,
    solves: List<Solve>
) {
    // Calculate session metrics
    val validSolves = solves.filter { it.status != SolveStatus.DNF }
    val sessionAverage = if (validSolves.isNotEmpty()) {
        validSolves.sumOf {
            if (it.status == SolveStatus.PLUS2) it.time + 2000 else it.time
        }.toDouble() / validSolves.size
    } else 0.0

    val bestTime = validSolves.minOfOrNull {
        if (it.status == SolveStatus.PLUS2) it.time + 2000 else it.time
    } ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date and solve count
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDateForDisplay(date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${solves.size} solves",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Session metrics
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Avg: ${formatDouble(sessionAverage)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Best: ${formatTime(bestTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Format time to a readable string
fun formatTime(timeMillis: Long): String {
    if (timeMillis <= 0) return "-"

    val hours = (timeMillis / 3600000).toInt()
    val minutes = ((timeMillis % 3600000) / 60000).toInt()
    val seconds = ((timeMillis % 60000) / 1000).toInt()
    val millis = ((timeMillis % 1000) / 10).toInt()  // Only use the first 2 digits of milliseconds

    return when {
        hours > 0 -> String.format("%d h %d:%02d", hours, minutes, seconds)  // No milliseconds when hours are present
        minutes > 0 -> String.format("%d:%02d.%02d", minutes, seconds, millis)
        else -> String.format("%d.%02d", seconds, millis)
    }
}

// Reuse the formatDouble function from TimerScreen
fun formatDouble(value: Double): String {
    return when {
        value == 0.0 -> "0.00"
        value == -1.0 -> "DNF"
        else -> formatTime(value.toLong())
    }
}

// Format date for display
fun formatDateForDisplay(dateString: String): String {
    try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        return date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        return dateString
    }
}

/**
 * Twisty Timer style statistics table.
 * Displays all the statistics in a clear table format.
 */
@Composable
fun TwistyTimerStatsTable(stats: TwistyTimerStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Tab row for different stat categories
            var selectedTab by remember { mutableStateOf(0) }
            val tabTitles = listOf("Times", "Averages", "Counts")

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> TimesTab(stats)
                1 -> AveragesTab(stats)
                2 -> CountsTab(stats)
            }
        }
    }
}

/**
 * Tab for displaying time statistics.
 */
@Composable
fun TimesTab(stats: TwistyTimerStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Header
        StatsTableHeader()

        // Best time
        StatsTableRow(
            label = "Best time",
            value = formatTime(stats.bestTime),
            isEvenRow = false
        )

        // Worst time
        StatsTableRow(
            label = "Worst time",
            value = formatTime(stats.worstTime),
            isEvenRow = true
        )

        // Global average
        StatsTableRow(
            label = "Global average",
            value = formatDouble(stats.globalAverage),
            isEvenRow = false
        )

        // Standard deviation
        StatsTableRow(
            label = "Standard deviation",
            value = formatDouble(stats.standardDeviation),
            isEvenRow = true
        )
    }
}

/**
 * Tab for displaying average statistics.
 */
@Composable
fun AveragesTab(stats: TwistyTimerStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Header with columns for best and latest
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Average",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Best",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Latest",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ao5
        AverageRow(
            label = "Ao5",
            bestValue = stats.bestAo5,
            latestValue = stats.latestAo5,
            isEvenRow = false
        )

        // Ao12
        AverageRow(
            label = "Ao12",
            bestValue = stats.bestAo12,
            latestValue = stats.latestAo12,
            isEvenRow = true
        )

        // Ao50
        AverageRow(
            label = "Ao50",
            bestValue = stats.bestAo50,
            latestValue = stats.latestAo50,
            isEvenRow = false
        )

        // Ao100
        AverageRow(
            label = "Ao100",
            bestValue = stats.bestAo100,
            latestValue = stats.latestAo100,
            isEvenRow = true
        )
    }
}

/**
 * Tab for displaying count statistics.
 */
@Composable
fun CountsTab(stats: TwistyTimerStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Header
        StatsTableHeader()

        // Total solves
        StatsTableRow(
            label = "Total solves",
            value = stats.totalSolves.toString(),
            isEvenRow = false
        )

        // Valid solves
        StatsTableRow(
            label = "Valid solves",
            value = stats.validSolves.toString(),
            isEvenRow = true
        )

        // DNF count
        StatsTableRow(
            label = "DNF count",
            value = stats.dnfCount.toString(),
            isEvenRow = false
        )

        // +2 count
        StatsTableRow(
            label = "+2 count",
            value = stats.plus2Count.toString(),
            isEvenRow = true
        )
    }
}

/**
 * Header for stats tables.
 */
@Composable
fun StatsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Statistic",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Value",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Row for stats tables.
 */
@Composable
fun StatsTableRow(
    label: String,
    value: String,
    isEvenRow: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isEvenRow) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Row for average statistics with best and latest values.
 */
@Composable
fun AverageRow(
    label: String,
    bestValue: Double,
    latestValue: Double,
    isEvenRow: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isEvenRow) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatDouble(bestValue),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = formatDouble(latestValue),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

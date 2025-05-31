package com.example.cubespeed.ui.screens.statistics.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.model.Statistics
import com.example.cubespeed.model.SolveStatistics
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * ViewModel for the Statistics screen.
 * Handles data fetching and processing for statistics display.
 */
class StatisticsViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // UI state
    var isLoading by mutableStateOf(true)
        private set

    var statistics by mutableStateOf(SolveStatistics())
        private set

    var stadistics by mutableStateOf(Statistics())
        private set

    var recentSolves by mutableStateOf<List<Solve>>(emptyList())
        private set

    var selectedCubeType by mutableStateOf("")
        private set

    var selectedTag by mutableStateOf("")
        private set

    // Listener registration for real-time updates
    private var solvesListener: ListenerRegistration? = null

    // Initialize with default values
    init {
        // Get the selected cube type and tag from AppState
        // We'll set a default value first, then update it in loadStatistics
        // This ensures we get the correct value after AppState is initialized from SharedPreferences
        selectedCubeType = AppState.selectedCubeType
        selectedTag = AppState.selectedTag
        loadStatistics()
    }

    /**
     * Updates the selected cube type and reloads statistics.
     */
    fun updateCubeType(cubeType: String) {
        Log.d("StatisticsViewModel", "Updating cube type from $selectedCubeType to $cubeType")
        selectedCubeType = cubeType
        loadStatistics()
    }

    /**
     * Updates the selected tag and reloads statistics.
     */
    fun updateTag(tag: String) {
        Log.d("StatisticsViewModel", "Updating tag from $selectedTag to $tag")
        selectedTag = tag
        loadStatistics()
    }

    /**
     * Loads statistics and recent solves for the selected cube type and tag.
     */
    fun loadStatistics() {
        isLoading = true

        // Update selectedCubeType and selectedTag from AppState
        // This ensures we get the correct values after AppState is initialized from SharedPreferences
        if (selectedCubeType != AppState.selectedCubeType) {
            Log.d("StatisticsViewModel", "Updating cube type from $selectedCubeType to ${AppState.selectedCubeType}")
            selectedCubeType = AppState.selectedCubeType
        }

        if (selectedTag != AppState.selectedTag) {
            Log.d("StatisticsViewModel", "Updating tag from $selectedTag to ${AppState.selectedTag}")
            selectedTag = AppState.selectedTag
        }

        Log.d("StatisticsViewModel", "Loading statistics for cube type: '$selectedCubeType', tag: '$selectedTag'")

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Log all available cube types for debugging
                    Log.d(
                        "StatisticsViewModel",
                        "Available cube types: ${CubeType.values().joinToString { "${it.name} (${it.displayName})" }}"
                    )

                    val cubeType = CubeType.fromDisplayName(AppState.selectedCubeType)
                    Log.d("StatisticsViewModel", "Converted cube type: ${cubeType.name} (${cubeType.displayName})")

                    // Load statistics
                    statistics = repository.getStats(cubeType, AppState.selectedTag)
                    Log.d(
                        "StatisticsViewModel",
                        "Loaded statistics: count=${statistics.count}, best=${statistics.best}, ao5=${statistics.ao5}"
                    )

                    // Load recent solves for the chart
                    loadRecentSolves(cubeType, selectedTag)
                }
            } catch (e: Exception) {
                // Handle error
                Log.e("StatisticsViewModel", "Error loading statistics", e)
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Loads recent solves for the selected cube type and tag.
     */
    private suspend fun loadRecentSolves(cubeType: CubeType, tagId: String) {
        withContext(Dispatchers.IO) {
            // Remove any existing listener
            solvesListener?.remove()

            // Log the cube type and tag being used for debugging
            Log.d(
                "StatisticsViewModel",
                "Loading solves for cube type: '${cubeType.name}' (display name: '${cubeType.displayName}'), tag: '$tagId'"
            )

            // Create a new listener for real-time updates
            solvesListener = repository.listenForSolves(
                onSolvesUpdate = { solves, _, _ ->
                    // Log the number of solves received
                    Log.d(
                        "StatisticsViewModel",
                        "Received ${solves.size} solves for '${cubeType.name}' (display name: '${cubeType.displayName}'), tag: '$tagId'"
                    )

                    // Log the cube types of the received solves
                    val cubeTypeCounts = solves.groupBy { it.cube.name }.mapValues { it.value.size }
                    Log.d("StatisticsViewModel", "Cube type distribution in received solves: $cubeTypeCounts")

                    // Sort by timestamp for chronological order
                    val solvesList = solves.sortedBy { it.timestamp }

                    // If we have more than 100 solves, take only the most recent 100
                    val limitedSolves = if (solvesList.size > 100) {
                        solvesList.takeLast(100)
                    } else {
                        solvesList
                    }

                    // Log the cube types of the limited solves
                    val limitedCubeTypeCounts = limitedSolves.groupBy { it.cube.name }.mapValues { it.value.size }
                    Log.d("StatisticsViewModel", "Cube type distribution in limited solves: $limitedCubeTypeCounts")

                    // Update the state
                    recentSolves = limitedSolves

                    // Calculate CubeSpeed style statistics
                    calculateCubeSpeedStats(limitedSolves)

                    // Set loading to false after data is loaded
                    isLoading = false
                },
                pageSize = 100, // Limit to 100 solves as per requirement
                selectedCubeType = cubeType.displayName,
                selectedTag = tagId
            )
        }
    }

    /**
     * Calculates CubeSpeed style statistics from a list of solves.
     */
    private fun calculateCubeSpeedStats(solves: List<Solve>) {
        if (solves.isEmpty()) {
            stadistics = Statistics()
            return
        }

        // Counts
        val totalSolves = solves.size
        val dnfCount = countSolvesByStatus(solves, SolveStatus.DNF)
        val plus2Count = countSolvesByStatus(solves, SolveStatus.PLUS2)
        val validSolves = totalSolves - dnfCount

        // Times
        val bestTime = getBestTime(solves)
        val worstTime = getWorstTime(solves)

        // Averages
        val globalAverage = getGlobalAverage(solves)

        // Best averages
        val bestAo5 = getBestAoN(solves, 5)
        val bestAo12 = getBestAoN(solves, 12)
        val bestAo50 = getBestAoN(solves, 50)
        val bestAo100 = getBestAoN(solves, 100)

        // Latest averages
        val latestAo5 = getLatestAoN(solves, 5)
        val latestAo12 = getLatestAoN(solves, 12)
        val latestAo50 = getLatestAoN(solves, 50)
        val latestAo100 = getLatestAoN(solves, 100)

        // Standard deviation
        val standardDeviation = calculateStandardDeviation(solves)

        // Update the state
        stadistics = Statistics(
            totalSolves = totalSolves,
            validSolves = validSolves,
            dnfCount = dnfCount,
            plus2Count = plus2Count,
            bestTime = bestTime,
            worstTime = worstTime,
            globalAverage = globalAverage,
            bestAo5 = bestAo5,
            bestAo12 = bestAo12,
            bestAo50 = bestAo50,
            bestAo100 = bestAo100,
            latestAo5 = latestAo5,
            latestAo12 = latestAo12,
            latestAo50 = latestAo50,
            latestAo100 = latestAo100,
            standardDeviation = standardDeviation
        )
    }

    /**
     * Calculates the standard deviation of solve times.
     * Ignores DNF solves.
     */
    private fun calculateStandardDeviation(solves: List<Solve>): Double {
        val validSolves = solves.filter { it.status != SolveStatus.DNF }
        if (validSolves.size <= 1) return 0.0

        // Calculate the mean
        val times = validSolves.map {
            if (it.status == SolveStatus.PLUS2) it.time + 2000.0 else it.time.toDouble()
        }
        val mean = times.average()

        // Calculate the sum of squared differences
        val sumSquaredDiff = times.sumOf { (it - mean) * (it - mean) }

        // Calculate the standard deviation
        return Math.sqrt(sumSquaredDiff / times.size)
    }

    /**
     * Calculates moving averages for the given solves.
     * Returns a map of average type to list of average values.
     */
    fun calculateMovingAverages(solves: List<Solve>): Map<String, List<Double>> {
        if (solves.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, List<Double>>()

        // Ao5
        if (solves.size >= 5) {
            val ao5List = mutableListOf<Double>()
            for (i in 4 until solves.size) {
                val window = solves.subList(i - 4, i + 1)
                ao5List.add(calculateAverageOfN(window, 5))
            }
            result["ao5"] = ao5List
        }

        // Ao12
        if (solves.size >= 12) {
            val ao12List = mutableListOf<Double>()
            for (i in 11 until solves.size) {
                val window = solves.subList(i - 11, i + 1)
                ao12List.add(calculateAverageOfN(window, 12))
            }
            result["ao12"] = ao12List
        }

        // Ao50
        if (solves.size >= 50) {
            val ao50List = mutableListOf<Double>()
            for (i in 49 until solves.size) {
                val window = solves.subList(i - 49, i + 1)
                ao50List.add(calculateAverageOfN(window, 50))
            }
            result["ao50"] = ao50List
        }

        // Ao100
        if (solves.size >= 100) {
            val ao100List = mutableListOf<Double>()
            for (i in 99 until solves.size) {
                val window = solves.subList(i - 99, i + 1)
                ao100List.add(calculateAverageOfN(window, 100))
            }
            result["ao100"] = ao100List
        }

        return result
    }

    /**
     * Gets the best time from a list of solves.
     * Ignores DNF solves.
     */
    fun getBestTime(solves: List<Solve>): Long {
        return solves.filter { it.status != SolveStatus.DNF }
            .minByOrNull { if (it.status == SolveStatus.PLUS2) it.time + 2000 else it.time }
            ?.let { if (it.status == SolveStatus.PLUS2) it.time + 2000 else it.time } ?: 0
    }

    /**
     * Gets the worst time from a list of solves.
     * Ignores DNF solves.
     */
    fun getWorstTime(solves: List<Solve>): Long {
        return solves.filter { it.status != SolveStatus.DNF }
            .maxByOrNull { if (it.status == SolveStatus.PLUS2) it.time + 2000 else it.time }
            ?.let { if (it.status == SolveStatus.PLUS2) it.time + 2000 else it.time } ?: 0
    }

    /**
     * Calculates the global average of all solves.
     * Returns -1.0 if all solves are DNF.
     */
    fun getGlobalAverage(solves: List<Solve>): Double {
        val validSolves = solves.filter { it.status != SolveStatus.DNF }
        if (validSolves.isEmpty()) return -1.0

        return validSolves.map {
            if (it.status == SolveStatus.PLUS2) it.time + 2000.0 else it.time.toDouble()
        }.average()
    }

    /**
     * Gets the best average of N (AoN) from a list of solves.
     * Returns -1.0 if no valid average exists.
     */
    fun getBestAoN(solves: List<Solve>, n: Int): Double {
        if (solves.size < n) return 0.0
        var best = Double.MAX_VALUE
        for (i in 0..solves.size - n) {
            val window = solves.subList(i, i + n)
            val avg = calculateAverageOfN(window, n)
            if (avg >= 0 && avg < best) best = avg
        }
        return if (best == Double.MAX_VALUE) 0.0 else best
    }

    fun getLatestAoN(solves: List<Solve>, n: Int): Double {
        return calculateAverageOfN(solves, n)
    }

    /**
     * Counts the number of solves with a specific status.
     */
    fun countSolvesByStatus(solves: List<Solve>, status: SolveStatus): Int {
        return solves.count { it.status == status }
    }

    /**
     * Calculates the average time for a list of solves.
     * Handles PLUS2 penalties by adding 2 seconds.
     * Returns -1.0 if any solve is DNF (CubeSpeed style).
     */
    private fun calculateAverageOfN(solves: List<Solve>, n: Int): Double {
        if (solves.size < n) return 0.0

        // Tomamos las N últimas resoluciones
        val recent = solves.takeLast(n)
        // Extraemos pares (tiempo, isDnf)
        val pairs = recent.map { solve ->
            val time = solve.time.toDouble()      // ya incluye el +2 si aplica
            val isDnf = (solve.status == SolveStatus.DNF)
            time to isDnf
        }

        // Contamos DNFs
        val dnfCount = pairs.count { it.second }

        // Determinamos maxAllowedDNFs y cuántas entradas recortar
        val (maxAllowedDnf, trimCount) = when {
            n <= 12 -> 1 to 1   // Ao5 y Ao12
            n <= 50 -> 2 to 2   // Ao50
            else -> 5 to 5   // Ao100
        }

        // Si hay demasiados DNFs, devolvemos DNF
        if (dnfCount > maxAllowedDnf) return -1.0

        // Ordenamos: los DNFs van al final (peores)
        val sorted = pairs.sortedWith(compareBy<Pair<Double, Boolean>> { it.second }
            .thenBy { it.first }
        )

        // Recortamos mejores y peores
        val trimmed = sorted.subList(trimCount, sorted.size - trimCount)

        // Si en el recortado queda algún DNF → DNF
        if (trimmed.any { it.second }) return -1.0

        // Devolvemos la media simple de los tiempos
        return trimmed.map { it.first }.average()
    }

    /**
     * Cleans up resources when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        // Remove the listener to avoid memory leaks
        solvesListener?.remove()
    }

    /**
     * Generates sample data for preview purposes.
     */
    companion object {
        fun getSampleData(): List<Solve> {
            val now = Date()
            val solves = mutableListOf<Solve>()

            // Generate 100 sample solves with times between 10 and 30 seconds
            for (i in 0 until 100) {
                val time = (10000 + Math.random() * 20000).toLong() // 10-30 seconds
                val date = Date(now.time - (100 - i) * 24 * 60 * 60 * 1000) // One day apart

                solves.add(
                    Solve(
                        id = "sample-$i",
                        cube = CubeType.CUBE_3X3,
                        tagId = "",
                        timestamp = Timestamp(date),
                        time = time,
                        scramble = "R U R' U'",
                        status = SolveStatus.OK,
                        comments = ""
                    )
                )
            }

            return solves
        }
    }
}

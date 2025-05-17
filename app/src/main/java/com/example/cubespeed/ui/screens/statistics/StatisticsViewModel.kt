package com.example.cubespeed.ui.screens.statistics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.repository.SolveStatistics
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Data class to hold all the statistics for the Twisty Timer style statistics panel.
 */
data class TwistyTimerStats(
    // Counts
    val totalSolves: Int = 0,
    val validSolves: Int = 0,
    val dnfCount: Int = 0,
    val plus2Count: Int = 0,

    // Times
    val bestTime: Long = 0,
    val worstTime: Long = 0,

    // Averages
    val globalAverage: Double = -1.0,

    // Best averages
    val bestAo5: Double = -1.0,
    val bestAo12: Double = -1.0,
    val bestAo50: Double = -1.0,
    val bestAo100: Double = -1.0,

    // Latest averages
    val latestAo5: Double = -1.0,
    val latestAo12: Double = -1.0,
    val latestAo50: Double = -1.0,
    val latestAo100: Double = -1.0,

    // Standard deviation
    val standardDeviation: Double = 0.0
)

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

    var twistyTimerStats by mutableStateOf(TwistyTimerStats())
        private set

    var recentSolves by mutableStateOf<List<Solve>>(emptyList())
        private set

    var selectedCubeType by mutableStateOf("")
        private set

    var selectedTag by mutableStateOf("")
        private set

    // Listener registration for real-time updates
    private var solvesListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Initialize with default values
    init {
        selectedCubeType = "3x3 Cube" // Default to 3x3
        selectedTag = ""  // Default to no tag
        loadStatistics()
    }

    /**
     * Updates the selected cube type and reloads statistics.
     */
    fun updateCubeType(cubeType: String) {
        selectedCubeType = cubeType
        loadStatistics()
    }

    /**
     * Updates the selected tag and reloads statistics.
     */
    fun updateTag(tag: String) {
        selectedTag = tag
        loadStatistics()
    }

    /**
     * Loads statistics and recent solves for the selected cube type and tag.
     */
    fun loadStatistics() {
        isLoading = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val cubeType = CubeType.fromDisplayName(selectedCubeType)

                    // Load statistics
                    statistics = repository.getStats(cubeType, selectedTag)

                    // Load recent solves for the chart
                    loadRecentSolves(cubeType, selectedTag)
                }
            } catch (e: Exception) {
                // Handle error
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

            // Create a new listener for real-time updates
            solvesListener = repository.listenForSolves(
                onSolvesUpdate = { solves, _, _ ->
                    // Sort by timestamp for chronological order
                    val solvesList = solves.sortedBy { it.timestamp }

                    // If we have more than 100 solves, take only the most recent 100
                    val limitedSolves = if (solvesList.size > 100) {
                        solvesList.takeLast(100)
                    } else {
                        solvesList
                    }

                    // Update the state
                    recentSolves = limitedSolves

                    // Calculate Twisty Timer style statistics
                    calculateTwistyTimerStats(limitedSolves)

                    // Set loading to false after data is loaded
                    isLoading = false
                },
                pageSize = 100,
                selectedCubeType = cubeType.displayName,
                selectedTag = tagId
            )
        }
    }

    /**
     * Calculates Twisty Timer style statistics from a list of solves.
     */
    private fun calculateTwistyTimerStats(solves: List<Solve>) {
        if (solves.isEmpty()) {
            twistyTimerStats = TwistyTimerStats()
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
        twistyTimerStats = TwistyTimerStats(
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
     * Returns -1.0 if any solve is DNF (Twisty Timer style).
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

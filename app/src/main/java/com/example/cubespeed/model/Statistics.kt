package com.example.cubespeed.model

/**
 * Data class to hold all the statistics for the CubeSpeed style statistics panel.
 */
data class Statistics(
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
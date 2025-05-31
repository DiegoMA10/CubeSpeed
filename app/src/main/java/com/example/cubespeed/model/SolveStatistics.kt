package com.example.cubespeed.model

/**
 * Data class to hold statistics for a specific cube type and tag
 */
data class SolveStatistics(
    val count: Int = 0,
    val validCount: Int = 0,
    val best: Long = 0,
    val average: Double = 0.0,
    val deviation: Double = 0.0,
    val ao5: Double = 0.0,
    val ao12: Double = 0.0,
    val ao50: Double = 0.0,
    val ao100: Double = 0.0
)
package com.example.cubespeed.ui.screens.timer.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Function to calculate font size based on text length and screen orientation
 *
 * @param text The text to calculate the font size for
 * @return The calculated font size
 */
@Composable
fun calculateFontSize(text: String): TextUnit {
    // Get the current screen configuration
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // Check if we're in landscape mode (width > height)
    val isLandscape = screenWidth > screenHeight

    // Base size depends on orientation
    val baseSize = if (isLandscape) {
        // In landscape, use a smaller base size to ensure visibility
        120.sp
    } else {
        // In portrait, use the original base size
        120.sp
    }

    // Adjust size based on text length
    return when {
        text.length <= 3 -> baseSize
        text.length <= 5 -> baseSize.times(0.85f)
        text.length <= 7 -> baseSize.times(0.7f)
        text.length <= 9 -> baseSize.times(0.6f)
        text.length <= 12 -> baseSize.times(0.5f)
        else -> baseSize.times(0.4f)
    }
}

/**
 * Formats a time in milliseconds to a human-readable string.
 *
 * @param timeMillis The time in milliseconds
 * @return The formatted time string
 */
fun formatTime(timeMillis: Long): String {
    val hours = (timeMillis / 3600000).toInt()
    val minutes = ((timeMillis % 3600000) / 60000).toInt()
    val seconds = ((timeMillis % 60000) / 1000).toInt()
    val millis = ((timeMillis % 1000) / 10).toInt()

    return when {
        hours > 0 -> String.format("%d h %d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%d:%02d.%02d", minutes, seconds, millis)
        else -> String.format("%d.%02d", seconds, millis)
    }
}

/**
 * Formats a double value to a string with two decimal places.
 *
 * @param value The double value to format
 * @return The formatted string
 */
fun formatDouble(value: Double): String {
    return when {
        value == 0.0 -> "--"
        value == -1.0 -> "DNF"
        else -> formatTime(value.toLong())
    }
}

/**
 * Gets the effective time for a solve, taking into account any penalties.
 * Since the time saved to Firestore already includes the +2 penalty,
 * we don't need to add it again when displaying.
 * If the status is DNF, returns the original time (DNF is handled separately in the UI).
 *
 * @param time The time in milliseconds (already includes +2 if status is PLUS2)
 * @param status The status of the solve (OK, DNF, PLUS2)
 * @return The effective time in milliseconds
 */
fun getEffectiveTime(time: Long, status: com.example.cubespeed.model.SolveStatus): Long {
    // Time already includes +2 penalty if status is PLUS2, so just return it
    return time
}

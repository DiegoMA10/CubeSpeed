package com.example.cubespeed.ui.screens.timer.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cubespeed.ui.screens.timer.utils.calculateFontSize
import com.example.cubespeed.ui.screens.timer.utils.formatTime

/**
 * A composable that displays the timer value.
 *
 * @param elapsedTime The elapsed time in milliseconds
 * @param timerColor The color of the timer text
 * @param timerScale The scale factor for the timer text
 * @param hasAddedTwoSeconds Whether a +2 seconds penalty has been added
 * @param isDNF Whether the solve is marked as DNF (Did Not Finish)
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun TimerDisplayComponent(
    elapsedTime: Long,
    timerColor: Color,
    timerScale: Float,
    hasAddedTwoSeconds: Boolean,
    isDNF: Boolean,
    modifier: Modifier = Modifier
) {
    // Check if we should display DNF or the time
    if (isDNF) {
        // Format the time string to calculate the font size
        val timeString = formatTime(elapsedTime)
        val parts = timeString.split(".")
        val mainPart = parts[0]
        val millisPart = if (parts.size > 1) ".${parts[1]}" else ""
        val fullTimeString = mainPart + millisPart + if (hasAddedTwoSeconds) "+" else ""

        // Display DNF but use the timer's font size
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = timerScale
                    scaleY = timerScale
                }
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DNF",
                fontSize = calculateFontSize(fullTimeString),
                fontWeight = FontWeight.Bold,
                color = timerColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    } else {
        // Split time into main part and milliseconds
        val timeString = formatTime(elapsedTime)
        val parts = timeString.split(".")
        val mainPart = parts[0]
        val millisPart = if (parts.size > 1) ".${parts[1]}" else ""

        // Wrap both text components in a Box with the graphicsLayer modifier for proper scaling
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = timerScale
                    scaleY = timerScale
                }
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Calculate font size based on the full time string
                val fullTimeString = mainPart + millisPart + if (hasAddedTwoSeconds) "+" else ""
                val calculatedSize = calculateFontSize(fullTimeString)

                // Main part of the time (hours/minutes/seconds)
                Text(
                    text = mainPart,
                    style = TextStyle(
                        fontSize = calculatedSize,
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                        baselineShift = BaselineShift.None
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

                // Milliseconds part with smaller font
                Text(
                    text = millisPart + if (hasAddedTwoSeconds) "+" else "",
                    style = TextStyle(
                        fontSize = calculatedSize.times(0.75f), // Smaller font for milliseconds
                        fontWeight = FontWeight.Bold,
                        color = timerColor,
                        baselineShift = BaselineShift(0.082F)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

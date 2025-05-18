package com.example.cubespeed.ui.screens.statistics.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cubespeed.model.Solve
import com.example.cubespeed.ui.screens.statistics.formatTime
import com.example.cubespeed.ui.screens.timer.utils.getEffectiveTime
import com.example.cubespeed.ui.theme.*
import com.example.cubespeed.ui.theme.LocalThemePreference
import com.example.cubespeed.ui.theme.AppThemeType

/**
 * A composable that displays a line chart in the style of Twisty Timer.
 * Shows solve times and moving averages without any interactive gestures.
 *
 * @param solves The list of solves to display
 * @param movingAverages Map of moving average type to list of values
 * @param modifier The modifier to apply to the chart
 */
@Composable
fun TwistyTimerChart(
    solves: List<Solve>,
    movingAverages: Map<String, List<Double>>,
    modifier: Modifier = Modifier
) {
    // Define colors for the chart using theme color definitions
    val backgroundColor = MaterialTheme.colorScheme.primary // Primary color background
    val solveLineColor = when (LocalThemePreference.current) {
        AppThemeType.BLUE -> Color.White
        AppThemeType.LIGHT -> ChartSolveLineColorLight
        else -> ChartSolveLineColorDark
    } // Solve line color
    val ao5LineColor = ChartAo5LineColor // Ao5 line color
    val ao12LineColor = when (LocalThemePreference.current) {
        AppThemeType.LIGHT -> ChartAo12LineColorLight
        else -> ChartAo12LineColorDark
    } // Ao12 line color
    val bestPointColor = ChartBestPointColor // Best point color
    val gridLineColor = when (LocalThemePreference.current) {
        AppThemeType.BLUE -> Color.White
        AppThemeType.LIGHT -> ChartGridLineColorLight
        else -> ChartGridLineColorDark
    } // Grid lines
    val axisLabelColor = when (LocalThemePreference.current) {
        AppThemeType.BLUE -> Color.White
        AppThemeType.LIGHT -> ChartAxisLabelColorLight
        else -> ChartAxisLabelColorDark
    } // Axis labels

    // Fixed values for positioning (no panning or zooming)
    val offsetX = 0f
    val scale = 1f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp)
        ) {
            // If no solves, show a message
            if (solves.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No solves available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (LocalThemePreference.current) {
                            AppThemeType.BLUE -> Color.White
                            AppThemeType.LIGHT -> Color.Black
                            else -> Color.White
                        }
                    )
                }
            } else {
                // Prepare data for the chart
                val solveTimes = solves.map {
                    getEffectiveTime(it.time, it.status).toDouble() / 1000.0 // Convert to seconds
                }

                // Simple chart legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Solves legend
                    ChartLegendItem(
                        color = solveLineColor,
                        label = "Everything",
                        isPoint = false
                    )

                    // Best times legend
                    ChartLegendItem(
                        color = bestPointColor,
                        label = "Best",
                        isPoint = true
                    )

                    // Ao5 legend if available
                    if (movingAverages.containsKey("ao5") && movingAverages["ao5"]!!.isNotEmpty()) {
                        ChartLegendItem(
                            color = ao5LineColor,
                            label = "Ao5",
                            isPoint = false
                        )
                    }

                    // Ao12 legend if available
                    if (movingAverages.containsKey("ao12") && movingAverages["ao12"]!!.isNotEmpty()) {
                        ChartLegendItem(
                            color = ao12LineColor,
                            label = "Ao12",
                            isPoint = false
                        )
                    }
                }

                // Interactive chart using Canvas with gesture detection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, end = 48.dp, bottom = 36.dp) // Add padding for axis labels
                ) {
                    // Find min and max values for scaling
                    val allValues = mutableListOf<Double>()
                    allValues.addAll(solveTimes)

                    movingAverages.forEach { (_, values) ->
                        allValues.addAll(values.map { it / 1000.0 }) // Convert to seconds
                    }

                    val minValue = allValues.minOrNull() ?: 0.0
                    val maxValue = allValues.maxOrNull() ?: 0.0
                    val valueRange = maxValue - minValue

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Horizontal grid lines with Y-axis labels
                        val numHorizontalLines = 6 // Increased for better granularity
                        for (i in 0..numHorizontalLines) {
                            val y = canvasHeight * (1 - i.toFloat() / numHorizontalLines)
                            // Draw grid line across the entire width
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )

                            // Calculate the time value for this grid line
                            val timeValue = minValue + (valueRange * i / numHorizontalLines)

                            // Convert to milliseconds for formatTime function
                            val timeMillis = (timeValue * 1000).toLong()

                            // Draw time label on Y-axis with better alignment
                            drawContext.canvas.nativeCanvas.drawText(
                                formatTime(timeMillis),
                                canvasWidth + 8, // More space from the chart edge
                                y + 4, // Better vertical alignment with grid line
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.argb(
                                        (axisLabelColor.alpha * 255).toInt(),
                                        (axisLabelColor.red * 255).toInt(),
                                        (axisLabelColor.green * 255).toInt(),
                                        (axisLabelColor.blue * 255).toInt()
                                    )
                                    textSize = 11.sp.toPx() // Slightly smaller for better fit
                                    textAlign = android.graphics.Paint.Align.LEFT
                                }
                            )
                        }

                        // Calculate the visible range based on zoom and pan
                        val visibleWidth = canvasWidth / scale
                        val startX = -offsetX / scale
                        val endX = startX + visibleWidth

                        // Calculate how many vertical grid lines to show based on zoom level
                        val numVerticalLines = (5 * scale).toInt().coerceIn(5, 20)

                        // Calculate which solve indices are visible
                        val scaledWidth = canvasWidth * scale
                        val pointSpacing = if (solveTimes.size > 1) scaledWidth / (solveTimes.size - 1) else 0f

                        // Draw vertical grid lines and x-axis labels
                        if (solveTimes.isNotEmpty()) {
                            // Calculate visible solve indices
                            val startIndex = ((startX * (solveTimes.size - 1)) / scaledWidth).toInt()
                                .coerceIn(0, solveTimes.size - 1)
                            val endIndex =
                                ((endX * (solveTimes.size - 1)) / scaledWidth).toInt().coerceIn(0, solveTimes.size - 1)

                            // Calculate how many grid lines to show based on zoom level
                            // More grid lines when zoomed in, fewer when zoomed out
                            val adjustedNumVerticalLines = (numVerticalLines * scale).toInt().coerceIn(5, 10)

                            // Calculate step size based on visible range and adjusted grid line count
                            val visibleRange = endIndex - startIndex
                            val step = (visibleRange / adjustedNumVerticalLines.toFloat()).coerceAtLeast(1f)

                            // Draw grid lines and labels at regular intervals
                            for (i in 0..adjustedNumVerticalLines) {
                                // Calculate index based on visible range
                                val index =
                                    (startIndex + (i * visibleRange / adjustedNumVerticalLines.toFloat())).toInt()
                                        .coerceIn(0, solveTimes.size - 1)
                                val x = (index * pointSpacing) + offsetX

                                // Only draw if within visible range
                                if (x >= 0 && x <= canvasWidth) {
                                    // Draw vertical grid line
                                    drawLine(
                                        color = gridLineColor,
                                        start = Offset(x, 0f),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 1f
                                    )

                                    // Draw solve number label directly under the point
                                    // Use a smaller font size and better positioning
                                    drawContext.canvas.nativeCanvas.drawText(
                                        (index + 1).toString(), // +1 to start from 1 instead of 0
                                        x,
                                        canvasHeight + 26, // Position further below the chart
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.argb(
                                                (axisLabelColor.alpha * 255).toInt(),
                                                (axisLabelColor.red * 255).toInt(),
                                                (axisLabelColor.green * 255).toInt(),
                                                (axisLabelColor.blue * 255).toInt()
                                            )
                                            textSize = 11.sp.toPx() // Slightly smaller for better fit
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                    )
                                }
                            }
                        }

                        // Draw solve times line with fill
                        if (solveTimes.size > 1) {
                            val path = Path()
                            val fillPath = Path()
                            val scaledWidth = canvasWidth * scale
                            val pointSpacing = scaledWidth / (solveTimes.size - 1)

                            solveTimes.forEachIndexed { index, time ->
                                val x = (index * pointSpacing) + offsetX
                                val normalizedValue = if (valueRange > 0) (time - minValue) / valueRange else 0.5
                                val y = canvasHeight * (1 - normalizedValue.toFloat())

                                // Only draw points that are within the visible range
                                if (x >= 0 && x <= canvasWidth) {
                                    if (index == 0 || path.isEmpty) {
                                        path.moveTo(x, y)
                                        fillPath.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }
                                }
                            }

                            // Complete the fill path
                            if (!fillPath.isEmpty) {
                                // Find the last visible X coordinate
                                val lastVisibleX = ((solveTimes.size - 1) * pointSpacing) + offsetX
                                val visibleLastX = if (lastVisibleX > canvasWidth) canvasWidth else lastVisibleX

                                // Complete the path by going down to the bottom of the chart,
                                // then back to the start, and closing the path
                                fillPath.lineTo(
                                    visibleLastX,
                                    canvasHeight - 1
                                ) // Subtract 1 to avoid overlap with bottom grid line
                                fillPath.lineTo(
                                    offsetX,
                                    canvasHeight - 1
                                ) // Subtract 1 to avoid overlap with bottom grid line
                                fillPath.close()

                                // Draw the fill with slightly reduced alpha for better visibility of grid lines
                                drawPath(
                                    path = fillPath,
                                    color = solveLineColor.copy(alpha = 0.08f) // Reduced alpha for better grid visibility
                                )
                            }

                            // Draw the line
                            drawPath(
                                path = path,
                                color = solveLineColor,
                                style = Stroke(width = 2f)
                            )
                        }

                        // Draw Ao5 line with fill if available
                        if (movingAverages.containsKey("ao5") && movingAverages["ao5"]!!.size > 1) {
                            val ao5Values = movingAverages["ao5"]!!.map { it / 1000.0 }
                            val path = Path()
                            val fillPath = Path()
                            val scaledWidth = canvasWidth * scale
                            val pointSpacing = scaledWidth / (ao5Values.size - 1)

                            ao5Values.forEachIndexed { index, time ->
                                val x = (index * pointSpacing) + offsetX
                                val normalizedValue = if (valueRange > 0) (time - minValue) / valueRange else 0.5
                                val y = canvasHeight * (1 - normalizedValue.toFloat())

                                // Only draw points that are within the visible range
                                if (x >= 0 && x <= canvasWidth) {
                                    if (index == 0 || path.isEmpty) {
                                        path.moveTo(x, y)
                                        fillPath.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }
                                }
                            }

                            // Complete the fill path
                            if (!fillPath.isEmpty) {
                                // Find the last visible X coordinate
                                val lastVisibleX = ((ao5Values.size - 1) * pointSpacing) + offsetX
                                val visibleLastX = if (lastVisibleX > canvasWidth) canvasWidth else lastVisibleX

                                // Complete the path by going down to the bottom of the chart,
                                // then back to the start, and closing the path
                                fillPath.lineTo(
                                    visibleLastX,
                                    canvasHeight - 1
                                ) // Subtract 1 to avoid overlap with bottom grid line
                                fillPath.lineTo(
                                    offsetX,
                                    canvasHeight - 1
                                ) // Subtract 1 to avoid overlap with bottom grid line
                                fillPath.close()

                                // Draw the fill with slightly reduced alpha for better visibility of grid lines
                                drawPath(
                                    path = fillPath,
                                    color = ao5LineColor.copy(alpha = 0.08f) // Reduced alpha for better grid visibility
                                )
                            }

                            // Draw the line
                            drawPath(
                                path = path,
                                color = ao5LineColor,
                                style = Stroke(width = 2f)
                            )
                        }

                        // Draw Ao12 line with fill if available
                        if (movingAverages.containsKey("ao12") && movingAverages["ao12"]!!.size > 1) {
                            val ao12Values = movingAverages["ao12"]!!.map { it / 1000.0 }
                            val path = Path()
                            val fillPath = Path()
                            val scaledWidth = canvasWidth * scale
                            val pointSpacing = scaledWidth / (ao12Values.size - 1)

                            ao12Values.forEachIndexed { index, time ->
                                val x = (index * pointSpacing) + offsetX
                                val normalizedValue = if (valueRange > 0) (time - minValue) / valueRange else 0.5
                                val y = canvasHeight * (1 - normalizedValue.toFloat())

                                // Only draw points that are within the visible range
                                if (x >= 0 && x <= canvasWidth) {
                                    if (index == 0 || path.isEmpty) {
                                        path.moveTo(x, y)
                                        fillPath.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }
                                }
                            }

                            // Complete the fill path
                            if (!fillPath.isEmpty) {
                                // Find the last visible X coordinate
                                val lastVisibleX = ((ao12Values.size - 1) * pointSpacing) + offsetX
                                val visibleLastX = if (lastVisibleX > canvasWidth) canvasWidth else lastVisibleX

                                // Complete the path by going down to the bottom of the chart,
                                // then back to the start, and closing the path
                                fillPath.lineTo(
                                    visibleLastX,
                                    canvasHeight - 1
                                ) // Subtract 1 to avoid overlap with bottom grid line
                                fillPath.lineTo(
                                    offsetX,
                                    canvasHeight - 1
                                ) // Subtract 1 to avoid overlap with bottom grid line
                                fillPath.close()

                                // Draw the fill with slightly reduced alpha for better visibility of grid lines
                                drawPath(
                                    path = fillPath,
                                    color = ao12LineColor.copy(alpha = 0.08f) // Reduced alpha for better grid visibility
                                )
                            }

                            // Draw the line
                            drawPath(
                                path = path,
                                color = ao12LineColor,
                                style = Stroke(width = 2f)
                            )
                        }

                        // Draw yellow points for best times
                        if (solveTimes.isNotEmpty()) {
                            // Find the best time
                            val bestTime = solveTimes.minOrNull() ?: 0.0

                            // Draw a point at each occurrence of the best time
                            solveTimes.forEachIndexed { index, time ->
                                if (time == bestTime) {
                                    val x = (index * (canvasWidth * scale / (solveTimes.size - 1))) + offsetX
                                    val normalizedValue = if (valueRange > 0) (time - minValue) / valueRange else 0.5
                                    val y = canvasHeight * (1 - normalizedValue.toFloat())

                                    // Only draw points that are within the visible range
                                    if (x >= 0 && x <= canvasWidth) {
                                        // Draw a yellow circle
                                        drawCircle(
                                            color = bestPointColor,
                                            radius = 6f,
                                            center = Offset(x, y)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A composable that displays a legend item for the chart.
 *
 * @param color The color of the legend item
 * @param label The label for the legend item
 * @param isPoint Whether the legend item is a point or a line
 */
@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    isPoint: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = color,
                    shape = if (isPoint) RoundedCornerShape(6.dp) else RoundedCornerShape(2.dp)
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = when (LocalThemePreference.current) {
                // For blue theme, use white for all labels
                AppThemeType.BLUE -> Color.White
                // For light theme, use black
                AppThemeType.LIGHT -> Color.Black
                // For dark theme, use white
                else -> Color.White
            }
        )
    }
}

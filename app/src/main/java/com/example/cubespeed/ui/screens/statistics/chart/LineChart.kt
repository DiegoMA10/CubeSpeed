package com.example.cubespeed.ui.screens.statistics.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cubespeed.ui.theme.ChartAxisLabelColorLight
import com.example.cubespeed.ui.theme.ChartAxisLabelColorDark
import com.example.cubespeed.ui.theme.ChartGridLineColorLight
import com.example.cubespeed.ui.theme.ChartGridLineColorDark
import com.example.cubespeed.ui.theme.isAppInLightTheme
import kotlin.math.max
import kotlin.math.min

/**
 * A composable that displays a line chart with interactive features.
 *
 * @param data The data to display in the chart
 * @param modifier The modifier to apply to the chart
 * @param backgroundColor The background color of the chart
 * @param gridLineColor The color of the grid lines
 * @param axisLabelColor The color of the axis labels
 * @param showLegend Whether to show the legend
 */
@Composable
fun LineChart(
    data: LineChartData,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    gridLineColor: Color = if (isAppInLightTheme) ChartGridLineColorLight else ChartGridLineColorDark,
    axisLabelColor: Color = if (isAppInLightTheme) ChartAxisLabelColorLight else ChartAxisLabelColorDark,
    showLegend: Boolean = true
) {
    // State for pan and zoom
    var offsetX by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var canvasWidth by remember { mutableStateOf(0f) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // If no data, show a message
            if (data.dataSets.isEmpty() || data.dataSets.all { it.entries.isEmpty() }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            } else {
                // Show legend if requested
                if (showLegend) {
                    ChartLegend(data.dataSets)
                }

                // Interactive chart using Canvas with gesture detection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, end = 40.dp, bottom = 20.dp) // Add padding for axis labels
                ) {
                    // Add gesture detection for zoom and pan
                    val transformableState = rememberTransformableState { zoomChange: Float, offsetChange: Offset, _ ->
                        // Update scale (zoom)
                        scale = (scale * zoomChange).coerceIn(1f, 5f) // Limit zoom between 1x and 5x

                        // Update offset (pan)
                        offsetX = (offsetX + offsetChange.x).coerceIn(
                            -((scale - 1) * 500), // Left bound
                            0f // Right bound
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformableState)
                    ) {
                        // Store canvas width in state for use outside the Canvas
                        canvasWidth = size.width
                        val canvasHeight = size.height

                        // Calculate visible range based on zoom and pan
                        val visibleWidth = canvasWidth / scale
                        val startX = -offsetX / scale
                        val endX = startX + visibleWidth

                        // Draw grid lines and axis labels
                        drawGridLinesAndLabels(
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            data = data,
                            gridLineColor = gridLineColor,
                            axisLabelColor = axisLabelColor
                        )

                        // Draw each data set
                        for (dataSet in data.dataSets) {
                            drawDataSet(
                                dataSet = dataSet,
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                yMin = data.yMin,
                                yMax = data.yMax,
                                scale = scale,
                                offsetX = offsetX,
                                axisLabelColor = axisLabelColor
                            )
                        }
                    }

                    // X-axis labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Calculate visible range of x-labels based on zoom and pan
                        val visibleLabels = calculateVisibleLabels(data.xLabels, scale, offsetX, canvasWidth)

                        for (label in visibleLabels) {
                            Text(
                                text = label.second,
                                color = axisLabelColor,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }

                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val numLabels = 5
                        for (i in numLabels downTo 0) {
                            val value = data.yMin + (data.yMax - data.yMin) * i / numLabels
                            Text(
                                text = String.format("%.1f", value),
                                color = axisLabelColor,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculates which x-axis labels should be visible based on the current zoom and pan.
 */
private fun calculateVisibleLabels(
    labels: List<String>,
    scale: Float,
    offsetX: Float,
    width: Float
): List<Pair<Float, String>> {
    if (labels.isEmpty()) return emptyList()

    val result = mutableListOf<Pair<Float, String>>()
    val visibleWidth = width / scale
    val startX = -offsetX / scale
    val endX = startX + visibleWidth

    // Calculate how many labels to show based on available width
    val labelCount = min(labels.size, max(5, (width / 60).toInt()))
    val step = max(1, labels.size / labelCount)

    for (i in labels.indices step step) {
        val x = i.toFloat() / (labels.size - 1) * width
        if (x >= startX && x <= endX) {
            result.add(Pair(x, labels[i]))
        }
    }

    return result
}

/**
 * Draws grid lines and axis labels on the chart.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLinesAndLabels(
    canvasWidth: Float,
    canvasHeight: Float,
    data: LineChartData,
    gridLineColor: Color,
    axisLabelColor: Color
) {
    // Horizontal grid lines
    val numHorizontalLines = 5
    for (i in 0..numHorizontalLines) {
        val y = canvasHeight * (1 - i.toFloat() / numHorizontalLines)
        drawLine(
            color = gridLineColor,
            start = Offset(0f, y),
            end = Offset(canvasWidth, y),
            strokeWidth = 1f
        )
    }

    // Vertical grid lines
    val numVerticalLines = 5
    for (i in 0..numVerticalLines) {
        val x = canvasWidth * (i.toFloat() / numVerticalLines)
        drawLine(
            color = gridLineColor,
            start = Offset(x, 0f),
            end = Offset(x, canvasHeight),
            strokeWidth = 1f
        )
    }
}

/**
 * Draws a single data set on the chart.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataSet(
    dataSet: LineChartDataSet,
    canvasWidth: Float,
    canvasHeight: Float,
    yMin: Float,
    yMax: Float,
    scale: Float,
    offsetX: Float,
    axisLabelColor: Color
) {
    if (dataSet.entries.isEmpty()) return

    val yRange = yMax - yMin
    val path = Path()
    val fillPath = Path()
    val scaledWidth = canvasWidth * scale
    val pointSpacing = if (dataSet.entries.size > 1) {
        scaledWidth / (dataSet.entries.size - 1)
    } else {
        0f
    }

    // Draw the line
    dataSet.entries.forEachIndexed { index, entry ->
        val x = (index * pointSpacing) + offsetX
        val normalizedY = if (yRange > 0) (entry.y - yMin) / yRange else 0.5f
        val y = canvasHeight * (1 - normalizedY)

        // Only draw points that are within the visible range
        if (x >= 0 && x <= canvasWidth) {
            if (index == 0 || path.isEmpty) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            // Draw highlighted points
            if (entry.highlighted) {
                drawCircle(
                    color = dataSet.highlightColor,
                    radius = dataSet.highlightRadius,
                    center = Offset(x, y)
                )
            }
            // Draw circles at each point if requested
            else if (dataSet.drawCircles) {
                drawCircle(
                    color = dataSet.color,
                    radius = dataSet.lineWidth,
                    center = Offset(x, y)
                )
            }

            // Draw values if requested
            if (dataSet.drawValues) {
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f", entry.y),
                    x,
                    y - 10,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (axisLabelColor.alpha * 255).toInt(),
                            (axisLabelColor.red * 255).toInt(),
                            (axisLabelColor.green * 255).toInt(),
                            (axisLabelColor.blue * 255).toInt()
                        )
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }

    // Draw the line
    drawPath(
        path = path,
        color = dataSet.color,
        style = Stroke(width = dataSet.lineWidth)
    )

    // Fill under the line if requested
    dataSet.fillColor?.let { fillColor ->
        // Complete the fill path
        val lastEntry = dataSet.entries.last()
        val lastX = ((dataSet.entries.size - 1) * pointSpacing) + offsetX
        val lastY = canvasHeight
        fillPath.lineTo(lastX, lastY)
        fillPath.lineTo(offsetX, lastY)
        fillPath.close()

        drawPath(
            path = fillPath,
            color = fillColor
        )
    }
}

/**
 * Displays a legend for the chart.
 */
@Composable
private fun ChartLegend(dataSets: List<LineChartDataSet>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (dataSet in dataSets) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = dataSet.color,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Text(
                    text = dataSet.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAppInLightTheme) Color.Black else Color.White
                )
            }
        }
    }
}

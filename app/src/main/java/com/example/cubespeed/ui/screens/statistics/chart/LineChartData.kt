package com.example.cubespeed.ui.screens.statistics.chart

import androidx.compose.ui.graphics.Color

/**
 * Represents a single data point in a line chart.
 *
 * @param x The x-coordinate value
 * @param y The y-coordinate value
 * @param highlighted Whether this point should be highlighted
 */
data class LineChartEntry(
    val x: Float,
    val y: Float,
    val highlighted: Boolean = false
)

/**
 * Represents a set of data points for a single line in the chart.
 *
 * @param entries The list of data points
 * @param label The label for this data set
 * @param color The color of the line
 * @param lineWidth The width of the line
 * @param drawCircles Whether to draw circles at each data point
 * @param drawValues Whether to draw values at each data point
 * @param fillColor The color to fill under the line (null for no fill)
 * @param highlightColor The color to use for highlighted points
 * @param highlightRadius The radius of highlighted points
 */
data class LineChartDataSet(
    val entries: List<LineChartEntry>,
    val label: String,
    val color: Color,
    val lineWidth: Float = 2f,
    val drawCircles: Boolean = false,
    val drawValues: Boolean = false,
    val fillColor: Color? = null,
    val highlightColor: Color = Color.Yellow,
    val highlightRadius: Float = 6f
)

/**
 * Represents all data for a line chart.
 *
 * @param dataSets The list of data sets to display
 * @param xLabels The labels for the x-axis
 * @param yMin The minimum value for the y-axis
 * @param yMax The maximum value for the y-axis
 * @param xMin The minimum value for the x-axis
 * @param xMax The maximum value for the x-axis
 */
data class LineChartData(
    val dataSets: List<LineChartDataSet>,
    val xLabels: List<String> = emptyList(),
    val yMin: Float = 0f,
    val yMax: Float = 0f,
    val xMin: Float = 0f,
    val xMax: Float = 0f
)
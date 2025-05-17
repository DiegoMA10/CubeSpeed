package com.example.cubespeed.ui.screens.statistics.chart

import androidx.compose.ui.graphics.Color
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.ui.screens.timer.utils.getEffectiveTime

/**
 * Utility functions for creating chart data from solve data.
 */
object ChartUtils {

    /**
     * Converts a list of solves and moving averages to LineChartData.
     *
     * @param solves The list of solves to display
     * @param movingAverages Map of moving average type to list of values
     * @param solveLineColor Color for the solve times line
     * @param ao5LineColor Color for the Ao5 line
     * @param ao12LineColor Color for the Ao12 line
     * @param bestPointColor Color for the best time points
     * @return LineChartData for use with the LineChart composable
     */
    fun createChartData(
        solves: List<Solve>,
        movingAverages: Map<String, List<Double>>,
        solveLineColor: Color = Color.White,
        ao5LineColor: Color = Color(0xFFFF5252),
        ao12LineColor: Color = Color(0xFF4CAF50),
        bestPointColor: Color = Color(0xFFFFEB3B)
    ): LineChartData {
        if (solves.isEmpty()) {
            return LineChartData(emptyList())
        }

        // Convert solve times to seconds
        val solveTimes = solves.map { 
            getEffectiveTime(it.time, it.status).toDouble() / 1000.0 
        }

        // Find min and max values for scaling
        val allValues = mutableListOf<Double>()
        allValues.addAll(solveTimes)

        movingAverages.forEach { (_, values) ->
            allValues.addAll(values.map { it / 1000.0 }) // Convert to seconds
        }

        val minValue = allValues.minOrNull() ?: 0.0
        val maxValue = allValues.maxOrNull() ?: 0.0

        // Create data sets
        val dataSets = mutableListOf<LineChartDataSet>()

        // Solve times data set
        val solveEntries = solveTimes.mapIndexed { index, time ->
            // Find the best time
            val bestTime = solveTimes.minOrNull() ?: 0.0
            // Highlight if this is a best time
            val isHighlighted = time == bestTime

            LineChartEntry(
                x = index.toFloat(),
                y = time.toFloat(),
                highlighted = isHighlighted
            )
        }

        dataSets.add(
            LineChartDataSet(
                entries = solveEntries,
                label = "Todo",
                color = solveLineColor,
                highlightColor = bestPointColor,
                fillColor = solveLineColor.copy(alpha = 0.1f) // Subtle fill under the curve
            )
        )

        // Ao5 data set if available
        if (movingAverages.containsKey("ao5") && movingAverages["ao5"]!!.size > 1) {
            val ao5Values = movingAverages["ao5"]!!.map { it / 1000.0 }
            val ao5Entries = ao5Values.mapIndexed { index, time ->
                LineChartEntry(
                    x = index.toFloat(),
                    y = time.toFloat()
                )
            }

            dataSets.add(
                LineChartDataSet(
                    entries = ao5Entries,
                    label = "Ao5",
                    color = ao5LineColor,
                    fillColor = ao5LineColor.copy(alpha = 0.1f) // Subtle fill under the curve
                )
            )
        }

        // Ao12 data set if available
        if (movingAverages.containsKey("ao12") && movingAverages["ao12"]!!.size > 1) {
            val ao12Values = movingAverages["ao12"]!!.map { it / 1000.0 }
            val ao12Entries = ao12Values.mapIndexed { index, time ->
                LineChartEntry(
                    x = index.toFloat(),
                    y = time.toFloat()
                )
            }

            dataSets.add(
                LineChartDataSet(
                    entries = ao12Entries,
                    label = "Ao12",
                    color = ao12LineColor,
                    fillColor = ao12LineColor.copy(alpha = 0.1f) // Subtle fill under the curve
                )
            )
        }

        // Create X-axis labels (solve numbers)
        val xLabels = solves.indices.map { (it + 1).toString() }

        return LineChartData(
            dataSets = dataSets,
            xLabels = xLabels,
            yMin = minValue.toFloat(),
            yMax = maxValue.toFloat(),
            xMin = 0f,
            xMax = (solves.size - 1).toFloat()
        )
    }
}

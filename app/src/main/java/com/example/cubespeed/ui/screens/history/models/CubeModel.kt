package com.example.cubespeed.ui.screens.history.models


import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.createBitmap
import com.caverock.androidsvg.SVG


/**
 * Represents a 3x3 Rubik's Cube using the facelet model.
 * The cube is represented as an array of 54 facelets (9 per face).
 * The faces are ordered as follows: U (up), L (left), F (front), R (right), B (back), D (down).
 */

/**
 * A composable that renders a 2D net of a Rubik's Cube based on a scramble.
 *
 * @param scramble The scramble to apply to the cube
 * @param cubeType The type of cube to visualize
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun ScrambleVisualization(
    scramble: String,
    cubeType: com.example.cubespeed.model.CubeType = com.example.cubespeed.model.CubeType.CUBE_3X3,
    modifier: Modifier = Modifier
) {
    // For 3x3 cube, use the CubeModel to render the cube

    // For other cube types, render a text representation
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Get the SVG from TNoodle (not used directly, but kept for reference)
        val svg = remember(scramble, cubeType) {
            println("[DEBUG_LOG] Starting SVG generation in ScrambleVisualization for cube type: ${cubeType.displayName}")
            val startTime = System.currentTimeMillis()
            val result =
                com.example.cubespeed.model.ScrambleGenerator.getInstance().generateScrambleSvg(cubeType, scramble)
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            println("[DEBUG_LOG] Finished SVG generation in ScrambleVisualization for cube type: ${cubeType.displayName} in $duration ms")
            result
        }

        // Render a text representation of the cube type and scramble
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Only render the SVG image if svg is not null and svgToBitmap returns a valid bitmap
            println("[DEBUG_LOG] Attempting to convert SVG to bitmap for cube type: ${cubeType.displayName}")
            val bitmap = svg?.let {
                println("[DEBUG_LOG] SVG is not null, converting to bitmap")
                svgToBitmap(it.toString(), 700, 700)
            }
            if (bitmap != null) {
                println("[DEBUG_LOG] Bitmap created successfully, rendering SvgImage")
                SvgImage(bitmap)
            }
        }
    }

}


@Composable
fun SvgImage(bitmap: Bitmap) {
    println("[DEBUG_LOG] SvgImage composable being recomposed with bitmap size: ${bitmap.width}x${bitmap.height}")

    Image(
        painter = BitmapPainter(bitmap.asImageBitmap()),
        contentDescription = null
    )
}


fun svgToBitmap(svgContent: String, width: Int, height: Int): Bitmap? {
    println("[DEBUG_LOG] Starting SVG to Bitmap conversion")
    val startTime = System.currentTimeMillis()

    return try {
        val svg = SVG.getFromString(svgContent)
        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")
        val bitmap = createBitmap(width, height)
        val canvas = android.graphics.Canvas(bitmap)
        svg.renderToCanvas(canvas)

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        println("[DEBUG_LOG] Finished SVG to Bitmap conversion in $duration ms")

        bitmap
    } catch (e: Exception) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        println("[DEBUG_LOG] Error converting SVG to Bitmap after $duration ms: ${e.message}")
        e.printStackTrace()
        null
    }
}
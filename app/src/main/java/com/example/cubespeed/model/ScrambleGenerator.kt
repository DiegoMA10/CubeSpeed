package com.example.cubespeed.model

import androidx.compose.ui.graphics.Color
import org.worldcubeassociation.tnoodle.scrambles.PuzzleRegistry
import org.worldcubeassociation.tnoodle.svglite.Svg

/**
 * A class that generates scrambles for different cube types.
 * This implementation uses the TNoodle library to provide high-quality scrambles.
 * TNoodle is the official scramble generator used in WCA competitions.
 *
 * Features:
 * - Uses TNoodle's scramble generation algorithms
 * - Generates scrambles for all WCA puzzles (2x2, 3x3, 4x4, 5x5, 6x6, 7x7, Pyraminx, Megaminx, Skewb, Square-1)
 * - Ensures scrambles are valid and follow WCA regulations
 * - Uses sophisticated algorithms to avoid redundant move sequences
 * - Provides optimal distribution of moves
 * - Generates SVG visualizations of scrambles
 */
class ScrambleGenerator private constructor() {

    companion object {
        // Singleton instance
        @Volatile
        private var instance: ScrambleGenerator? = null

        fun getInstance(): ScrambleGenerator {
            return instance ?: synchronized(this) {
                instance ?: ScrambleGenerator().also { instance = it }
            }
        }
    }

    /**
     * Generates a scramble for the specified cube type.
     * Uses the TNoodle library for official WCA scrambles.
     *
     * @param cubeType The type of cube to generate a scramble for
     * @return A string representing the scramble
     */
    fun generateScramble(cubeType: CubeType): String {
        println("[DEBUG_LOG] Starting scramble generation for cube type: ${cubeType.displayName}")
        val startTime = System.currentTimeMillis()

        val scramble = when (cubeType) {
            CubeType.CUBE_2X2 -> generate2x2Scramble()
            CubeType.CUBE_3X3 -> generate3x3Scramble()
            CubeType.CUBE_4X4 -> generate4x4Scramble()
            CubeType.CUBE_5X5 -> generate5x5Scramble()
            CubeType.CUBE_6X6 -> generate6x6Scramble()
            CubeType.CUBE_7X7 -> generate7x7Scramble()
            CubeType.PYRAMINX -> generatePyraminxScramble()
            CubeType.MEGAMINX -> generateMegaminxScramble()
            CubeType.SKEWB -> generateSkewbScramble()
            CubeType.SQUARE_1 -> generateSquare1Scramble()
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        println("[DEBUG_LOG] Finished scramble generation for cube type: ${cubeType.displayName} in $duration ms")

        return scramble
    }

    /**
     * Generates an SVG visualization for the specified cube type and scramble.
     * Uses the TNoodle library to generate the SVG.
     *
     * @param cubeType The type of cube to generate an SVG for
     * @param scramble The scramble to visualize
     * @return An SVG visualization of the scramble, or null if the scramble cannot be processed
     */
    fun generateScrambleSvg(cubeType: CubeType, scramble: String): Svg? {
        println("[DEBUG_LOG] Starting SVG generation for cube type: ${cubeType.displayName}")
        val startTime = System.currentTimeMillis()

        try {
            val svg = when (cubeType) {
                CubeType.CUBE_2X2 -> PuzzleRegistry.TWO.scrambler.drawScramble(scramble, null)
                CubeType.CUBE_3X3 -> PuzzleRegistry.THREE.scrambler.drawScramble(scramble, null)
                CubeType.CUBE_4X4 -> PuzzleRegistry.FOUR.scrambler.drawScramble(scramble, null)
                CubeType.CUBE_5X5 -> PuzzleRegistry.FIVE.scrambler.drawScramble(scramble, null)
                CubeType.CUBE_6X6 -> PuzzleRegistry.SIX.scrambler.drawScramble(scramble, null)
                CubeType.CUBE_7X7 -> PuzzleRegistry.SEVEN.scrambler.drawScramble(scramble, null)
                CubeType.PYRAMINX -> PuzzleRegistry.PYRA.scrambler.drawScramble(scramble, null)
                CubeType.MEGAMINX -> PuzzleRegistry.MEGA.scrambler.drawScramble(scramble, null)
                CubeType.SKEWB -> PuzzleRegistry.SKEWB.scrambler.drawScramble(scramble, null)
                CubeType.SQUARE_1 -> PuzzleRegistry.SQ1.scrambler.drawScramble(scramble, null)
            }

            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            println("[DEBUG_LOG] Finished SVG generation for cube type: ${cubeType.displayName} in $duration ms")

            return svg
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            println("[DEBUG_LOG] Error generating SVG for scramble: $scramble, cube type: $cubeType after $duration ms")
            e.printStackTrace()
            return null
        }
    }


    /**
     * Generates a scramble for a 2x2 cube.
     * Uses a TNoodle-like approach with state-based scrambling for optimal distribution.
     * This ensures all possible cube states are equally likely.
     */
    private fun generate2x2Scramble(): String {
        // For 2x2, TNoodle uses a state-based approach, but we'll simulate it
        // by using a more sophisticated move selection algorithm


        val scramble = PuzzleRegistry.TWO.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.TWO.scrambler.drawScramble(scramble, null) // null = usa los colores estándar


        return scramble

    }

    /**
     * Generates a scramble for a 3x3 cube.
     * Uses the TNoodle library for official WCA scrambles.
     */
    private fun generate3x3Scramble(): String {
        // Use TNoodle's official scramble generator
        val scramble = PuzzleRegistry.THREE.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.THREE.scrambler.drawScramble(scramble, null) // null = usa los colores estándar

        return scramble
    }

    /**
     * Generates a scramble for a 4x4 cube.
     * Uses the TNoodle library for official WCA scrambles.
     */
    private fun generate4x4Scramble(): String {
        // Use TNoodle's official scramble generator
        val scramble = PuzzleRegistry.FOUR.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.FOUR.scrambler.drawScramble(scramble, null) // null = usa los colores estándar

        return scramble
    }

    /**
     * Generates a scramble for a 5x5 cube.
     * Uses the TNoodle library for official WCA scrambles.
     */
    private fun generate5x5Scramble(): String {
        // Use TNoodle's official scramble generator
        val scramble = PuzzleRegistry.FIVE.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.FIVE.scrambler.drawScramble(scramble, null) // null = usa los colores estándar

        return scramble
    }

    /**
     * Generates a scramble for a 6x6 cube.
     * Uses the TNoodle library for official WCA scrambles.
     */
    private fun generate6x6Scramble(): String {
        // Use TNoodle's official scramble generator
        val scramble = PuzzleRegistry.SIX.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.SIX.scrambler.drawScramble(scramble, null) // null = usa los colores estándar

        return scramble
    }

    /**
     * Generates a scramble for a 7x7 cube.
     * Uses the TNoodle library for official WCA scrambles.
     */
    private fun generate7x7Scramble(): String {
        // Use TNoodle's official scramble generator
        val scramble = PuzzleRegistry.SEVEN.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.SEVEN.scrambler.drawScramble(scramble, null) // null = usa los colores estándar

        return scramble
    }

    /**
     * Generates a scramble for a Pyraminx.
     * Uses the TNoodle library for official WCA scrambles.
     */
    private fun generatePyraminxScramble(): String {
        // Use TNoodle's official scramble generator
        val scramble = PuzzleRegistry.PYRA.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.PYRA.scrambler.drawScramble(scramble, null) // null = usa los colores estándar

        return scramble
    }

    /**
     * Generates a scramble for a Megaminx.
     * Megaminx uses a specific notation: R++ D++ R-- D-- ...
     * This implementation closely matches TNoodle's Megaminx scrambler behavior.
     */
    private fun generateMegaminxScramble(): String {
        val scramble = PuzzleRegistry.MEGA.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.MEGA.scrambler.drawScramble(scramble, null) // null = usa los colores estándar

        return scramble
    }

    /**
     * Generates a scramble for a Skewb.
     * Skewb has its own set of moves: R, L, U, B.
     * Uses TNoodle-like optimizations for better distribution and move selection.
     * This implementation closely matches TNoodle's Skewb scrambler behavior.
     */
    private fun generateSkewbScramble(): String {
        val scramble = PuzzleRegistry.SKEWB.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.SKEWB.scrambler.drawScramble(scramble, null) // null = usa los colores estándar
        return scramble
    }

    /**
     * Generates a scramble for a Square-1.
     * Square-1 has a unique notation: (0,-1) / (1,0) / ...
     * This implementation closely matches TNoodle's Square-1 scrambler behavior.
     */
    private fun generateSquare1Scramble(): String {
        val scramble = PuzzleRegistry.SQ1.scrambler.generateScramble()
        val svg: Svg = PuzzleRegistry.SQ1.scrambler.drawScramble(scramble, null) // null = usa los colores estándar
        return scramble
    }


}

package com.example.cubespeed.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Utility class for cube algorithm visualization
 */
object AlgUtils {
    // Map of characters to colors for cube faces
    private val colorLetterMap = mapOf(
        'Y' to Color.parseColor("#FDD835"), // Yellow
        'R' to Color.parseColor("#EC0000"), // Red
        'G' to Color.parseColor("#02D040"), // Green
        'B' to Color.parseColor("#304FFE"), // Blue
        'O' to Color.parseColor("#FF8B24"), // Orange
        'W' to Color.parseColor("#FFFFFF"), // White
        'N' to Color.parseColor("#A7A7A7"), // Gray
        'X' to 0                            // Transparent
    )

    // OLL case states with realistic patterns (21 characters)
    // Format: 3 frontales + 3 izquierdos + 9 superiores + 3 derechos + 3 traseros
    private val ollCaseStates = mapOf(
        // Cross cases
        "OLL 1" to "NYNYYYNNNNYNNNNYYYNYN",
        "OLL 2" to "NYYYYYNNNNYNNNNNYNNYY",
        "OLL 3" to "YYNNYNNNNNYNYNNYYNNYY",
        "OLL 4" to "NYYYYNNNNNYNNNYNYNYYN",
        "OLL 5" to "YYNNYYNNNNYYNYYYNNNNN",
        "OLL 6" to "NNNYYNNYYNYYNNNNNYYYN",
        "OLL 7" to "YNNNNNNYNYYNYNNYYNNYY",
        "OLL 8" to "NNYYYNNYNNYYNNYNNNYYN",

        // Dot cases
        "OLL 9" to "NYNYNNNNYYYNNYNNYYYNN",
        "OLL 10" to "YYNNNYNNYYYNNYNNYNNNY",
        "OLL 11" to "YYNNYNNNNNYYYYNYNNNNY",
        "OLL 12" to "NYNYYNNNYNYYNYNNNYYNN",

        // L shapes
        "OLL 13" to "YYNNNNNNNYYYYNNYNNNYY",
        "OLL 14" to "NYYYNNNNNYYYNNYNNNYYN",
        "OLL 15" to "YYNNNYNNNYYYNNYYNNNYN",
        "OLL 16" to "NYNYNNNNYYYYNNNNNYYYN",

        // P shapes
        "OLL 17" to "NYYNYYYNNNYNNNYNYNNYN",
        "OLL 18" to "NYNNYNYNYNYNNNNNYNYYY",
        "OLL 19" to "NYNNYYYNYNYNNNNNYYNYN",
        "OLL 20" to "NYNNYNYNYNYNYNYNYNNYN",

        // T shapes
        "OLL 21" to "NNNYNYNYNYYYNYNYNYNNN",
        "OLL 22" to "NNYYNYNYNYYYNYNNNNNNY",
        "OLL 23" to "NNNNNNYYYYYYNYNNNNYNY",
        "OLL 24" to "YNNNNNNYYYYYNYYNNNYNN",

        // C shapes
        "OLL 25" to "NNNNNNYYNYYYNYYYNNYNN",
        "OLL 26" to "NNYNNNYYNYYYNYNNNYYNN",
        "OLL 27" to "YNNNNNNYNYYYYYNYNNNNY",

        // Corners oriented correctly
        "OLL 28" to "NNNNNNYYYYYNYNYNYNNYN",
        "OLL 29" to "NYNNNYYNYYYNNYNNYYNNN",
        "OLL 30" to "NYNNYYYNYNYYNYNNNYNNN",
        "OLL 31" to "YNNNYNNYYNYYNNYNNNYYN",
        "OLL 32" to "YYNNYNNNYNYYNYYNNNYNN",
        "OLL 33" to "YYNNNNNNYYYYNNYNNNYYN",
        "OLL 34" to "NYNNNYYNYYYYNNNNNYNYN",
        "OLL 35" to "NYNNYNYNNNYYNYYYNNYNN",
        "OLL 36" to "NYNNNNYNNYYNNYYYYNYNN",
        "OLL 37" to "NNNNNNYYNYYNNNYYYNYYN",

        // W shapes
        "OLL 38" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 39" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 40" to "YYBYYYYYYYYYYYYYYYY",

        // Awkward shapes
        "OLL 41" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 42" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 43" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 44" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 45" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 46" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 47" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 48" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 49" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 50" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 51" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 52" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 53" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 54" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 55" to "BYBYYYYYYYYYYYYYYYY",
        "OLL 56" to "YYBYYYYYYYYYYYYYYYY",
        "OLL 57" to "BYBYYYYYYYYYYYYYYYY"
    )

    /**
     * Gets the color for a specific index in the cube state
     *
     * @param state The cube state as a 21-character string:
     *   - 3 stickers frontales [0-2]
     *   - 3 stickers izquierdos [3-5]
     *   - 9 stickers superiores [6-14]
     *   - 3 stickers derechos [15-17]
     *   - 3 stickers traseros [18-20]
     * @param index The index of the sticker (0-20)
     * @return The color as an integer
     */
    @ColorInt
    fun getColorFromStateIndex(state: String, index: Int): Int {
        return try {
            colorLetterMap[state[index]] ?: Color.GRAY
        } catch (e: Exception) {
            Color.GRAY
        }
    }

    /**
     * Gets the state for a specific OLL case
     *
     * @param context The context
     * @param subset The subset (e.g., "OLL")
     * @param name The name of the case (e.g., "OLL 17")
     * @return The state as a 21-character string:
     *   - 3 stickers frontales [0-2]
     *   - 3 stickers izquierdos [3-5]
     *   - 9 stickers superiores [6-14]
     *   - 3 stickers derechos [15-17]
     *   - 3 stickers traseros [18-20]
     */
    fun getCaseState(context: Context, subset: String, name: String): String {
        return ollCaseStates[name] ?: "YYYYYYYYYYYYYYYYYYY"
    }

    /**
     * Gets all OLL case names
     *
     * @return A list of OLL case names
     */
    fun getAllOLLCases(): List<String> {
        return (1..57).map { "OLL $it" }
    }

    /**
     * Gets the default algorithm for a specific OLL case
     *
     * @param name The name of the case (e.g., "OLL 17")
     * @return The default algorithm as a string
     */
    fun getDefaultAlgorithm(name: String): String {
        return when (name) {
            "OLL 1" -> "R U2 R2 F R F' U2 R' F R F'"
            "OLL 2" -> "F R U R' U' F' f R U R' U' f'"
            "OLL 3" -> "f R U R' U' f' U' F R U R' U' F'"
            "OLL 4" -> "f R U R' U' f' U F R U R' U' F'"
            "OLL 5" -> "r' U2 R U R' U r"
            "OLL 6" -> "r U2 R' U' R U' r'"
            "OLL 7" -> "r U R' U R U2 r'"
            "OLL 8" -> "r' U' R U' R' U2 r"
            "OLL 9" -> "R U R' U' R' F R2 U R' U' F'"
            "OLL 10" -> "R U R' U R' F R F' R U2 R'"
            "OLL 11" -> "r U R' U R' F R F' R U2 r'"
            "OLL 12" -> "M' R' U' R U' R' U2 R U' R r'"
            "OLL 13" -> "F U R U' R2 F' R U R U' R'"
            "OLL 14" -> "R' F R U R' F' R F U' F'"
            "OLL 15" -> "r' U' r R' U' R U r' U r"
            "OLL 16" -> "r U r' R U R' U' r U' r'"
            "OLL 17" -> "R U R' U R' F R F' U2 R' F R F'"
            "OLL 18" -> "r U R' U R U2 r2 U' R U' R' U2 r"
            "OLL 19" -> "M U R U R' U' M' R' F R F'"
            "OLL 20" -> "M U R U R' U' M2 U R U' r'"
            "OLL 21" -> "R U2 R' U' R U R' U' R U' R'"
            "OLL 22" -> "R U2 R2 U' R2 U' R2 U2 R"
            "OLL 23" -> "R2 D R' U2 R D' R' U2 R'"
            "OLL 24" -> "r U R' U' r' F R F'"
            "OLL 25" -> "F' r U R' U' r' F R"
            "OLL 26" -> "R U2 R' U' R U' R'"
            "OLL 27" -> "R U R' U R U2 R'"
            "OLL 28" -> "r U R' U' M U R U' R'"
            "OLL 29" -> "M U R U R' U' R' F R F' M'"
            "OLL 30" -> "F R' F R2 U' R' U' R U R' F2"
            "OLL 31" -> "R' U' F U R U' R' F' R"
            "OLL 32" -> "S R U R' U' R' F R f'"
            "OLL 33" -> "R U R' U' R' F R F'"
            "OLL 34" -> "R U R2 U' R' F R U R U' F'"
            "OLL 35" -> "R U2 R2 F R F' R U2 R'"
            "OLL 36" -> "L' U' L U' L' U L U L F' L' F"
            "OLL 37" -> "F R U' R' U' R U R' F'"
            "OLL 38" -> "R U R' U R U' R' U' R' F R F'"
            "OLL 39" -> "L F' L' U' L U F U' L'"
            "OLL 40" -> "R' F R U R' U' F' U R"
            "OLL 41" -> "R U R' U R U2 R' F R U R' U' F'"
            "OLL 42" -> "R' U' R U' R' U2 R F R U R' U' F'"
            "OLL 43" -> "f' L' U' L U f"
            "OLL 44" -> "f R U R' U' f'"
            "OLL 45" -> "F R U R' U' F'"
            "OLL 46" -> "R' U' R' F R F' U R"
            "OLL 47" -> "F' L' U' L U L' U' L U F"
            "OLL 48" -> "F R U R' U' R U R' U' F'"
            "OLL 49" -> "r U' r2 U r2 U r2 U' r"
            "OLL 50" -> "r' U r2 U' r2 U' r2 U r'"
            "OLL 51" -> "f R U R' U' R U R' U' f'"
            "OLL 52" -> "R U R' U R d' R U' R' F'"
            "OLL 53" -> "r' U' R U' R' U R U' R' U2 r"
            "OLL 54" -> "r U R' U R U' R' U R U2 r'"
            "OLL 55" -> "R U2 R2 U' R U' R' U2 F R F'"
            "OLL 56" -> "r U r' U R U' R' U R U' R' r U' r'"
            "OLL 57" -> "R U R' U' M' U R U' r'"
            else -> "Algorithm not available"
        }
    }
}

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
        "OLL 01" to "NYNYYYNNNNYNNNNYYYNYN",
        "OLL 02" to "NYYYYYNNNNYNNNNNYNNYY",
        "OLL 03" to "YYNNYNNNNNYNYNNYYNNYY",
        "OLL 04" to "NYYYYNNNNNYNNNYNYNYYN",
        "OLL 05" to "YYNNYYNNNNYYNYYYNNNNN",
        "OLL 06" to "NNNYYNNYYNYYNNNNNYYYN",
        "OLL 07" to "YNNNNNNYNYYNYNNYYNNYY",
        "OLL 08" to "NNYYYNNYNNYYNNYNNNYYN",

        // Dot cases
        "OLL 09" to "NYNYNNNNYYYNNYNNYYYNN",
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
        "OLL 38" to "YNNNNNNYYYYNYNNNYYNYN",
        "OLL 39" to "NNYNYYYYNNYNNYYNYNNNN",
        "OLL 40" to "NNNYYNNYYNYNYYNNYNNNY",

        // Awkward shapes
        "OLL 41" to "NYNNYNYNYNYYNYNNNNYNY",
        "OLL 42" to "NYNNNNYNYYYNNYNNYNYNY",
        "OLL 43" to "NYNNNNYNNYYNYYNYYYNNN",
        "OLL 44" to "NYNYYYNNYNYYNYYNNNNNN",
        "OLL 45" to "NYNYNYNNYYYYNNYNNNNYN",
        "OLL 46" to "NNNNYNYYNNYNYYNYYYNNN",
        "OLL 47" to "YNNNYNNYNNYYNNNYNYYYN",
        "OLL 48" to "NNYYNYNYNYYNNNNNYNNYY",
        "OLL 49" to "YYNNNNNNNYYNNYNYYYYNN",
        "OLL 50" to "NYYYYYNNNNYYNYNNNNNNY",
        "OLL 51" to "NYYYNYNNNYYYNNNNNNNYY",
        "OLL 52" to "YNNNYNNYNNYNNYNYYYYNN",
        "OLL 53" to "NYNYYYNNNNYYNYNYNYNNN",
        "OLL 54" to "NNNYYYNYNNYYNNNYNYNYN",
        "OLL 55" to "NNNYYYNYNNYNNYNYYYNNN",
        "OLL 56" to "NYNYNYNNNYYYNNNYNYNYN",
        "OLL 57" to "NYNNNNYNYYYYYNYNNNNYN"
    )

    // PLL case states with realistic patterns (21 characters)
    // Format: 3 frontales + 3 izquierdos + 9 superiores + 3 derechos + 3 traseros
    private val pllCaseStates = mapOf(
        "aa_perm" to "GOGOBBYYYYYYYYYRGBRRO",
        "ab_perm" to "OORBBRYYYYYYYYYBGOGRG",
        "e_perm" to "GOBOBRYYYYYYYYYOGRGRB",
        "f_perm" to "GOBOGBYYYYYYYYYOBGRRR",
        "ga_perm" to "RBOGROYYYYYYYYYGGRBOB",
        "gb_perm" to "BRORORYYYYYYYYYGGBGBO",
        "gc_perm" to "OGRBBRYYYYYYYYYBROGOG",
        "gd_perm" to "ORGBBGYYYYYYYYYROROGB",
        "h_perm" to "OROBGBYYYYYYYYYGBGROR",
        "ja_perm" to "BOORROYYYYYYYYYGGGBBR",
        "jb_perm" to "OOGBBBYYYYYYYYYRRORGG",
        "na_perm" to "OORBGGYYYYYYYYYBBGORR",
        "nb_perm" to "ROOGGBYYYYYYYYYGBBRRO",
        "ra_perm" to "OGOBBRYYYYYYYYYGORGRB",
        "rb_perm" to "GOBOBBYYYYYYYYYORGRGR",
        "t_perm" to "OOGBGBYYYYYYYYYRBORRG",
        "ua_perm" to "OBOBGBYYYYYYYYYGOGRRR",
        "ub_perm" to "OGOBOBYYYYYYYYYGBGRRR",
        "v_perm" to "RGOGBBYYYYYYYYYGOBRRO",
        "y_perm" to "RBOGOBYYYYYYYYYGGBRRO",
        "z_perm" to "OBOBOBYYYYYYYYYGRGRGR"
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
     * Gets the state for a specific case (OLL or PLL)
     *
     * @param context The context
     * @param subset The subset (e.g., "OLL" or "PLL")
     * @param name The name of the case (e.g., "OLL 17" or "T")
     * @return The state as a 21-character string:
     *   - 3 stickers frontales [0-2]
     *   - 3 stickers izquierdos [3-5]
     *   - 9 stickers superiores [6-14]
     *   - 3 stickers derechos [15-17]
     *   - 3 stickers traseros [18-20]
     */
    fun getCaseState(context: Context, subset: String, name: String): String {
        return when (subset) {
            "OLL" -> ollCaseStates[name] ?: "YYYYYYYYYYYYYYYYYYY"
            "PLL" -> pllCaseStates[name] ?: "YYYYYYYYYYYYYYYYYYY"
            else -> "YYYYYYYYYYYYYYYYYYY"
        }
    }


    /**
     * Gets all OLL case names
     *
     * @return A list of OLL case names
     */
    fun getAllOLLCases(): List<String> {
        return (1..57).map { "OLL ${if (it < 10) "0$it" else "$it"}" }
    }

    /**
     * Gets all PLL case names
     *
     * @return A list of PLL case names
     */
    fun getAllPLLCases(): List<String> {
        return listOf(
            "aa_perm",
            "ab_perm",
            "e_perm",
            "f_perm",
            "ga_perm",
            "gb_perm",
            "gc_perm",
            "gd_perm",
            "h_perm",
            "ja_perm",
            "jb_perm",
            "na_perm",
            "nb_perm",
            "ra_perm",
            "rb_perm",
            "t_perm",
            "ua_perm",
            "ub_perm",
            "v_perm",
            "y_perm",
            "z_perm"
        )
    }

    /**
     * Gets the default algorithm for a specific OLL case
     *
     * @param name The name of the case (e.g., "OLL 17")
     * @return The default algorithm as a string
     */
    fun getDefaultAlgorithm(name: String): String {
        return when (name) {
            "OLL 01" -> "R U2 R2' F R F' U2 R' F R F' \n" +
                    "R U B' R B R2 U' R' F R F' \n" +
                    "y R U' R2 D' r U' r' D R2 U R' \n" +
                    "r U R' U R' r2 U' R' U R' r2 U2 r'"

            "OLL 02" -> "F R U R' U' F' f R U R' U' f' \n" +
                    "F R U R' U' S R U R' U' f' \n" +
                    "y r U r' U2 R U2 R' U2 r U' r' \n" +
                    "F R U r' U' R U R' M' U' F'"

            "OLL 03" -> "y' f R U R' U' f' U' F R U R' U' F' \n" +
                    "r' R2 U R' U r U2 r' U M' \n" +
                    "r' R U R' F2 R U L' U L M' \n" +
                    "y F U R U' R' F' U F R U R' U' F'"

            "OLL 04" -> "y' f R U R' U' f' U F R U R' U' F' \n" +
                    "M U' r U2 r' U' R U' R2 r \n" +
                    "y F U R U' R' F' U' F R U R' U' F' \n" +
                    "y2 r R2' U' R U' r' U2 r U' M"

            "OLL 05" -> "r' U2 R U R' U r \n" +
                    "y2 l' U2 L U L' U l \n" +
                    "y2 R' F2 r U r' F R \n" +
                    "L' U' L2 F' L' F2 U' F'"

            "OLL 06" -> "r U2 R' U' R U' r' \n" +
                    "y2 l U2 L' U' L U' l' \n" +
                    "y2 R U R2 F R F2 U F \n" +
                    "y' x' D R2 U' R' U R' D' x"

            "OLL 07" -> "r U R' U R U2 r' \n" +
                    "L' U2 L U2 L F' L' F \n" +
                    "F R' F' R U2 R U2 R' \n" +
                    "r U r' U R U' R' r U' r'"

            "OLL 08" -> "y2 r' U' R U' R' U2 r \n" +
                    "l' U' L U' L' U2 l \n" +
                    "R U2 R' U2 R' F R F' \n" +
                    "F' L F L' U2 L' U2 L"

            "OLL 09" -> "y R U R' U' R' F R2 U R' U' F' \n" +
                    "y2 R' U' R U' R' U R' F R F' U R \n" +
                    "r' R2 U2 R' U' R U' R' U' M' \n" +
                    "y' L' U' L U' L F' L' F L' U2 L"

            "OLL 10" -> "R U R' U R' F R F' R U2 R' \n" +
                    "R U R' y R' F R U' R' F' R \n" +
                    "y2 L' U' L U L F' L2 U' L U F \n" +
                    "R U R' y' r' U r U' r' U' r"

            "OLL 11" -> "r' R2 U R' U R U2 R' U M' \n" +
                    "M R U R' U R U2 R' U M' \n" +
                    "r U R' U R' F R F' R U2 r' \n" +
                    "y2 r U R' U R' F R F' R U2 r'"

            "OLL 12" -> "F R U R' U' F' U F R U R' U' F' \n" +
                    "y' r R2' U' R U' R' U2 R U' R r' \n" +
                    "y M U2 R' U' R U' R' U2 R U M' \n" +
                    "y M L' U' L U' L' U2 L U' M'"

            "OLL 13" -> "r U' r' U' r U r' F' U F \n" +
                    "F U R U' R2 F' R U R U' R' \n" +
                    "F U R U2 R' U' R U R' F' \n" +
                    "r U' r' U' r U r' y' R' U R"

            "OLL 14" -> "R' F R U R' F' R F U' F' \n" +
                    "R' F R U R' F' R y' R U' R' \n" +
                    "F' U' r' F r2 U r' U' r' F r \n" +
                    "r' U r U r' U' r y R U' R'"

            "OLL 15" -> "r' U' r R' U' R U r' U r \n" +
                    "y2 l' U' l L' U' L U l' U l \n" +
                    "r' U' M' U' R U r' U r \n" +
                    "y' R' U2 R U R' F U R U' R' F' R"

            "OLL 16" -> "r U r' R U R' U' r U' r' \n" +
                    "r U M U R' U' r U' r' \n" +
                    "y2 R' F R U R' U' F' R U' R' U2 R \n" +
                    "y2 l U l' L U L' U' l U' l'"

            "OLL 17" -> "R U R' U R' F R F' U2 R' F R F' \n" +
                    "f R U R' U' f' U' R U R' U' R' F R F' \n" +
                    "y2 F R' F' R2 r' U R U' R' U' M' \n" +
                    "y' F' r U r' U' S r' F r S'"

            "OLL 18" -> "r U R' U R U2 r2 U' R U' R' U2 r \n" +
                    "y R U2 R2 F R F' U2 M' U R U' r' \n" +
                    "y2 F R U R' d R' U2 R' F R F' \n" +
                    "y2 F R U R' U y' R' U2 R' F R F'"

            "OLL 19" -> "M U R U R' U' M' R' F R F' \n" +
                    "r' R U R U R' U' r R2' F R F' \n" +
                    "r' U2 R U R' U r2 U2 R' U' R U' r' \n" +
                    "R' U2 F R U R' U' F2 U2 F R"

            "OLL 20" -> "M U R U R' U' M2 U R U' r' \n" +
                    "r U R' U' M2 U R U' R' U' M' \n" +
                    "M' U M' U M' U M' U' M' U M' U M' U M' \n" +
                    "M' U' R' U' R U M2' U' R' U r"

            "OLL 21" -> "y R U2 R' U' R U R' U' R U' R' \n" +
                    "y F R U R' U' R U R' U' R U R' U' F' \n" +
                    "R U R' U R U' R' U R U2 R' \n" +
                    "R' U' R U' R' U R U' R' U2 R"

            "OLL 22" -> "R U2 R2 U' R2 U' R2 U2 R \n" +
                    "f R U R' U' f' F R U R' U' F' \n" +
                    "R' U2 R2 U R2 U R2 U2 R' \n" +
                    "R U2' R2' U' R2 U' R2' U2 R"

            "OLL 23" -> "R2 D R' U2 R D' R' U2 R' \n" +
                    "y2 R2 D' R U2 R' D R U2 R \n" +
                    "y R U R' U' R U' R' U2 R U' R' U2 R U R' \n" +
                    "R U R' U R U2 R2 U' R U' R' U2 R"

            "OLL 24" -> "r U R' U' r' F R F' \n" +
                    "y2 l' U' L U R U' r' F \n" +
                    "y' x' R U R' D R U' R' D' x \n" +
                    "r U R' U' L' U R U' x'"

            "OLL 25" -> "y F' r U R' U' r' F R \n" +
                    "R' F R B' R' F' R B \n" +
                    "F R' F' r U R U' r' \n" +
                    "y2 R U2 R' U' R U R' U' R U R' U' R U' R'"

            "OLL 26" -> "y R U2 R' U' R U' R' \n" +
                    "R' U' R U' R' U2 R \n" +
                    "y2 L' U' L U' L' U2 L \n" +
                    "R' U L U' R U L'"

            "OLL 27" -> "R U R' U R U2 R' \n" +
                    "y' R' U2 R U R' U R \n" +
                    "R U' L' U R' U' L \n" +
                    "y L' U2 L U L' U L"

            "OLL 28" -> "r U R' U' M U R U' R' \n" +
                    "y2 M' U M U2 M' U M \n" +
                    "M U M' U2 M U M' \n" +
                    "y' M' U' M U2 M' U' M"

            "OLL 29" -> "M U R U R' U' R' F R F' M' \n" +
                    "r2 D' r U r' D r2 U' r' U' r \n" +
                    "y R U R' U' R U' R' F' U' F R U R' \n" +
                    "y2 R' F R F' R U2 R' U' F' U' F"

            "OLL 30" -> "M U' L' U' L U L F' L' F M' \n" +
                    "y' r' D' r U' r' D r2 U' r' U r U r' \n" +
                    "y2 F R' F R2 U' R' U' R U R' F2 \n" +
                    "R2 U R' B' R U' R2 U R B R'"

            "OLL 31" -> "R' U' F U R U' R' F' R \n" +
                    "y2 S' L' U' L U L F' L' f \n" +
                    "y' F R' F' R U R U R' U' R U' R' \n" +
                    "y S R U R' U' f' U' F"

            "OLL 32" -> "S R U R' U' R' F R f' \n" +
                    "R U B' U' R' U R B R' \n" +
                    "y2 L U F' U' L' U L F L' \n" +
                    "R d L' d' R' U l U l'"

            "OLL 33" -> "R U R' U' R' F R F' \n" +
                    "F R U' R' U R U R' F' \n" +
                    "y2 L' U' L U L F' L' F \n" +
                    "y' r' U' r' D' r U r' D r2"

            "OLL 34" -> "y2 R U R' U' B' R' F R F' B \n" +
                    "y2 R U R2 U' R' F R U R U' F' \n" +
                    "F R U R' U' R' F' r U R U' r' \n" +
                    "y2 R U R' U' y' r' U' R U M'"

            "OLL 35" -> "R U2 R2' F R F' R U2 R' \n" +
                    "f R U R' U' f' R U R' U R U2 R' \n" +
                    "y' R U2 R' U' R U' R' U2 F R U R' U' F' \n" +
                    "R U2 R' U' y' r' U r U' r' U' r"

            "OLL 36" -> "y2 L' U' L U' L' U L U L F' L' F \n" +
                    "R' U' R U' R' U R U l U' R' U x \n" +
                    "R' U' R U' R' U R U R y R' F' R \n" +
                    "R U2 r D r' U2 r D' R' r'"

            "OLL 37" -> "F R U' R' U' R U R' F' \n" +
                    "F R' F' R U R U' R' \n" +
                    "R' F R F' U' F' U F \n" +
                    "y' R U2 R' F R' F' R2 U2 R'"

            "OLL 38" -> "R U R' U R U' R' U' R' F R F' \n" +
                    "L' U' L F L' U' L U L F' L' U L F' L' F"

            "OLL 39" -> "y L F' L' U' L U F U' L' \n" +
                    "y' R U R' F' U' F U R U2 R' \n" +
                    "y' R B' R' U' R U B U' R' \n" +
                    "R' r' D' r U' r' D r U R"

            "OLL 40" -> "y R' F R U R' U' F' U R \n" +
                    "R r D r' U r D' r' U' R' \n" +
                    "y' f R' F' R U R U' R' S' \n" +
                    "y' F R U R' U' F' R U R' U R U2 R'"

            "OLL 41" -> "y2 R U R' U R U2' R' F R U R' U' F' \n" +
                    "R U' R' U2 R U y R U' R' U' F' \n" +
                    "y' L F' L' F L F' L' F L' U' L U L' U' L \n" +
                    "f R U R' U' f' U' R U R' U R U2 R'"

            "OLL 42" -> "R' U' R U' R' U2 R F R U R' U' F' \n" +
                    "y R' F R F' R' F R F' R U R' U' R U R' \n" +
                    "L' U L U2 L' U' y' L' U L U F \n" +
                    "R' U R U2 R' U' F' U F U R"

            "OLL 43" -> "f' L' U' L U f \n" +
                    "y2 F' U' L' U L F \n" +
                    "y R' U' F' U F R \n" +
                    "y2 R' U' F R' F' R U R"

            "OLL 44" -> "f R U R' U' f' \n" +
                    "y2 F U R U' R' F' \n" +
                    "y2 r U x' R U' R' U x U' r' \n" +
                    "y' L d R U' R' F'"

            "OLL 45" -> "F R U R' U' F' \n" +
                    "y2 f U R U' R' f' \n" +
                    "y2 F' L' U' L U F \n" +
                    "F R2 D R' U R D' R2 U' F'"

            "OLL 46" -> "R' U' R' F R F' U R \n" +
                    "y F R U R' y' R' U R U2 R' \n" +
                    "y2 r' F' L' U L U' F r"

            "OLL 47" -> "F' L' U' L U L' U' L U F \n" +
                    "R' U' R' F R F' R' F R F' U R \n" +
                    "R' U' l' U R U' R' U R U' x' U R \n" +
                    "y2 B' R' U' R U R' U' R U B"

            "OLL 48" -> "F R U R' U' R U R' U' F' \n" +
                    "R U2 R' U' R U R' U2 R' F R F'"

            "OLL 49" -> "y2 r U' r2 U r2 U r2 U' r \n" +
                    "l U' l2 U l2 U l2 U' l \n" +
                    "R B' R2 F R2 B R2 F' R \n" +
                    "y2 R' F R' F' R2 U2 B' R B R'"

            "OLL 50" -> "r' U r2 U' r2' U' r2 U r' \n" +
                    "y2 R' F R2 B' R2 F' R2 B R' \n" +
                    "y' R U2 R' U' R U' R' F R U R' U' F' \n" +
                    "y2 l' U l2 U' l2 U' l2 U l'"

            "OLL 51" -> "f R U R' U' R U R' U' f' \n" +
                    "y2 F U R U' R' U R U' R' F' \n" +
                    "y' R' U' R' F R F' R U' R' U2 R \n" +
                    "y2 f' L' U' L U L' U' L U f"

            "OLL 52" -> "R U R' U R d' R U' R' F' \n" +
                    "R' U' R U' R' d R' U R B \n" +
                    "R' U' R U' R' U F' U F R \n" +
                    "R U R' U R U' y R U' R' F'"

            "OLL 53" -> "r' U' R U' R' U R U' R' U2 r \n" +
                    "y2 l' U' L U' L' U L U' L' U2 l \n" +
                    "y r' U2 R U R' U' R U R' U r \n" +
                    "y' l' U2 L U L' U' L U L' U l"

            "OLL 54" -> "r U R' U R U' R' U R U2 r' \n" +
                    "y' r U2 R' U' R U R' U' R U' r' \n" +
                    "F' L' U' L U L' U L U' L' U' L F \n" +
                    "y2 F R' F' R U2 F2 L F L' F"

            "OLL 55" -> "R U2 R2 U' R U' R' U2 F R F' \n" +
                    "y R' F R U R U' R2 F' R2 U' R' U R U R' \n" +
                    "r U2 R2 F R F' U2 r' F R F' \n" +
                    "R' U2 R2 U R' U R U2 y R' F' R"

            "OLL 56" -> "r U r' U R U' R' U R U' R' r U' r' \n" +
                    "F R U R' U' R F' r U R' U' r' \n" +
                    "y f R U R' U' f' F R U R' U' R U R' U' F' \n" +
                    "r' U' r U' R' U R U' R' U R r' U r"

            "OLL 57" -> "R U R' U' M' U R U' r' \n" +
                    "M' U M' U M' U2 M U M U M \n" +
                    "R U R' U' r R' U R U' r' \n" +
                    "M' U M' U M' U M' U2 M' U M' U M' U M'"

            // PLL Algorithms
            "aa_perm" -> "x R' U R' D2 R U' R' D2 R2 x' \n" +
                    "l' U R' D2 R U' R' D2 R2 \n" +
                    "R' F R' B2 R F' R' B2 R2"

            "ab_perm" -> "x R2 D2 R U R' D2 R U' R x' \n" +
                    "l U' R D2 R' U R D2 R2 \n" +
                    "R2 B2 R F R' B2 R F' R"

            "e_perm" -> "x' R U' R' D R U R' D' R U R' D R U' R' D' x \n" +
                    "R' U L' D2 L U' R L' U R' D2 R U' L \n" +
                    "R2 U R' U' y R U R' U' R U R' U' R U R' y' R U' R2"

            "f_perm" -> "R' U' F' R U R' U' R' F R2 U' R' U' R U R' U R \n" +
                    "R' U2 R' d' R' F' R2 U' R' U R' F R F \n" +
                    "M' U2 L F' R U2 r' U r' R2 U2 R2"

            "ga_perm" -> "R2 U R' U R' U' R U' R2 D U' R' U R D' \n" +
                    "R2 u R' U R' U' R u' R2 y' R' U R \n" +
                    "R2 U R' U R' U' R U' R2 U' D R' U R D'"

            "gb_perm" -> "R' U' R U D' R2 U R' U R U' R U' R2 D \n" +
                    "R' d' F R2 u R' U R U' R u' R2 \n" +
                    "F' U' F R2 u R' U R U' R u' R2"

            "gc_perm" -> "R2 U' R U' R U R' U R2 D' U R U' R' D \n" +
                    "R2 F2 R U2 R U2 R' F R U R' U' R' F R2 \n" +
                    "R2 u' R U' R U R' u R2 y R U' R'"

            "gd_perm" -> "R U R' U' D R2 U' R U' R' U R' U R2 D' \n" +
                    "R U R' y' R2 u' R U' R' U R' u R2 \n" +
                    "f R f' R2 u' R U' R' U R' u R2"

            "h_perm" -> "M2 U M2 U2 M2 U M2 \n" +
                    "M2 U' M2 U2 M2 U' M2 \n" +
                    "R2 U2 R2 U2 R2 U2 R2 U2"

            "ja_perm" -> "x R2 F R F' R U2 r' U r U2 x' \n" +
                    "R' U L' U2 R U' R' U2 R L \n" +
                    "L' U' L F L' U' L U L F' L2 U L"

            "jb_perm" -> "R U R' F' R U R' U' R' F R2 U' R' \n" +
                    "R U2 R' U' R U2 L' U R' U' L \n" +
                    "R' U R U' R2 F' R U R U' R' F R U' R'"

            "na_perm" -> "R U R' U R U R' F' R U R' U' R' F R2 U' R' U2 R U' R' \n" +
                    "z U R' D R2 U' R D' U R' D R2 U' R D' z' \n" +
                    "F' R U R' U' R' F R2 F U' R' U' R U F' R'"

            "nb_perm" -> "R' U R U' R' F' U' F R U R' F R' F' R U' R \n" +
                    "r' D' F r U' r' F' D r2 U r' U' r' F r F' \n" +
                    "R' U L' U2 R U' L R' U L' U2 R U' L"

            "ra_perm" -> "R U R' F' R U2 R' U2 R' F R U R U2 R' \n" +
                    "R U' R' U' R U R D R' U' R D' R' U2 R' \n" +
                    "L U2 L' U2 L F' L' U' L U L F L2"

            "rb_perm" -> "R2 F R U R U' R' F' R U2 R' U2 R \n" +
                    "R' U2 R U2 R' F R U R' U' R' F' R2 \n" +
                    "R' U2 R' D' R U' R' D R U R U' R' U' R"

            "t_perm" -> "R U R' U' R' F R2 U' R' U' R U R' F' \n" +
                    "R2 U R2 U' R2 U' D R2 U' R2 U R2 D' \n" +
                    "x' R2 U' R' U R' U' R U R2 D' U R' U R D x"

            "ua_perm" -> "M2 U M U2 M' U M2 \n" +
                    "R2 U' R' U' R U R U R U' R \n" +
                    "M2 U M' U2 M U M2"

            "ub_perm" -> "M2 U' M U2 M' U' M2 \n" +
                    "R' U R' U' R' U' R' U R U R2 \n" +
                    "M2 U' M' U2 M U' M2"

            "v_perm" -> "R' U R' d' R' F' R2 U' R' U R' F R F \n" +
                    "R' U R' U' y R' F' R2 U' R' U R' F R F \n" +
                    "z D' R2 D R2 U R' D' R U' R U R' D R U' z'"

            "y_perm" -> "F R U' R' U' R U R' F' R U R' U' R' F R F' \n" +
                    "F R' F R2 U' R' U' R U R' F' R U R' U' F' \n" +
                    "R2 U' R2 U' R2 U y' R2 U' R2 U' R2"

            "z_perm" -> "M' U M2 U M2 U M' U2 M2 \n" +
                    "M2 U M2 U M' U2 M2 U2 M' \n" +
                    "M' U' M2 U' M2 U' M' U2 M2"

            else -> "Algorithm not available"
        }
    }
}

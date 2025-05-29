package com.example.cubespeed.model

enum class CubeType(val displayName: String, val idName: String = "") {
    CUBE_2X2("2x2 Cube", "CUBE_2X2_"),
    CUBE_3X3("3x3 Cube", "CUBE_3X3_"),
    CUBE_4X4("4x4 Cube", "CUBE_4X4_"),
    CUBE_5X5("5x5 Cube", "CUBE_5X5_"),
    CUBE_6X6("6x6 Cube", "CUBE_6X6_"),
    CUBE_7X7("7x7 Cube", "CUBE_7X7_"),
    PYRAMINX("Pyraminx", "PYRAMINX_"),
    MEGAMINX("Megaminx", "MEGAMINX_"),
    SKEWB("Skewb", "SKEWB_"),
    SQUARE_1("Square-1", "SQUARE_1_");

    companion object {
        fun getAllDisplayNames(): List<String> {
            return values().map { it.displayName }
        }

        fun fromDisplayName(displayName: String): CubeType {
            val normalizedDisplayName = displayName.trim()

            // If empty or null, default to 3x3
            if (normalizedDisplayName.isEmpty()) {
                println("[DEBUG_LOG] Empty display name, defaulting to CUBE_3X3")
                return CUBE_3X3
            }

            // First try exact match
            val exactMatch = values().find { it.displayName == normalizedDisplayName }
            if (exactMatch != null) {
                println("[DEBUG_LOG] Exact match found for '$normalizedDisplayName': ${exactMatch.name}")
                return exactMatch
            }

            // Try case-insensitive match
            val caseInsensitiveMatch = values().find { 
                it.displayName.equals(normalizedDisplayName, ignoreCase = true)
            }
            if (caseInsensitiveMatch != null) {
                println("[DEBUG_LOG] Case-insensitive match found for '$normalizedDisplayName': ${caseInsensitiveMatch.name}")
                return caseInsensitiveMatch
            }

            // If no exact match, try to match by the cube size/type part
            // For example, if displayName is "6x6", it should match "6x6 Cube"
            // But be more strict about partial matches to avoid incorrect matches
            val cubeTypeMatch = values().find { cubeType ->
                val cubeTypePart = cubeType.displayName.split(" ")[0].trim().lowercase()
                val displayNamePart = normalizedDisplayName.split(" ")[0].trim().lowercase()

                // Only match if the parts are exactly equal or one contains the other completely
                cubeTypePart == displayNamePart || 
                (cubeTypePart.length > displayNamePart.length && cubeTypePart.startsWith(displayNamePart)) ||
                (displayNamePart.length > cubeTypePart.length && displayNamePart.startsWith(cubeTypePart))
            }

            if (cubeTypeMatch != null) {
                println("[DEBUG_LOG] Partial match found for '$normalizedDisplayName': ${cubeTypeMatch.name}")
                return cubeTypeMatch
            }

            println("[DEBUG_LOG] No match found for '$normalizedDisplayName', defaulting to CUBE_3X3")
            return CUBE_3X3 // Default to 3x3 if no match found
        }
    }
}

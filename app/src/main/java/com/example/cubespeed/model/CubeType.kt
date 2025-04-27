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
            return values().find { it.displayName == displayName } 
                ?: throw IllegalArgumentException("No cube type found for display name: $displayName")
        }
    }
}

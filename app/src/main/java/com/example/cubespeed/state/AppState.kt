package com.example.cubespeed.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cubespeed.model.CubeType

/**
 * Singleton object to store and share application state across screens
 */
object AppState {
    // Default values
    var selectedCubeType by mutableStateOf(CubeType.CUBE_3X3.displayName)
    var selectedTag by mutableStateOf("normal")
}
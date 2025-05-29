package com.example.cubespeed.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
    var commentText by mutableStateOf("")

    // Trigger to refresh history screen when a new solve is added
    var historyRefreshTrigger by mutableLongStateOf(0L)

    // SharedPreferences reference
    private var sharedPreferences: SharedPreferences? = null

    /**
     * Initialize AppState with SharedPreferences
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("cubespeed_settings", 0)

        // Load saved values
        sharedPreferences?.let {
            val savedCubeType = it.getString("selected_cube_type", CubeType.CUBE_3X3.displayName)
            val savedTag = it.getString("selected_tag", "normal")

            // Update the state values
            selectedCubeType = savedCubeType ?: CubeType.CUBE_3X3.displayName
            selectedTag = savedTag ?: "normal"
        }
    }

    /**
     * Update the cube type and save it to SharedPreferences
     */
    fun updateCubeType(cubeType: String) {
        selectedCubeType = cubeType
        saveToPreferences()
    }

    /**
     * Update the tag and save it to SharedPreferences
     */
    fun updateTag(tag: String) {
        selectedTag = tag
        saveToPreferences()
    }

    /**
     * Save current state to SharedPreferences
     */
    private fun saveToPreferences() {
        sharedPreferences?.let {
            with(it.edit()) {
                putString("selected_cube_type", selectedCubeType)
                putString("selected_tag", selectedTag)
                apply()
            }
        }
    }
}

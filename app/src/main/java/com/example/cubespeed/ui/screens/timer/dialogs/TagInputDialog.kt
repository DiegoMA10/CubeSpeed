package com.example.cubespeed.ui.screens.timer.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.theme.dialogButtonTextColor
import com.example.cubespeed.ui.theme.isAppInLightTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Function to load tags from Firestore
 */
suspend fun loadTags(onTagsLoaded: (List<String>) -> Unit) {
    val userId = Firebase.auth.currentUser?.uid
    if (userId != null) {
        try {
            val tagsSnapshot = Firebase.firestore.collection("users")
                .document(userId)
                .collection("tags")
                .get()
                .await()

            val loadedTags = tagsSnapshot.documents.mapNotNull { it.getString("name") }
            // Add the default "normal" tag to the list if it's not already included
            val allTags = if (!loadedTags.contains("normal")) {
                listOf("normal") + loadedTags
            } else {
                loadedTags
            }
            onTagsLoaded(allTags)
        } catch (e: Exception) {
            // Handle error - include at least the default tag
            onTagsLoaded(listOf("normal"))
        }
    } else {
        // Return at least the default tag
        onTagsLoaded(listOf("normal"))
    }
}

/**
 * A dialog for selecting or creating a tag.
 *
 * @param currentTag The currently selected tag
 * @param onTagConfirmed Callback for when a tag is selected or created
 * @param onDismiss Callback for when the dialog is dismissed
 */
@Composable
fun TagInputDialog(
    currentTag: String,
    onTagConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // State for the new tag input
    var newTagInput by remember { mutableStateOf("") }

    // State for the list of tags
    var tags by remember { mutableStateOf(listOf<String>()) }

    // State for the selected tag
    var selectedTag by remember { mutableStateOf(currentTag) }

    // State for showing the add tag input
    var showAddTagInput by remember { mutableStateOf(false) }

    // State for showing the delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // State for the tag to delete
    var tagToDelete by remember { mutableStateOf("") }

    // State for error message when adding a tag
    var tagError by remember { mutableStateOf("") }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Create an instance of FirebaseRepository
    val firebaseRepository = remember { FirebaseRepository() }

    // Get the current configuration to determine screen orientation
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // More robust landscape detection using both orientation and screen dimensions
    val orientationBasedLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val dimensionBasedLandscape = screenWidth > screenHeight
    val isLandscape = orientationBasedLandscape || dimensionBasedLandscape

    // Debug logging for orientation detection
    println("[DEBUG_LOG] Screen dimensions: ${screenWidth}x${screenHeight}")
    println("[DEBUG_LOG] Orientation-based landscape: $orientationBasedLandscape")
    println("[DEBUG_LOG] Dimension-based landscape: $dimensionBasedLandscape")
    println("[DEBUG_LOG] Final isLandscape: $isLandscape")

    // Effect to load tags from Firestore
    LaunchedEffect(Unit) {
        loadTags { loadedTags ->
            tags = loadedTags
            // Debug log to verify tags are loaded
            println("[DEBUG_LOG] Loaded tags: $loadedTags")
        }
    }

    // Function to add a tag to Firestore
    fun addTag(tag: String, onTagAdded: () -> Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null && tag.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    Firebase.firestore.collection("users")
                        .document(userId)
                        .collection("tags")
                        .add(mapOf("name" to tag))
                        .await()

                    // Reload tags
                    loadTags { loadedTags ->
                        tags = loadedTags
                        onTagAdded()
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    // Function to delete a tag from Firestore
    fun deleteTag(tag: String, onTagDeleted: () -> Unit) {
        if (tag.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    // Use FirebaseRepository to remove the tag and all associated solves and statistics
                    val success = firebaseRepository.removeTag(tag)

                    if (success) {
                        // Reload tags
                        loadTags { loadedTags ->
                            tags = loadedTags
                            onTagDeleted()
                        }
                    } else {
                        // Handle error - tag removal failed
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isAppInLightTheme) Color.White else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isAppInLightTheme) 4.dp else 0.dp,
            shadowElevation = if (isAppInLightTheme) 4.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth(1f) // Using full width in both orientations for better visibility
                .padding(16.dp)
                .heightIn(max = if (isLandscape) 400.dp else 600.dp) // Adjust maximum height based on orientation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Header
                    Text(
                        text = "Select Tag",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Scrollable content with fixed height to ensure buttons remain visible
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 150.dp else 350.dp) // Increased height in portrait mode for better tag visibility
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight() // Fill the Box height
                        ) {
                            // Show add tag input if requested
                            if (showAddTagInput) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newTagInput,
                                        onValueChange = { 
                                            newTagInput = it
                                            // Clear error when user types
                                            if (tagError.isNotEmpty()) {
                                                tagError = ""
                                            }
                                        },
                                        label = { Text("New Tag") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        isError = tagError.isNotEmpty()
                                    )

                                    IconButton(
                                        onClick = {
                                            val trimmedInput = newTagInput.trim()
                                            if (trimmedInput.isNotEmpty()) {
                                                // Check if any existing tag matches the trimmed input (case-insensitive)
                                                val tagExists = tags.any { it.trim().equals(trimmedInput, ignoreCase = true) }
                                                if (!tagExists) {
                                                    addTag(trimmedInput) {
                                                        selectedTag = trimmedInput
                                                        newTagInput = ""
                                                        tagError = "" // Clear any previous error
                                                        // Return to the initial "Add Tag" state
                                                        showAddTagInput = false
                                                    }
                                                } else {
                                                    // Show error for duplicate tag
                                                    tagError = "Tag already exists"
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Tag",
                                            tint = if (isAppInLightTheme) Color.Gray else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Display error message if there is one
                                if (tagError.isNotEmpty()) {
                                    Text(
                                        text = tagError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                                    )
                                }
                            }

                            // List of tags - make it scrollable within the fixed height container
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight() // Fill available height in the Box for proper scrolling
                            ) {
                                items(tags) { tag ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedTag = tag
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Row {
                                            // Show a checkmark for the currently selected tag
                                            if (tag == selectedTag) {
                                                Box(
                                                    modifier = Modifier.size(48.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = if (isAppInLightTheme) Color.Gray else MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            // Delete icon (hide for "normal" tag)
                                            if (tag != "normal") {
                                                IconButton(
                                                    onClick = {
                                                        tagToDelete = tag
                                                        showDeleteConfirmation = true
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Tag",
                                                        tint = if (isAppInLightTheme) Color.Gray else MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Add a divider between items
                                    if (tag != tags.lastOrNull()) {
                                        Divider(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Buttons layout that adapts to orientation
                    if (isLandscape) {
                        // In landscape mode, use a row layout similar to portrait mode but with adjusted spacing
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Add tag button
                            TextButton(
                                onClick = {
                                    showAddTagInput = !showAddTagInput
                                    if (!showAddTagInput) {
                                        newTagInput = ""
                                    }
                                }
                            ) {
                                Text(
                                    if (showAddTagInput) "Cancel" else "Add Tag", 
                                    color = dialogButtonTextColor,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            // Action buttons
                            Row {
                                TextButton(
                                    onClick = onDismiss
                                ) {
                                    Text(
                                        "Cancel", 
                                        color = dialogButtonTextColor,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                TextButton(
                                    onClick = {
                                        onTagConfirmed(selectedTag)
                                        AppState.updateTag(selectedTag)
                                    }
                                ) {
                                    Text(
                                        "Confirm", 
                                        color = dialogButtonTextColor,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // In portrait mode, keep the original layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Add tag button
                            TextButton(
                                onClick = {
                                    showAddTagInput = !showAddTagInput
                                    if (!showAddTagInput) {
                                        newTagInput = ""
                                    }
                                }
                            ) {
                                Text(if (showAddTagInput) "Cancel" else "Add Tag", color = dialogButtonTextColor)
                            }

                            // Action buttons
                            Row {
                                TextButton(
                                    onClick = onDismiss
                                ) {
                                    Text("Cancel", color = dialogButtonTextColor)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = {
                                        onTagConfirmed(selectedTag)
                                        AppState.updateTag(selectedTag)
                                    }
                                ) {
                                    Text("Confirm", color = dialogButtonTextColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Tag") },
            text = { Text("Are you sure you want to delete the tag \"$tagToDelete\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTag(tagToDelete) {
                            // If the deleted tag was selected, select the first available tag or "normal"
                            // and automatically confirm the selection
                            if (selectedTag == tagToDelete) {
                                val newTag = "normal" // Default to "normal" tag
                                selectedTag = newTag
                                // Automatically confirm the selection
                                onTagConfirmed(newTag)
                                AppState.updateTag(newTag)
                                onDismiss() // Close the dialog
                            }
                        }
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = dialogButtonTextColor)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel", color = dialogButtonTextColor)
                }
            }
        )
    }
}

package com.example.cubespeed.repository

import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Data class to hold statistics for a specific cube type and tag
 */
data class SolveStatistics(
    val count: Int = 0,
    val validCount: Int = 0,
    val best: Long = 0,
    val average: Double = 0.0,
    val deviation: Double = 0.0,
    val ao5: Double = 0.0,
    val ao12: Double = 0.0,
    val ao50: Double = 0.0,
    val ao100: Double = 0.0
)

class FirebaseRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions

    // Default tags that are always available
    private val defaultTags = listOf("normal")

    /**
     * Saves a solve to Firebase Firestore
     * Structure: users/{uid}/timers/{solveId}
     * @return The ID of the saved solve, or an empty string if the save failed
     */
    suspend fun saveSolve(solve: Solve): String {
        val currentUser = auth.currentUser ?: return ""

        try {
            // Create a map with the solve data
            val solveData = hashMapOf(
                "cube" to solve.cube.name,
                "tagId" to solve.tagId,
                "timestamp" to solve.timestamp,
                "duration" to solve.time,
                "status" to solve.status.name,
                "scramble" to solve.scramble,
                "comment" to solve.comments
            )

            // Save to Firestore
            val solveRef = if (solve.id.isNotEmpty()) {
                // Update existing solve
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("timers")
                    .document(solve.id)
            } else {
                // Create new solve
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("timers")
                    .document()
            }

            solveRef.set(solveData).await()

            // Update stats
            updateStats(currentUser.uid, solve.cube, solve.tagId)

            // Return the ID of the saved solve
            return solveRef.id
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error saving solve: ${e.message}", e)
            return ""
        }
    }

    /**
     * Updates the statistics for a specific cube and tag
     * 
     * Note: Statistics are calculated by Firebase Functions written in Node.js.
     * The functions are triggered automatically when a solve is added, updated, or deleted.
     * See the 'functions/index.js' file for the implementation.
     * 
     * The app observes the stats document in Firestore for real-time updates
     * using snapshot listeners in the UI components.
     * 
     * This method is a no-op as the statistics are calculated automatically.
     */
    private suspend fun updateStats(userId: String, cubeType: CubeType, tagId: String) {
        // No action needed here as the function is triggered automatically
    }

    /**
     * Gets statistics for a specific cube type and tag
     * Returns a SolveStatistics object with the statistics
     * 
     * Note: This method simply fetches the current value from Firestore.
     * For real-time updates, UI components should set up their own snapshot listeners
     * on the stats document, as demonstrated in the StatisticsComponent.
     */
    suspend fun getStats(cubeType: CubeType, tagId: String): SolveStatistics {
        val currentUser = auth.currentUser ?: return SolveStatistics()

        try {
            // Get statistics from Firestore
            val statsRef = firestore.collection("users")
                .document(currentUser.uid)
                .collection("stats")
                .document("${cubeType.name}_$tagId")

            val statsDoc = statsRef.get().await()

            if (statsDoc.exists()) {
                // Convert Firestore document to SolveStatistics
                return SolveStatistics(
                    count = statsDoc.getLong("count")?.toInt() ?: 0,
                    validCount = statsDoc.getLong("validCount")?.toInt() ?: 0,
                    best = statsDoc.getLong("best") ?: 0,
                    average = statsDoc.getDouble("average") ?: 0.0,
                    deviation = statsDoc.getDouble("deviation") ?: 0.0,
                    ao5 = statsDoc.getDouble("ao5") ?: 0.0,
                    ao12 = statsDoc.getDouble("ao12") ?: 0.0,
                    ao50 = statsDoc.getDouble("ao50") ?: 0.0,
                    ao100 = statsDoc.getDouble("ao100") ?: 0.0
                )
            }

            // If no statistics found, return empty statistics
            return SolveStatistics()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting stats for ${cubeType.name}_$tagId: ${e.message}", e)
            return SolveStatistics()
        }
    }

    /**
     * Gets all tags for the current user
     * Returns a list of tags including default tags and user-defined tags
     */
    suspend fun getTags(): List<String> {
        val currentUser = auth.currentUser ?: return defaultTags

        try {
            // Get user-defined tags from Firestore
            val tagsRef = firestore.collection("users")
                .document(currentUser.uid)
                .collection("tags")

            val tagsSnapshot = tagsRef.get().await()

            // Convert documents to tag strings
            val userTags = tagsSnapshot.documents.mapNotNull { it.getString("name") }

            // Combine default tags with user tags, ensuring no duplicates
            return (defaultTags + userTags).distinct()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting tags: ${e.message}", e)
            // If there's an error, return just the default tags
            return defaultTags
        }
    }

    /**
     * Adds a new tag for the current user
     * Returns true if successful, false otherwise
     */
    suspend fun addTag(tagName: String): Boolean {
        // Don't add if it's already a default tag
        if (defaultTags.contains(tagName)) return true

        val currentUser = auth.currentUser ?: return false

        try {
            // Check if tag already exists
            val existingTags = getTags()
            if (existingTags.contains(tagName)) return true

            // Add new tag to Firestore
            val tagData = hashMapOf("name" to tagName)

            firestore.collection("users")
                .document(currentUser.uid)
                .collection("tags")
                .document() // Auto-generate ID
                .set(tagData)
                .await()

            return true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error adding tag '$tagName': ${e.message}", e)
            return false
        }
    }

    /**
     * Deletes a solve from Firebase Firestore
     * @param solveId The ID of the solve to delete
     * @return True if the solve was deleted successfully, false otherwise
     */
    suspend fun deleteSolve(solveId: String): Boolean {
        val currentUser = auth.currentUser ?: return false

        try {
            // Get the solve to determine its cube type and tag for stats update
            val solveRef = firestore.collection("users")
                .document(currentUser.uid)
                .collection("timers")
                .document(solveId)

            val solveDoc = solveRef.get().await()

            if (!solveDoc.exists()) {
                Log.w("FirebaseRepository", "Solve $solveId not found for deletion")
                return false
            }

            // Extract cube type and tag ID for stats update
            val cubeType = solveDoc.getString("cube")?.let { CubeType.valueOf(it) } ?: return false
            val tagId = solveDoc.getString("tagId") ?: return false

            // Delete the solve
            solveRef.delete().await()

            // Update stats
            updateStats(currentUser.uid, cubeType, tagId)

            // Check if this was the last solve for this cube type and tag
            val remainingSolves = countSolvesByCubeTypeAndTag(cubeType, tagId)
            if (remainingSolves == 0) {
                // If no solves remain, delete the stats document
                val statsDocId = "${cubeType.name}_${tagId}"
                val statsRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("stats")
                    .document(statsDocId)

                // Check if the stats document exists before deleting
                val statsDoc = statsRef.get().await()
                if (statsDoc.exists()) {
                    Log.d("FirebaseRepository", "Deleting stats document: $statsDocId")
                    statsRef.delete().await()
                }
            }

            return true
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting solve $solveId: ${e.message}", e)
            return false
        }
    }

    /**
     * Counts the number of solves for a specific cube type and tag
     * @param cubeType The cube type to count solves for
     * @param tagId The tag ID to count solves for
     * @return The number of solves for the specified cube type and tag
     */
    suspend fun countSolvesByCubeTypeAndTag(cubeType: CubeType, tagId: String): Int {
        val currentUser = auth.currentUser ?: return 0

        return try {
            val query = firestore.collection("users")
                .document(currentUser.uid)
                .collection("timers")
                .whereEqualTo("cube", cubeType.name)
                .whereEqualTo("tagId", tagId)

            val countResult = query.count().get(AggregateSource.SERVER).await()
            countResult.count.toInt()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error counting solves for ${cubeType.name}_$tagId: ${e.message}", e)
            0
        }
    }

    /**
     * Removes a tag for the current user and all timers and stats associated with it
     * Returns true if successful, false otherwise
     * Note: Default tags cannot be removed
     */
    suspend fun removeTag(tagName: String): Boolean {
        // Don't remove default tags
        if (defaultTags.contains(tagName)) {
            Log.w("FirebaseRepository", "Cannot remove default tag: $tagName")
            return false
        }

        val currentUser = auth.currentUser ?: return false

        try {
            // Find the tag document
            val tagsRef = firestore.collection("users")
                .document(currentUser.uid)
                .collection("tags")
                .whereEqualTo("name", tagName)

            val querySnapshot = tagsRef.get().await()

            // If tag found, delete it and all associated timers and stats
            if (!querySnapshot.isEmpty) {
                // First, find all timers with this tag
                val timersRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("timers")
                    .whereEqualTo("tagId", tagName)

                val timersSnapshot = timersRef.get().await()
                Log.d("FirebaseRepository", "Deleting ${timersSnapshot.size()} solves with tag: $tagName")

                // Delete timers in batches of 500 (Firestore batch limit)
                timersSnapshot.documents.chunked(500).forEach { batch ->
                    val writeBatch = firestore.batch()
                    batch.forEach { timerDoc ->
                        writeBatch.delete(timerDoc.reference)
                    }
                    writeBatch.commit().await()
                }

                // Find and delete all stats documents for this tag
                // Stats documents have IDs in the format "{cubeType}_{tagId}"
                val statsRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("stats")

                val statsSnapshot = statsRef.get().await()

                // Filter stats documents that have IDs ending with "_{tagId}"
                val statsToDelete = statsSnapshot.documents.filter { doc ->
                    doc.id.endsWith("_$tagName")
                }
                Log.d("FirebaseRepository", "Deleting ${statsToDelete.size} stats documents for tag: $tagName")

                // Delete stats documents in batches of 500
                statsToDelete.chunked(500).forEach { batch ->
                    val writeBatch = firestore.batch()
                    batch.forEach { statsDoc ->
                        writeBatch.delete(statsDoc.reference)
                    }
                    writeBatch.commit().await()
                }

                // Then delete the tag itself
                val tagBatch = firestore.batch()
                querySnapshot.documents.forEach { doc ->
                    tagBatch.delete(doc.reference)
                }
                tagBatch.commit().await()

                return true
            }

            Log.w("FirebaseRepository", "Tag not found for removal: $tagName")
            return false
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error removing tag $tagName: ${e.message}", e)
            return false
        }
    }
}

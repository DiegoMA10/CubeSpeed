package com.example.cubespeed.repository

import android.util.Log
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.model.SolveStatistics
import com.example.cubespeed.ui.screens.history.enums.SortOrder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions

    // Default tags that are always available
    private val defaultTags = listOf("normal")

    // Maximum batch size for Firestore batch operations
    private val MAX_BATCH_SIZE = 500

    /**
     * Saves a solve to Firebase Firestore
     * Structure: users/{uid}/solves/{solveId}
     * @return The ID of the saved solve, or an empty string if the save failed
     */
    suspend fun saveSolve(solve: Solve): String {
        val currentUser = auth.currentUser ?: return ""

        try {
            // Save to Firestore
            val solveRef = if (solve.id.isNotEmpty()) {
                // Update existing solve - don't include timestamp to preserve the original
                val solveData = mutableMapOf<String, Any>(
                    "cube" to solve.cube.name,
                    "tagId" to solve.tagId,
                    "duration" to solve.time,
                    "status" to solve.status.name,
                    "scramble" to solve.scramble,
                    "comment" to solve.comments
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("solves")
                    .document(solve.id)
                    .update(solveData)
                    .await()

                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("solves")
                    .document(solve.id)
            } else {
                // Create new solve - include timestamp for new solves
                val solveData = hashMapOf(
                    "cube" to solve.cube.name,
                    "tagId" to solve.tagId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "duration" to solve.time,
                    "status" to solve.status.name,
                    "scramble" to solve.scramble,
                    "comment" to solve.comments
                )

                // Create new solve
                val newSolveRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("solves")
                    .document()

                newSolveRef.set(solveData).await()
                newSolveRef
            }

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
            // Create the stats document ID
            val statsDocId = "${cubeType.name}_$tagId"
            Log.d(
                "FirebaseRepository",
                "Getting stats for document ID: '$statsDocId' (cube type: '${cubeType.name}', display name: '${cubeType.displayName}', tag: '$tagId')"
            )

            // Check if there are any stats documents for this user
            val allStatsQuery = firestore.collection("users")
                .document(currentUser.uid)
                .collection("stats")
                .get()
                .await()

            Log.d("FirebaseRepository", "Found ${allStatsQuery.size()} stats documents for user ${currentUser.uid}")
            allStatsQuery.documents.forEach { doc ->
                Log.d("FirebaseRepository", "Stats document: ${doc.id}")
            }

            // Get statistics from Firestore
            val statsRef = firestore.collection("users")
                .document(currentUser.uid)
                .collection("stats")
                .document(statsDocId)

            val statsDoc = statsRef.get().await()

            if (statsDoc.exists()) {
                Log.d("FirebaseRepository", "Stats document exists for '$statsDocId'")
                // Convert Firestore document to SolveStatistics
                val stats = SolveStatistics(
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
                Log.d(
                    "FirebaseRepository",
                    "Loaded stats for '$statsDocId': count=${stats.count}, best=${stats.best}, ao5=${stats.ao5}"
                )
                return stats
            } else {
                Log.d("FirebaseRepository", "No stats document found for '$statsDocId'")
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
                .collection("solves")
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
                .collection("solves")
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
     * Sets up a real-time listener for solves with pagination support
     *
     * @param onSolvesUpdate Callback that will be called whenever the solves list changes
     * @param pageSize Number of solves to load per page
     * @param lastVisibleDocument The last document from the previous page, or null for the first page
     * @param selectedCubeType The cube type to filter by, or "All" for all cube types
     * @param selectedTag The tag to filter by, or "All" for all tags
     * @param sortOrder The order to sort the solves by
     * @return A ListenerRegistration that can be used to remove the listener
     */
    fun listenForSolves(
        onSolvesUpdate: (List<Solve>, DocumentSnapshot?, Boolean) -> Unit,
        pageSize: Long = 100,
        lastVisibleDocument: DocumentSnapshot? = null,
        selectedCubeType: String = "All",
        selectedTag: String = "All",
        sortOrder: SortOrder = SortOrder.DATE_DESC
    ): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null

        try {
            // Create base query
            val solveCollection = firestore.collection("users")
                .document(currentUser.uid)
                .collection("solves")

            // Start with a query on the collection
            var query: Query = solveCollection

            // Apply sort order based on the provided parameter
            query = when (sortOrder) {
                SortOrder.DATE_DESC -> query.orderBy("timestamp", Query.Direction.DESCENDING)
                SortOrder.DATE_ASC -> query.orderBy("timestamp", Query.Direction.ASCENDING)
                SortOrder.TIME_ASC -> query.orderBy("duration", Query.Direction.ASCENDING)
                SortOrder.TIME_DESC -> query.orderBy("duration", Query.Direction.DESCENDING)
            }

            // Apply filters directly in the Firestore query if specific values are selected
            if (selectedCubeType != "All") {
                // Convert display name to enum name
                Log.d("FirebaseRepository", "Converting selected cube type: '$selectedCubeType'")
                val cubeTypeEnum = CubeType.fromDisplayName(selectedCubeType)
                Log.d("FirebaseRepository", "Converted to enum: ${cubeTypeEnum.name} (${cubeTypeEnum.displayName})")
                query = query.whereEqualTo("cube", cubeTypeEnum.name)
                Log.d("FirebaseRepository", "Added filter: cube = ${cubeTypeEnum.name}")
            }

            if (selectedTag != "All") {
                query = query.whereEqualTo("tagId", selectedTag)
                Log.d("FirebaseRepository", "Added filter: tagId = $selectedTag")
            }

            // Apply limit after filters
            // Query for one more document than the page size to determine if there are more solves
            query = query.limit(pageSize + 1)

            // If not the first page, start after the last document
            if (lastVisibleDocument != null) {
                query = query.startAfter(lastVisibleDocument)
            }

            // Add the snapshot listener
            return query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Error listening for solves: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("FirebaseRepository", "Snapshot is null")
                    return@addSnapshotListener
                }

                // Log the number of documents returned
                Log.d("FirebaseRepository", "Snapshot contains ${snapshot.documents.size} documents")

                // Convert documents to Solve objects
                val solves = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val cubeTypeStr = doc.getString("cube") ?: return@mapNotNull null
                    Log.d("FirebaseRepository", "Document ${doc.id} has cube type: '$cubeTypeStr'")

                    val cubeType = try {
                        CubeType.valueOf(cubeTypeStr)
                    } catch (e: Exception) {
                        Log.e(
                            "FirebaseRepository",
                            "Error converting cube type '$cubeTypeStr' to enum, defaulting to CUBE_3X3",
                            e
                        )
                        CubeType.CUBE_3X3
                    }

                    val tagId = doc.getString("tagId") ?: "normal"
                    val timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    val time = doc.getLong("duration") ?: 0L
                    val scramble = doc.getString("scramble") ?: ""
                    val statusStr = doc.getString("status") ?: SolveStatus.OK.name
                    val status = try {
                        SolveStatus.valueOf(statusStr)
                    } catch (e: Exception) {
                        SolveStatus.OK
                    }
                    val comments = doc.getString("comment") ?: ""

                    Solve(
                        id = id,
                        cube = cubeType,
                        tagId = tagId,
                        timestamp = timestamp,
                        time = time,
                        scramble = scramble,
                        status = status,
                        comments = comments
                    )
                }

                // Log the number of solves after conversion
                Log.d("FirebaseRepository", "Converted ${solves.size} solves")

                // Since we're filtering in the query, we don't need to filter again
                // But we'll keep this as a safety check in case the query filters don't work as expected
                val filteredSolves = solves

                // Check if we got more solves than the requested page size
                val hasMoreData = filteredSolves.size > pageSize.toInt()

                // Limit the solves to the requested page size
                val solvesToReturn = if (hasMoreData) {
                    filteredSolves.take(pageSize.toInt())
                } else {
                    filteredSolves
                }

                // Get the last visible document for pagination
                // Use the last document in the returned solves, not the extra one we queried
                val lastVisible = if (solvesToReturn.isNotEmpty()) {
                    // Find the document that corresponds to the last solve in solvesToReturn
                    val lastSolveId = solvesToReturn.last().id
                    snapshot.documents.find { it.id == lastSolveId }
                } else {
                    null
                }

                // Call the callback with the filtered solves, last visible document, and whether there are more solves
                onSolvesUpdate(solvesToReturn, lastVisible, hasMoreData)
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error setting up solves listener: ${e.message}", e)
            return null
        }
    }

    /**
     * Deletes multiple solves at once using Firestore batched writes
     * @param solveIds List of solve IDs to delete
     * @return The number of successfully deleted solves, or -1 if an error occurred
     */
    suspend fun deleteMultipleSolves(solveIds: List<String>): Int {
        if (solveIds.isEmpty()) return 0

        val currentUser = auth.currentUser ?: return -1

        try {
            // Group solves by cube type and tag for stats updates
            val solvesToDelete = mutableMapOf<Pair<CubeType, String>, MutableList<String>>()

            // First, get information about each solve to determine cube type and tag
            for (solveId in solveIds) {
                try {
                    val solveRef = firestore.collection("users")
                        .document(currentUser.uid)
                        .collection("solves")
                        .document(solveId)

                    val solveDoc = solveRef.get().await()

                    if (solveDoc.exists()) {
                        val cubeTypeStr = solveDoc.getString("cube") ?: continue
                        val tagId = solveDoc.getString("tagId") ?: continue

                        val cubeType = try {
                            CubeType.valueOf(cubeTypeStr)
                        } catch (e: Exception) {
                            continue
                        }

                        val key = Pair(cubeType, tagId)
                        if (!solvesToDelete.containsKey(key)) {
                            solvesToDelete[key] = mutableListOf()
                        }
                        solvesToDelete[key]?.add(solveId)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseRepository", "Error getting solve $solveId: ${e.message}", e)
                }
            }

            // Delete solves in batches
            var deletedCount = 0

            for ((cubeTypeTagPair, ids) in solvesToDelete) {
                // Process in batches of MAX_BATCH_SIZE
                ids.chunked(MAX_BATCH_SIZE).forEach { batchIds ->
                    val batch = firestore.batch()

                    for (id in batchIds) {
                        val solveRef = firestore.collection("users")
                            .document(currentUser.uid)
                            .collection("solves")
                            .document(id)

                        batch.delete(solveRef)
                    }

                    // Commit the batch
                    batch.commit().await()
                    deletedCount += batchIds.size
                }

                // Update stats for this cube type and tag
                val (cubeType, tagId) = cubeTypeTagPair
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
            }

            return deletedCount
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error deleting multiple solves: ${e.message}", e)
            return -1
        }
    }

    /**
     * Removes a tag for the current user and all solves and stats associated with it
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

            // If tag found, delete it and all associated solves and stats
            if (!querySnapshot.isEmpty) {
                // First, find all solves with this tag
                val solvesRef = firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("solves")
                    .whereEqualTo("tagId", tagName)

                val solvesSnapshot = solvesRef.get().await()
                Log.d("FirebaseRepository", "Deleting ${solvesSnapshot.size()} solves with tag: $tagName")

                // Delete solves in batches of 500 (Firestore batch limit)
                solvesSnapshot.documents.chunked(500).forEach { batch ->
                    val writeBatch = firestore.batch()
                    batch.forEach { solveDoc ->
                        writeBatch.delete(solveDoc.reference)
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

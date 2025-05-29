package com.example.cubespeed.ui.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.model.Solve
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * ViewModel for the History screen.
 * Handles business logic and state management for the history screen.
 */
class HistoryViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // State for solves
    var solves by mutableStateOf<List<Solve>>(emptyList())
        private set
    var filteredSolves by mutableStateOf<List<Solve>>(emptyList())
        private set

    // Search and sort state
    var searchQuery by mutableStateOf("")
        private set
    var sortOrder by mutableStateOf(SortOrder.DATE_DESC)
        private set

    // Filter state
    var selectedCubeType by mutableStateOf(AppState.selectedCubeType)
        private set
    var selectedTag by mutableStateOf(AppState.selectedTag)
        private set

    // Pagination state
    var lastVisibleDocument by mutableStateOf<DocumentSnapshot?>(null)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasMoreData by mutableStateOf(true)
        private set
    var isLoadingNextPage by mutableStateOf(false)
        private set

    // Selection mode state
    var isSelectionMode by mutableStateOf(false)
        private set
    var selectedSolves by mutableStateOf<Set<String>>(emptySet())
        private set

    // Dialog state
    var showDeleteConfirmation by mutableStateOf(false)
        private set
    var showCubeSelectionDialog by mutableStateOf(false)
        private set
    var showTagDialog by mutableStateOf(false)
        private set

    // Listener registration
    private var listenerRegistration: ListenerRegistration? = null

    // Page size for pagination
    private val pageSize = 100

    // Filter change key to track filter changes
    private var filterChangeKey = 0

    init {
        // Set up initial listener
        setupListener()
    }

    /**
     * Sets up a listener for solves with the current filter criteria.
     */
    private fun setupListener() {
        // Remove any existing listener
        listenerRegistration?.remove()

        // Reset state for new query
        lastVisibleDocument = null
        hasMoreData = true
        solves = emptyList()
        filteredSolves = emptyList()
        isLoadingMore = true
        isLoadingNextPage = false

        // Set up new listener
        listenerRegistration = repository.listenForSolves(
            onSolvesUpdate = { newSolves, lastVisible, hasMore ->
                handleSolvesUpdate(newSolves, lastVisible, hasMore)
            },
            pageSize = pageSize.toLong(),
            selectedCubeType = selectedCubeType,
            selectedTag = selectedTag,
            sortOrder = sortOrder
        )
    }

    /**
     * Handles updates from the snapshot listener.
     */
    private fun handleSolvesUpdate(
        newSolves: List<Solve>,
        lastVisible: DocumentSnapshot?,
        hasMore: Boolean
    ) {
        if (isLoadingNextPage) {
            // Append new solves to existing list when loading next page
            solves = solves + newSolves
            isLoadingNextPage = false
        } else {
            // Replace solves list when not loading next page
            solves = newSolves
        }

        lastVisibleDocument = lastVisible
        hasMoreData = hasMore
        isLoadingMore = false

        // Apply sort and filter
        applySortAndFilter()
    }

    /**
     * Applies search filter to the solves list.
     * Note: Sorting is now handled by the Firestore query, so we don't need to sort in memory.
     */
    private fun applySortAndFilter() {
        // Apply search filter
        filteredSolves = if (searchQuery.isBlank()) {
            solves
        } else {
            solves.filter { it.comments.contains(searchQuery, ignoreCase = true) }
        }
    }

    /**
     * Loads more solves when the user scrolls to the bottom.
     */
    fun loadMore() {
        viewModelScope.launch {
            // Set flag to indicate we're loading the next page
            isLoadingNextPage = true
            isLoadingMore = true

            // Set up a new listener for the next page
            listenerRegistration?.remove()
            listenerRegistration = repository.listenForSolves(
                onSolvesUpdate = { newSolves, lastVisible, hasMore ->
                    handleSolvesUpdate(newSolves, lastVisible, hasMore)
                },
                pageSize = pageSize.toLong(),
                lastVisibleDocument = lastVisibleDocument,
                selectedCubeType = selectedCubeType,
                selectedTag = selectedTag,
                sortOrder = sortOrder
            )
        }
    }

    /**
     * Updates the search query and applies the filter.
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        applySortAndFilter()
    }

    /**
     * Updates the sort order and resets the listener to apply the new sort order to the Firestore query.
     */
    fun updateSortOrder(order: SortOrder) {
        // Only reset the listener if the sort order has changed
        if (sortOrder != order) {
            sortOrder = order

            // Reset the listener to apply the new sort order to the Firestore query
            setupListener()
        }
    }

    /**
     * Updates the selected cube type and resets the listener.
     */
    fun updateSelectedCubeType(cubeType: String) {
        selectedCubeType = cubeType
        AppState.updateCubeType(cubeType)

        // Exit selection mode when changing filters
        exitSelectionMode()

        // Increment filter change key to trigger listener reset
        filterChangeKey++
        setupListener()
    }

    /**
     * Updates the selected tag and resets the listener.
     */
    fun updateSelectedTag(tag: String) {
        selectedTag = tag
        AppState.updateTag(tag)

        // Exit selection mode when changing filters
        exitSelectionMode()

        // Increment filter change key to trigger listener reset
        filterChangeKey++
        setupListener()
    }

    /**
     * Toggles the selection of a solve.
     */
    fun toggleSelection(solveId: String) {
        selectedSolves = if (selectedSolves.contains(solveId)) {
            // Remove from selection
            selectedSolves - solveId
        } else {
            // Add to selection
            selectedSolves + solveId
        }

        // If no solves are selected, exit selection mode
        if (selectedSolves.isEmpty()) {
            isSelectionMode = false
        }
    }

    /**
     * Enters selection mode and selects the given solve.
     */
    fun enterSelectionMode(solveId: String) {
        isSelectionMode = true
        selectedSolves = setOf(solveId)
    }

    /**
     * Exits selection mode and clears the selection.
     */
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedSolves = emptySet()
    }

    /**
     * Selects all solves.
     */
    fun selectAll() {
        selectedSolves = filteredSolves.map { it.id }.toSet()
    }

    /**
     * Shows the delete confirmation dialog.
     */
    fun showDeleteConfirmation() {
        if (selectedSolves.isNotEmpty()) {
            showDeleteConfirmation = true
        }
    }

    /**
     * Hides the delete confirmation dialog.
     */
    fun hideDeleteConfirmation() {
        showDeleteConfirmation = false
    }

    /**
     * Deletes the selected solves.
     */
    fun deleteSelectedSolves() {
        viewModelScope.launch {
            val deletedCount = repository.deleteMultipleSolves(selectedSolves.toList())
            if (deletedCount > 0) {
                // Increment history refresh trigger to update history screen
                AppState.historyRefreshTrigger += 1
                // Exit selection mode
                exitSelectionMode()
            }
        }
        showDeleteConfirmation = false
    }

    /**
     * Shows the cube selection dialog.
     */
    fun showCubeSelectionDialog() {
        showCubeSelectionDialog = true
    }

    /**
     * Hides the cube selection dialog.
     */
    fun hideCubeSelectionDialog() {
        showCubeSelectionDialog = false
    }

    /**
     * Shows the tag dialog.
     */
    fun showTagDialog() {
        showTagDialog = true
    }

    /**
     * Hides the tag dialog.
     */
    fun hideTagDialog() {
        showTagDialog = false
    }

    /**
     * Refreshes the data by resetting the listener.
     * This is called when the historyRefreshTrigger is incremented.
     */
    fun refreshData() {
        // Reset the listener to fetch fresh data
        setupListener()
    }

    /**
     * Cleans up resources when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

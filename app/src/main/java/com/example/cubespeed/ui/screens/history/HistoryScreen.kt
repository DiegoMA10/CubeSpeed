package com.example.cubespeed.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.screens.history.components.SearchFilterBar
import com.example.cubespeed.ui.screens.history.components.SelectionTopBar
import com.example.cubespeed.ui.screens.timer.dialogs.CubeSelectionDialog
import com.example.cubespeed.ui.screens.timer.dialogs.TagInputDialog
import com.example.cubespeed.ui.theme.dialogButtonTextColor
import com.example.cubespeed.ui.theme.isAppInLightTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * History screen that displays a list of solves with search, sort, and filter capabilities.
 *
 * @param navController Optional NavController for navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController? = null) {
    // Create or get the ViewModel
    val viewModel: HistoryViewModel = viewModel()

    // Observe changes to AppState and update ViewModel
    LaunchedEffect(AppState.selectedCubeType, AppState.selectedTag, AppState.historyRefreshTrigger) {
        // Check if filter criteria have changed
        if (viewModel.selectedCubeType != AppState.selectedCubeType || 
            viewModel.selectedTag != AppState.selectedTag) {
            // Update ViewModel with new filter criteria
            if (viewModel.selectedCubeType != AppState.selectedCubeType) {
                viewModel.updateSelectedCubeType(AppState.selectedCubeType)
            }
            if (viewModel.selectedTag != AppState.selectedTag) {
                viewModel.updateSelectedTag(AppState.selectedTag)
            }
        } else if (AppState.historyRefreshTrigger > 0) {
            // If only the refresh trigger changed, refresh the data
            viewModel.refreshData()
        }
    }

    // List of cube types
    val cubeTypes = CubeType.getAllDisplayNames()

    Column(modifier = Modifier.fillMaxSize()) {
        // Selection mode top bar or search/filter bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isAppInLightTheme) 2.dp else 0.dp,
            shadowElevation = if (isAppInLightTheme) 2.dp else 0.dp
        ) {
            if (viewModel.isSelectionMode) {
                // Selection mode top bar
                SelectionTopBar(
                    selectedCount = viewModel.selectedSolves.size,
                    onExitSelectionMode = viewModel::exitSelectionMode,
                    onSelectAll = viewModel::selectAll,
                    onDeleteSelected = viewModel::showDeleteConfirmation
                )
            } else {
                // Compact search and filter bar
                SearchFilterBar(
                    searchQuery = viewModel.searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    sortOrder = viewModel.sortOrder,
                    onSortOrderChange = viewModel::updateSortOrder,
                    filteredSolvesCount = viewModel.filteredSolves.size
                )
            }
        }

        // Display list of solves using EstadisticasGrid with pagination
        EstadisticasGrid(
            solves = viewModel.filteredSolves,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            isLoadingMore = viewModel.isLoadingMore,
            hasMoreData = viewModel.hasMoreData,
            onLoadMore = viewModel::loadMore,
            isSelectionMode = viewModel.isSelectionMode,
            selectedSolves = viewModel.selectedSolves,
            onSolveClick = { solve ->
                if (viewModel.isSelectionMode) {
                    viewModel.toggleSelection(solve.id)
                }
            },
            onSolveLongClick = { solve ->
                if (!viewModel.isSelectionMode) {
                    viewModel.enterSelectionMode(solve.id)
                } else {
                    viewModel.toggleSelection(solve.id)
                }
            }
        )
    }

    // Cube Selection Dialog
    if (viewModel.showCubeSelectionDialog) {
        CubeSelectionDialog(
            cubeTypes = cubeTypes,
            onCubeSelected = { 
                viewModel.updateSelectedCubeType(it)
                viewModel.hideCubeSelectionDialog()
            },
            onDismiss = viewModel::hideCubeSelectionDialog
        )
    }

    // Tag Dialog
    if (viewModel.showTagDialog) {
        TagInputDialog(
            currentTag = viewModel.selectedTag,
            onTagConfirmed = { 
                viewModel.updateSelectedTag(it)
                viewModel.hideTagDialog()
            },
            onDismiss = viewModel::hideTagDialog
        )
    }

    // Delete confirmation dialog
    if (viewModel.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text("Delete Selected Solves") },
            text = {
                Text(
                    "Are you sure you want to delete ${viewModel.selectedSolves.size} " +
                            "solve${if (viewModel.selectedSolves.size > 1) "s" else ""}? " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteSelectedSolves,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text("Cancel", color = dialogButtonTextColor)
                }
            }
        )
    }
}

// Sort order enum
enum class SortOrder {
    DATE_DESC, DATE_ASC, TIME_ASC, TIME_DESC
}

// Format date to a readable string (day and month only)
fun formatDate(date: Date): String {
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    return dateFormat.format(date)
}

// Format time to a readable string
fun formatTime(timeMillis: Long): String {
    val totalSeconds = timeMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val millis = (timeMillis % 1000) / 10  // Only use the first 2 digits of milliseconds

    return when {
        hours > 0 -> String.format("%d h %d:%02d", hours, minutes, seconds)  // No milliseconds when hours are present
        minutes > 0 -> String.format("%d:%02d.%02d", minutes, seconds, millis)
        else -> String.format("%d.%02d", seconds, millis)
    }
}

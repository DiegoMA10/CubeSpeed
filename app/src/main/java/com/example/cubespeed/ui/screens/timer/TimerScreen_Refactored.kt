package com.example.cubespeed.ui.screens.timer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cubespeed.model.CubeType
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.screens.timer.components.TimerComponent
import com.example.cubespeed.ui.screens.timer.dialogs.CubeSelectionDialog
import com.example.cubespeed.ui.screens.timer.dialogs.TagInputDialog
import com.example.cubespeed.ui.screens.timer.viewmodels.TimerViewModel
import kotlinx.coroutines.launch

/**
 * The main screen for the timer functionality.
 *
 * @param onTimerRunningChange Callback for when the timer's running state changes
 */
@Composable
fun TimerScreenRefactored(
    onTimerRunningChange: (Boolean) -> Unit = {}
) {
    // Use shared state for the selected cube type and tag
    var selectedCubeType by remember { mutableStateOf(AppState.selectedCubeType) }
    var selectedTag by remember { mutableStateOf(AppState.selectedTag) }

    // State for showing dialogs
    var showCubeSelectionDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Create the ViewModel
    val viewModel = remember { TimerViewModel(FirebaseRepository(), coroutineScope) }

    // Observe changes to AppState and update local state
    LaunchedEffect(AppState.selectedCubeType, AppState.selectedTag) {
        selectedCubeType = AppState.selectedCubeType
        selectedTag = AppState.selectedTag

        // Update the ViewModel
        viewModel.updateSelectedCubeType(selectedCubeType)
        viewModel.updateSelectedTag(selectedTag)
    }

    // List of available cube types
    val cubeTypes = CubeType.getAllDisplayNames()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Use the TimerComponent
            TimerComponent(
                viewModel = viewModel,
                onTimerRunningChange = onTimerRunningChange
            )
        }
    }

    // Cube Selection Dialog
    if (showCubeSelectionDialog) {
        CubeSelectionDialog(
            cubeTypes = cubeTypes,
            onCubeSelected = {
                selectedCubeType = it
                AppState.updateCubeType(it)
                viewModel.updateSelectedCubeType(it)
                showCubeSelectionDialog = false
            },
            onDismiss = { showCubeSelectionDialog = false }
        )
    }

    // Tag Dialog
    if (showTagDialog) {
        TagInputDialog(
            currentTag = selectedTag,
            onTagConfirmed = {
                selectedTag = it
                AppState.updateTag(it)
                viewModel.updateSelectedTag(it)
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false }
        )
    }
}

package com.example.cubespeed.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.ui.screens.timer.utils.getEffectiveTime
import com.example.cubespeed.ui.theme.isAppInLightTheme


/**
 * A composable that displays a grid of statistics with pagination support.
 *
 * @param solves The list of solves to display
 * @param modifier The modifier to be applied to the grid
 * @param isLoadingMore Whether more items are currently being loaded
 * @param hasMoreData Whether there are more items to load
 * @param onLoadMore Callback to load more items when the user scrolls to the bottom
 * @param isSelectionMode Whether selection mode is active
 * @param selectedSolves Set of selected solve IDs
 * @param onSolveClick Callback for when a solve is clicked
 * @param onSolveLongClick Callback for when a solve is long-clicked
 */
@Composable
fun EstadisticasGrid(
    solves: List<Solve>,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    hasMoreData: Boolean = false,
    onLoadMore: () -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedSolves: Set<String> = emptySet(),
    onSolveClick: (Solve) -> Unit = {},
    onSolveLongClick: (Solve) -> Unit = {}
) {
    // Remember the last index we've seen
    var lastVisibleItemIndex by remember { mutableStateOf(0) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(solves) { solve ->
            StatCard(
                solve = solve,
                isSelectionMode = isSelectionMode,
                isSelected = selectedSolves.contains(solve.id),
                onClick = { onSolveClick(solve) },
                onLongClick = { onSolveLongClick(solve) }
            )
        }

        // Add a loading indicator at the bottom when loading more
        if (isLoadingMore) {
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Show loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Add a "Load More" button if there's more data, we're not currently loading, and there are solves
        else if (hasMoreData && solves.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center

                ) {
                    Button(
                        onClick = onLoadMore,
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(text = "Load More")
                    }
                }
            }
        }
    }

    // Detect when user scrolls near the bottom and load more data automatically
    LaunchedEffect(solves.size) {
        if (solves.size > 0 && solves.size - lastVisibleItemIndex < 10 && !isLoadingMore && hasMoreData) {
            // We're close to the end, load more data
            onLoadMore()
        }

        // Update the last visible item index
        lastVisibleItemIndex = solves.size
    }
}

/**
 * A card displaying a single solve.
 *
 * @param solve The solve to display
 * @param modifier The modifier to be applied to the card
 * @param isSelectionMode Whether selection mode is active
 * @param isSelected Whether this solve is selected
 * @param onClick Callback for when the card is clicked
 * @param onLongClick Callback for when the card is long-clicked
 */
@Composable
fun StatCard(
    solve: Solve,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    // State to control dialog visibility
    var showDialog by remember { mutableStateOf(false) }

    // Show dialog when state is true and not in selection mode
    if (showDialog && !isSelectionMode) {
        TimeDetailDialog(
            solve = solve,
            onDismiss = { showDialog = false },
            onComment = { /* Handle comment action */ },
            onFlag = { /* Handle flag action */ },
            onMore = { /* Handle more action */ }
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .aspectRatio(2f)
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { 
                    if (isSelectionMode) {
                        onClick()
                    } else {
                        showDialog = true
                    }
                },
                onLongClick = { onLongClick() }
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAppInLightTheme) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Date in top-left corner
            Text(
                text = formatDate(solve.timestamp.toDate()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // Value in center
            if (solve.status == SolveStatus.DNF) {
                // Display DNF
                Text(
                    text = "DNF",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Get the effective time (with penalty applied if needed)
                val effectiveTime = getEffectiveTime(solve.time, solve.status)

                // Format the time and split it into main part and milliseconds part
                val timeString = formatTime(effectiveTime)
                val parts = timeString.split(".")
                val mainPart = parts[0]
                val millisPart = if (parts.size > 1) ".${parts[1]}" else ""

                // Add a "+" at the end if the status is PLUS2
                val suffix = if (solve.status == SolveStatus.PLUS2) "+" else ""

                // Display the time with main part and milliseconds part separately
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                ) {
                    // Main part of the time (hours/minutes/seconds)
                    Text(
                        text = mainPart,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            baselineShift = BaselineShift.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )

                    // Milliseconds part with smaller font
                    Text(
                        color = MaterialTheme.colorScheme.onSurface,
                        text = millisPart + suffix,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            baselineShift = BaselineShift(0.082F)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

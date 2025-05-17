package com.example.cubespeed.ui.screens.history

// Import the ScrambleVisualization composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cubespeed.model.Solve
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.repository.FirebaseRepository
import com.example.cubespeed.state.AppState
import com.example.cubespeed.ui.screens.timer.dialogs.CommentDialog
import com.example.cubespeed.ui.screens.timer.utils.getEffectiveTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * A dialog that displays details about a solve.
 *
 * @param solve The solve to display details for
 * @param onDismiss Callback for when the dialog is dismissed
 * @param onComment Callback for when the comment button is clicked
 * @param onFlag Callback for when the flag button is clicked
 */
@Composable
fun TimeDetailDialog(
    solve: Solve,
    onDismiss: () -> Unit,
    onComment: () -> Unit,
    onFlag: () -> Unit,
    onMore: () -> Unit
) {
    // State for dialogs
    var showCommentDialog by remember { mutableStateOf(false) }
    var showPenaltyDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf(solve.comments) }

    // Create repository and coroutine scope
    val repository = remember { FirebaseRepository() }
    val coroutineScope = rememberCoroutineScope()
    // Semi-transparent black background
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // White card in the center
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .clickable(enabled = false) { /* Prevent clicks from passing through */ },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header: Time and Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time with split format (main part and milliseconds)
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (solve.status == SolveStatus.DNF) {
                                // Display DNF
                                Text(
                                    text = "DNF",
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            } else {
                                // Get the effective time (with penalty applied if needed)
                                val effectiveTime = getEffectiveTime(solve.time, solve.status)

                                // Format the time and split it into main part and milliseconds part
                                val timeString = formatTime(effectiveTime)
                                val parts = timeString.split(".")
                                val mainPart = parts[0]
                                val millisPart = if (parts.size > 1) ".${parts[1]}" else ""

                                // No suffix for PLUS2 status as we'll show it separately
                                val suffix = ""

                                // Main part of the time (hours/minutes/seconds)
                                Text(
                                    text = mainPart,
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        baselineShift = BaselineShift.None
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )

                                // Milliseconds part with smaller font
                                Text(
                                    text = millisPart + suffix,
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        baselineShift = BaselineShift(0.082F)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }

                            // Show status in red to the right of the timer
                            if (solve.status == SolveStatus.DNF) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DNF",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            } else if (solve.status == SolveStatus.PLUS2) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+2",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }

                        // Date and time in small font
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date",
                                tint = Color.Gray
                            )
                            Text(
                                text = formatDateTimeForDisplay(solve.timestamp.toDate()),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness, color = Color.LightGray
                    )

                    // Comments section (only shown if comments exist)
                    if (solve.comments.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = Color.Gray,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            Text(
                                text = solve.comments,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black,
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness, color = Color.LightGray
                        )
                    }

                    // Scramble section
                    var isScrambleExpanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isScrambleExpanded = !isScrambleExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "Scramble",
                                tint = Color.Gray,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            Text(
                                text = solve.scramble,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black,
                                modifier = Modifier.weight(1f),
                                maxLines = if (isScrambleExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Icon(
                                imageVector = if (isScrambleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isScrambleExpanded) "Collapse" else "Expand",
                                tint = Color.Gray
                            )
                        }

                        // Show cube visualization when expanded
                        if (isScrambleExpanded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ScrambleVisualization(
                                    scramble = solve.scramble,
                                    cubeType = solve.cube,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness, color = Color.LightGray
                    )

                    // Footer with three icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Three dots on the left with dropdown menu
                        var showMoreOptions by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMoreOptions = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "More options",
                                    tint = Color.Gray
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreOptions,
                                onDismissRequest = { showMoreOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Delete", color = Color.Red)
                                        }
                                    },
                                    onClick = {
                                        showMoreOptions = false
                                        // Delete the solve
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val success = repository.deleteSolve(solve.id)
                                            if (success) {
                                                // Increment history refresh trigger to update history screen
                                                AppState.historyRefreshTrigger += 1
                                                // Dismiss the dialog
                                                onDismiss()
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Comment and flag on the right
                        Row {
                            IconButton(onClick = { showCommentDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Comment",
                                    tint = Color.Gray
                                )
                            }

                            IconButton(onClick = { showPenaltyDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Flag",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Comment Dialog
    if (showCommentDialog) {
        CommentDialog(
            commentText = commentText,
            onCommentChanged = { commentText = it },
            onDismiss = { showCommentDialog = false },
            onSave = {
                // Update the solve with the new comment
                val updatedSolve = solve.copy(comments = commentText)

                // Save the updated solve to Firestore
                coroutineScope.launch(Dispatchers.IO) {
                    repository.saveSolve(updatedSolve)
                    // Increment history refresh trigger to update history screen
                    AppState.historyRefreshTrigger += 1
                }
            }
        )
    }

    // Penalty Selection Dialog
    if (showPenaltyDialog) {
        PenaltySelectionDialog(
            currentStatus = solve.status,
            onStatusSelected = { newStatus ->
                // Calculate the new time based on status change
                val newTime = when {
                    // If changing from non-PLUS2 to PLUS2, add 2 seconds
                    solve.status != SolveStatus.PLUS2 && newStatus == SolveStatus.PLUS2 ->
                        solve.time + 2000 // Add 2 seconds (2000 ms)

                    // If changing from PLUS2 to non-PLUS2, subtract 2 seconds
                    solve.status == SolveStatus.PLUS2 && newStatus != SolveStatus.PLUS2 ->
                        solve.time - 2000 // Subtract 2 seconds (2000 ms)

                    // No change in time for other status changes
                    else -> solve.time
                }

                // Update the solve with the new status and adjusted time
                val updatedSolve = solve.copy(status = newStatus, time = newTime)

                // Save the updated solve to Firestore
                coroutineScope.launch(Dispatchers.IO) {
                    repository.saveSolve(updatedSolve)
                    // Increment history refresh trigger to update history screen
                    AppState.historyRefreshTrigger += 1
                }
            },
            onDismiss = { showPenaltyDialog = false }
        )
    }
}

/**
 * Formats a time in milliseconds to a display string.
 */
private fun formatTimeForDisplay(timeMillis: Long): String {
    return String.format("%.2f", timeMillis / 1000f)
}

/**
 * Formats a date to a display string.
 */
private fun formatDateTimeForDisplay(date: Date): String {
    val dateFormat = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(date)
}

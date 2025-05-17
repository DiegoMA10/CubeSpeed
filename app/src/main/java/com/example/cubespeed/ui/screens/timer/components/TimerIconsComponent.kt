package com.example.cubespeed.ui.screens.timer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A composable that displays the control icons for the timer.
 *
 * @param isLandscape Whether the device is in landscape mode
 * @param hasAddedTwoSeconds Whether two seconds have been added to the time
 * @param isDNF Whether the solve is marked as DNF (Did Not Finish)
 * @param isScreenPressed Whether the screen is currently being pressed
 * @param onDeleteClick Callback for when the delete icon is clicked
 * @param onDNFClick Callback for when the DNF icon is clicked
 * @param onPlusTwoClick Callback for when the +2 icon is clicked
 * @param onCommentClick Callback for when the comment icon is clicked
 */
@Composable
fun TimerIconsComponent(
    isLandscape: Boolean,
    hasAddedTwoSeconds: Boolean,
    isDNF: Boolean,
    isScreenPressed: Boolean = false,
    onDeleteClick: () -> Unit,
    onDNFClick: () -> Unit,
    onPlusTwoClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    // Button and icon sizes depend on orientation
    val buttonSize = if (isLandscape) 40.dp else 48.dp
    val iconSize = if (isLandscape) 25.dp else 28.dp
    val textSize = if (isLandscape) 12.sp else 14.sp

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // When either DNF or +2 is applied, show only the undo icon
        if (isDNF || hasAddedTwoSeconds) {
            // Show only a single "back" icon to undo the DNF or +2
            IconButton(
                onClick = if (isDNF) onDNFClick else onPlusTwoClick,
                modifier = Modifier
                    .size(buttonSize)
                    .background(Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = if (isDNF) "Remove DNF" else "Remove +2",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        // In all other cases, show all icons
        else {
            // Show all icons when no penalty is applied or when not in press state
            // Icon to remove the attempt
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(buttonSize)
                    .background(Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attempt",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize)
                )
            }

            // Icon to mark as DNF (Did Not Finish)
            IconButton(
                onClick = onDNFClick,
                modifier = Modifier
                    .size(buttonSize)
                    .background(Color.Transparent, CircleShape)
            ) {
                Text(
                    text = "DNF",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = textSize
                )
            }

            // Icon to add/remove 2 seconds
            IconButton(
                onClick = onPlusTwoClick,
                modifier = Modifier
                    .size(buttonSize)
                    .background(Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add 2 seconds",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize)
                )
            }

            // Icon to add comment
            IconButton(
                onClick = onCommentClick,
                modifier = Modifier
                    .size(buttonSize)
                    .background(Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Add comment",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

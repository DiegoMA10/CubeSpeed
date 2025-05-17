package com.example.cubespeed.ui.screens.timer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cubespeed.ui.utils.ScreenUtils

/**
 * A responsive composable that displays a scramble and provides controls for editing and refreshing it.
 * The layout adapts based on screen size and orientation.
 *
 * @param scramble The scramble to display
 * @param isLoading Whether the scramble is being loaded
 * @param contentColor The color of the scramble text
 * @param onEdit Callback for when the edit button is clicked
 * @param onShuffle Callback for when the shuffle button is clicked
 * @param onScrambleClick Callback for when the scramble text is clicked
 * @param modifier The modifier to be applied to the composable
 */
@Composable
fun ResponsiveScrambleBar(
    scramble: String,
    isLoading: Boolean,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onEdit: (current: String) -> Unit = {},
    onShuffle: () -> Unit = {},
    onScrambleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // State for showing the full scramble dialog
    var showFullScrambleDialog by remember { mutableStateOf(false) }

    // Check if we're on a tablet or in landscape mode
    val isTablet = ScreenUtils.isTablet()
    val isLandscape = ScreenUtils.isLandscape()

    // Determine the maximum number of characters to display based on device type and orientation
    val maxChars = when {
        isTablet -> 400 // Tablets can show more text
        isLandscape -> 100 // Landscape phones show less text to avoid overlap
        else -> 60 // Portrait phones show a moderate amount of text
    }

    // Determine the font size based on device type and orientation
    val fontSize = ScreenUtils.getResponsiveTextSize(
        tabletSize = 20.sp,
        phoneSize = if (isLandscape) 16.sp else 18.sp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(5.dp)
        ) {
            if (isLoading) {
                // Show loading indicator and text when scramble is loading
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Generando scramble...",
                        fontSize = fontSize,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Show the scramble text when not loading
                // Truncate long scrambles and add an ellipsis
                val displayText = if (scramble.length > maxChars) {
                    scramble.take(maxChars) + "..."
                } else {
                    scramble
                }

                Text(
                    text = displayText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = if (isLandscape) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // No ripple effect
                        ) {
                            if (scramble.length > maxChars) {
                                // Show the full scramble dialog if the scramble is truncated
                                showFullScrambleDialog = true
                            } else {
                                // Otherwise, use the default click behavior
                                onScrambleClick()
                            }
                        }
                )
            }

            // Icons at the bottom right
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit icon
                IconButton(
                    onClick = { onEdit(scramble) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar scramble",
                        tint = contentColor
                    )
                }

                // Refresh icon
                IconButton(
                    onClick = {
                        // Just call onShuffle which will update the Timer's scramble
                        // and the ScrambleBar will get the new scramble on recomposition
                        onShuffle()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Nueva scramble",
                        tint = contentColor
                    )
                }
            }
        }
    }

    // Show the full scramble dialog if requested
    if (showFullScrambleDialog) {
        FullScrambleDialog(
            scramble = scramble,
            onDismiss = { showFullScrambleDialog = false }
        )
    }
}

/**
 * A dialog that displays the full scramble.
 *
 * @param scramble The scramble to display
 * @param onDismiss Callback for when the dialog is dismissed
 */
@Composable
private fun FullScrambleDialog(
    scramble: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Scramble",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = scramble,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

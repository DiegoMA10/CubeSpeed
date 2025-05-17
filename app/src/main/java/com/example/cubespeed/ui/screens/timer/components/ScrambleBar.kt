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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A composable that displays a scramble and provides controls for editing and refreshing it.
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
fun ScrambleBar(
    scramble: String,
    isLoading: Boolean,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onEdit: (current: String) -> Unit = {},
    onShuffle: () -> Unit = {},
    onScrambleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Show the scramble text when not loading
                Text(
                    text = scramble,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // No ripple effect
                        ) {
                            onScrambleClick()
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
}
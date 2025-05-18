package com.example.cubespeed.ui.screens.timer.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.cubespeed.ui.theme.dialogButtonTextColor
import com.example.cubespeed.ui.theme.isAppInLightTheme

/**
 * A dialog for selecting a cube type.
 *
 * @param cubeTypes The list of available cube types
 * @param onCubeSelected Callback for when a cube type is selected
 * @param onDismiss Callback for when the dialog is dismissed
 */
@Composable
fun CubeSelectionDialog(
    cubeTypes: List<String>,
    onCubeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isAppInLightTheme) Color.White else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isAppInLightTheme) 4.dp else 0.dp,
            shadowElevation = if (isAppInLightTheme) 4.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Cube Type",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(cubeTypes) { cubeType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCubeSelected(cubeType)
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cubeType,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            // Show a checkmark for the currently selected cube type
                            if (cubeType == onCubeSelected.toString()) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (isAppInLightTheme) Color.Gray else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Add a divider between items
                        if (cubeType != cubeTypes.last()) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }
                }

                // Add a cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 16.dp)
                ) {
                    Text("Cancel", color = dialogButtonTextColor)
                }
            }
        }
    }
}

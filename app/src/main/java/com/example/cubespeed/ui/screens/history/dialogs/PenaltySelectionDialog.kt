package com.example.cubespeed.ui.screens.history.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cubespeed.model.SolveStatus
import com.example.cubespeed.ui.theme.dialogButtonTextColor

/**
 * A dialog for selecting a penalty for a solve.
 *
 * @param currentStatus The current status of the solve
 * @param onStatusSelected Callback for when a status is selected
 * @param onDismiss Callback for when the dialog is dismissed
 */
@Composable
fun PenaltySelectionDialog(
    currentStatus: SolveStatus,
    onStatusSelected: (SolveStatus) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Penalty") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // None option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = selectedStatus == SolveStatus.OK,
                        onClick = { selectedStatus = SolveStatus.OK }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "None",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // +2 option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = selectedStatus == SolveStatus.PLUS2,
                        onClick = { selectedStatus = SolveStatus.PLUS2 }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+2",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // DNF option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = selectedStatus == SolveStatus.DNF,
                        onClick = { selectedStatus = SolveStatus.DNF }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DNF",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onStatusSelected(selectedStatus)
                    onDismiss()
                }
            ) {
                Text("Apply", color = dialogButtonTextColor)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = dialogButtonTextColor)
            }
        }
    )
}
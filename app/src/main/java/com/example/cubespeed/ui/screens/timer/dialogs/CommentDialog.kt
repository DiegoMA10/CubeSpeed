package com.example.cubespeed.ui.screens.timer.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.cubespeed.ui.theme.dialogButtonTextColor

/**
 * A dialog for adding a comment to a solve.
 *
 * @param commentText The current comment text
 * @param onCommentChanged Callback for when the comment text changes
 * @param onDismiss Callback for when the dialog is dismissed
 * @param onSave Callback for when the comment is saved
 */
@Composable
fun CommentDialog(
    commentText: String,
    onCommentChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Comment") },
        text = {
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentChanged,
                label = { Text("Comment") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave()
                    onDismiss()
                }
            ) {
                Text("Save", color = dialogButtonTextColor)
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

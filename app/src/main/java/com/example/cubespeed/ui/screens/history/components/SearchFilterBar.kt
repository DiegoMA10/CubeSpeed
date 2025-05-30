package com.example.cubespeed.ui.screens.history.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cubespeed.ui.screens.history.enums.SortOrder
import com.example.cubespeed.ui.theme.AppThemeType
import com.example.cubespeed.ui.theme.LocalThemePreference

/**
 * Helper function to determine the appropriate icon color based on the current theme and selection state
 */
@Composable
private fun getIconColor(isSelected: Boolean): Color {
    val currentTheme = LocalThemePreference.current

    return when {
        isSelected -> {
            when (currentTheme) {
                AppThemeType.LIGHT -> Color.Black
                AppThemeType.DARK -> Color.White
                AppThemeType.BLUE -> MaterialTheme.colorScheme.secondary
            }
        }

        else -> {
            // Non-selected icons are gray in all themes
            Color.Gray
        }
    }
}

/**
 * A composable that displays a search and filter bar for the history screen.
 *
 * @param searchQuery The current search query
 * @param onSearchQueryChange Callback for when the search query changes
 * @param sortOrder The current sort order
 * @param onSortOrderChange Callback for when the sort order changes
 * @param filteredSolvesCount The number of filtered solves
 */
@Composable
fun SearchFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    filteredSolvesCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            placeholder = {
                Text(
                    "Search comments...",
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = getIconColor(isSelected = false),
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        // Results count
                        Text(
                            text = "$filteredSolvesCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Clear button
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.secondary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            ),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
            )
        )

        // Date sort icon button
        IconButton(
            onClick = {
                onSortOrderChange(
                    if (sortOrder == SortOrder.DATE_DESC)
                        SortOrder.DATE_ASC
                    else
                        SortOrder.DATE_DESC
                )
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Sort by date",
                tint = getIconColor(isSelected = sortOrder == SortOrder.DATE_DESC || sortOrder == SortOrder.DATE_ASC),
                modifier = Modifier.size(20.dp)
            )
        }

        // Time sort icon button
        IconButton(
            onClick = {
                onSortOrderChange(
                    if (sortOrder == SortOrder.TIME_ASC)
                        SortOrder.TIME_DESC
                    else
                        SortOrder.TIME_ASC
                )
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Sort by time",
                tint = getIconColor(isSelected = sortOrder == SortOrder.TIME_ASC || sortOrder == SortOrder.TIME_DESC),
                modifier = Modifier.size(20.dp)
            )
        }

        // Single sort direction arrow for both filters
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp)
        ) {
            // Always show arrow to indicate current sort direction
            Icon(
                imageVector = when (sortOrder) {
                    SortOrder.DATE_ASC -> Icons.Default.ArrowUpward
                    SortOrder.TIME_ASC -> Icons.Default.ArrowUpward
                    SortOrder.DATE_DESC -> Icons.Default.ArrowDownward
                    SortOrder.TIME_DESC -> Icons.Default.ArrowDownward
                },
                contentDescription = "Sort direction",
                tint = getIconColor(isSelected = true),
                modifier = Modifier.size(20.dp)
            )
        }

    }
}

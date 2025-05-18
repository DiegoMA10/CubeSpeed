package com.example.cubespeed.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cubespeed.ui.theme.isAppInLightTheme
import com.example.cubespeed.ui.theme.LocalThemePreference
import com.example.cubespeed.ui.theme.AppThemeType

/**
 * A reusable top bar component that displays the cube type and tag.
 * 
 * @param modifier Modifier to be applied to the composable
 * @param title The main title to display (usually the cube type)
 * @param subtitle The subtitle to display (usually the tag)
 * @param backgroundColor The background color of the top bar
 * @param contentColor The color of the content (text and icons)
 * @param onSettingsClick Callback for when the settings icon is clicked
 * @param onOptionsClick Callback for when the tag icon is clicked
 * @param onCubeClick Callback for when the cube type is clicked
 */
@Composable
fun CubeTopBar(
    modifier: Modifier = Modifier,
    title: String = "3x3 Cube",
    subtitle: String = "normal",
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onSettingsClick: () -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onCubeClick: () -> Unit = {}
) {
    // Create a scroll state for horizontal scrolling
    val scrollState = rememberScrollState()

    // Wrap the Surface in a horizontally scrollable container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .horizontalScroll(scrollState)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }

                // Add some spacing
                Spacer(modifier = Modifier.width(16.dp))

                // Cube type + dropdown and tag centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onCubeClick)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Cube Type",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }

                // Add some spacing
                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = onOptionsClick) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = "Add Tag",
                        tint = when (LocalThemePreference.current) {
                            AppThemeType.BLUE -> Color.White
                            AppThemeType.LIGHT -> Color.Black
                            else -> contentColor
                        }
                    )
                }
            }
        }
    }
}

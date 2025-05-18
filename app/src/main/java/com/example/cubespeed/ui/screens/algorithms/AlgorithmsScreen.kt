package com.example.cubespeed.ui.screens.algorithms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cubespeed.navigation.Route
import com.example.cubespeed.ui.theme.isAppInLightTheme

/**
 * Screen that displays three cards for OLL, PLL, and GAME
 * 
 * @param navController Navigation controller for navigating to other screens
 */
@Composable
fun AlgorithmsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // OLL Card
        AlgorithmCard(
            title = "OLL",
            description = "Orientation of Last Layer",
            icon = Icons.Filled.Code,
            onClick = {
                navController.navigate(Route.OLLAlgorithms.route)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PLL Card
        AlgorithmCard(
            title = "PLL",
            description = "Permutation of Last Layer",
            icon = Icons.Outlined.Code,
            onClick = {
                navController.navigate(Route.PLLAlgorithms.route)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Game Card
        AlgorithmCard(
            title = "GAME",
            description = "Practice your skills",
            icon = Icons.Filled.Games,
            onClick = {
                navController.navigate(Route.Game.route)
            }
        )
    }
}

/**
 * A card component for displaying algorithm categories
 * 
 * @param title The title of the card
 * @param description A brief description
 * @param icon Icon to display
 * @param onClick Callback for when the card is clicked
 */
@Composable
fun AlgorithmCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isAppInLightTheme) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on the left
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

package com.example.cubespeed.ui.screens.algorithms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.cubespeed.ui.components.OLLView
import com.example.cubespeed.ui.theme.isAppInLightTheme
import com.example.cubespeed.utils.AlgUtils

/**
 * Screen that displays OLL algorithms with a custom top bar
 * 
 * @param navController Navigation controller for navigating back
 */
@Composable
fun OLLAlgorithmsScreen(navController: NavController) {
    var showOLLAlgorithmDialog by remember { mutableStateOf(false) }
    var selectedOLLAlgorithm by remember { mutableStateOf<String?>(null) }
    val ollCases = remember { AlgUtils.getAllOLLCases() }
    val context = LocalContext.current

    // Show OLL algorithm detail dialog when an OLL algorithm is selected
    if (showOLLAlgorithmDialog && selectedOLLAlgorithm != null) {
        OLLAlgorithmDetailDialogImpl(
            caseName = selectedOLLAlgorithm!!,
            onDismiss = { showOLLAlgorithmDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom top bar with back button and "Algoritmos" title
        Surface(
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Algoritmos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // OLL Algorithms content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Grid of OLL cases with improved styling
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                items(ollCases) { caseName ->
                    val caseState = AlgUtils.getCaseState(context, "OLL", caseName)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f) // Taller cards
                            .clickable {
                                selectedOLLAlgorithm = caseName
                                showOLLAlgorithmDialog = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isAppInLightTheme) 4.dp else 0.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp), // Reduced padding
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Case name at the top with colored background
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            ) {
                                Text(
                                    text = caseName,
                                    fontSize = 14.sp, // Slightly larger font
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )
                            }

                            // Divider between title and image
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )

                            // OLL view takes most of the space
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                OLLView(
                                    state = caseState,
                                    size = 80.dp, // Larger size
                                    gap = 3.dp,
                                    cornerRadius = 4.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog that displays details about an OLL algorithm.
 *
 * @param caseName The name of the OLL case
 * @param onDismiss Callback for when the dialog is dismissed
 */
@Composable
private fun OLLAlgorithmDetailDialogImpl(
    caseName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val caseState = AlgUtils.getCaseState(context, "OLL", caseName)
    val algorithm = AlgUtils.getDefaultAlgorithm(caseName)

    // Split algorithm by line breaks
    val algorithmLines = algorithm.split("\n")

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
                    .width(340.dp) // Wider card
                    .clickable(enabled = false) { /* Prevent clicks from passing through */ },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isAppInLightTheme) 4.dp else 0.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Case name with colored background
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = caseName,
                            fontSize = 22.sp, // Larger font
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    // Display the OLL view with a larger size
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            OLLView(
                                state = caseState,
                                size = 240.dp, // Much larger size
                                gap = 6.dp,
                                cornerRadius = 6.dp
                            )
                        }
                    }

                    // Display the algorithm with better formatting
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // Same background color as image
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Solutions:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Display each algorithm line separately
                            algorithmLines.forEach { line ->
                                if (line.isNotEmpty()) {
                                    Text(
                                        text = line,
                                        fontSize = 14.sp, // Smaller font
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

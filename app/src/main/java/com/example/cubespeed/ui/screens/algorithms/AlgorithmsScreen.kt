package com.example.cubespeed.ui.screens.algorithms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cubespeed.ui.components.OLLView
import com.example.cubespeed.ui.components.PLLView
import com.example.cubespeed.utils.AlgUtils

/**
 * Screen that displays OLL and PLL algorithms in a style similar to Twisty Timer
 */
@Composable
fun AlgorithmsScreen() {
    var showOLLAlgorithmDialog by remember { mutableStateOf(false) }
    var selectedOLLAlgorithm by remember { mutableStateOf<String?>(null) }
    var showPLLAlgorithmDialog by remember { mutableStateOf(false) }
    var selectedPLLAlgorithm by remember { mutableStateOf<String?>(null) }
    val ollCases = remember { AlgUtils.getAllOLLCases() }
    val pllCases = remember { AlgUtils.getAllPLLCases() }
    val context = LocalContext.current

    // Show OLL algorithm detail dialog when an OLL algorithm is selected
    if (showOLLAlgorithmDialog && selectedOLLAlgorithm != null) {
        OLLAlgorithmDetailDialog(
            caseName = selectedOLLAlgorithm!!,
            onDismiss = { showOLLAlgorithmDialog = false }
        )
    }

    // Show PLL algorithm detail dialog when a PLL algorithm is selected
    if (showPLLAlgorithmDialog && selectedPLLAlgorithm != null) {
        PLLAlgorithmDetailDialog(
            caseName = selectedPLLAlgorithm!!,
            onDismiss = { showPLLAlgorithmDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "OLL Algorithms",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Grid of OLL cases with improved styling
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp) // Increased height for larger cards
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
                                color = Color.Black,
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

        // Add spacing between OLL and PLL sections
        Spacer(modifier = Modifier.height(24.dp))

        // PLL Algorithms section
        Text(
            text = "PLL Algorithms",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Grid of PLL cases with improved styling
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            items(pllCases) { caseName ->
                val caseState = AlgUtils.getCaseState(context, "PLL", caseName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f) // Taller cards
                        .clickable {
                            selectedPLLAlgorithm = caseName
                            showPLLAlgorithmDialog = true
                        },// Increased shadow and corner radius
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.dp), // Increased padding
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
                                color = Color.Black,
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
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )

                        // PLL view takes most of the space
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PLLView(
                                state = caseState,
                                pllCase = caseName,
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

/**
 * Dialog that displays details about an OLL algorithm.
 *
 * @param caseName The name of the OLL case
 * @param onDismiss Callback for when the dialog is dismissed
 */
@Composable
fun OLLAlgorithmDetailDialog(
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
                    containerColor = Color.White
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

/**
 * Dialog that displays details about a PLL algorithm.
 *
 * @param caseName The name of the PLL case
 * @param onDismiss Callback for when the dialog is dismissed
 */
@Composable
fun PLLAlgorithmDetailDialog(
    caseName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val caseState = AlgUtils.getCaseState(context, "PLL", caseName)
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
                    containerColor = Color.White
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
                        color = MaterialTheme.colorScheme.secondary, // Using secondary color for PLL
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = caseName,
                            fontSize = 22.sp, // Larger font
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }

                    // Display the PLL view with a larger size
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), // Using secondary color for PLL
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PLLView(
                                state = caseState,
                                pllCase = caseName,
                                size = 240.dp, // Much larger size
                                gap = 6.dp,
                                cornerRadius = 6.dp
                            )
                        }
                    }

                    // Display the algorithm with better formatting
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), // Same background color as image
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

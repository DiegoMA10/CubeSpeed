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
import com.example.cubespeed.utils.AlgUtils

/**
 * Screen that displays OLL algorithms in a style similar to Twisty Timer
 */
@Composable
fun AlgorithmsScreen() {
    var showAlgorithmDialog by remember { mutableStateOf(false) }
    var selectedAlgorithm by remember { mutableStateOf<String?>(null) }
    val ollCases = remember { AlgUtils.getAllOLLCases() }
    val context = LocalContext.current

    // Show algorithm detail dialog when an algorithm is selected
    if (showAlgorithmDialog && selectedAlgorithm != null) {
        AlgorithmDetailDialog(
            caseName = selectedAlgorithm!!,
            onDismiss = { showAlgorithmDialog = false }
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
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp) // Reduced height to make it more compact
        ) {
            items(ollCases) { caseName ->
                val caseState = AlgUtils.getCaseState(context, "OLL", caseName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.9f)
                        .clickable {
                            selectedAlgorithm = caseName
                            showAlgorithmDialog = true
                        }
                        .shadow(2.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // OLL view takes most of the space
                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .padding(2.dp)
                        ) {
                            OLLView(
                                state = caseState,
                                size = 60.dp,
                                gap = 2.dp,
                                cornerRadius = 3.dp
                            )
                        }

                        // Case name at the bottom with colored background
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.3f),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        ) {
                            Text(
                                text = caseName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
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
fun AlgorithmDetailDialog(
    caseName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val caseState = AlgUtils.getCaseState(context, "OLL", caseName)
    val algorithm = AlgUtils.getDefaultAlgorithm(caseName)

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
                    .width(320.dp)
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Display the OLL view with a larger size
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        OLLView(
                            state = caseState,
                            size = 200.dp,
                            gap = 5.dp,
                            cornerRadius = 5.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display the algorithm with better formatting
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = algorithm.ifEmpty { "No algorithm available" },
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

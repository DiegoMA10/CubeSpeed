package com.example.cubespeed.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.cubespeed.ui.theme.AppThemeType
import com.example.cubespeed.ui.theme.isAppInLightTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

/**
 * Settings screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onThemeChanged: (AppThemeType) -> Unit = {}
) {
    val auth = remember { Firebase.auth }
    val currentUser = remember { auth.currentUser }
    val context = LocalContext.current

    // State for settings
    var selectedTheme by remember { mutableStateOf(AppThemeType.BLUE) }

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load settings from preferences
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("cubespeed_settings", 0)
        val themeOrdinal = sharedPrefs.getInt("theme_type", AppThemeType.BLUE.ordinal)
        selectedTheme = AppThemeType.values()[themeOrdinal]
    }

    // Function to save settings
    fun saveSettings() {
        val sharedPrefs = context.getSharedPreferences("cubespeed_settings", 0)
        with(sharedPrefs.edit()) {
            putInt("theme_type", selectedTheme.ordinal)
            apply()
        }

        scope.launch {
            snackbarHostState.showSnackbar("Settings saved")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isAppInLightTheme) 2.dp else 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "User Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Email: ${currentUser?.email ?: "Not logged in"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Theme settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isAppInLightTheme) 2.dp else 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )

                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Radio button group for theme selection
                    Column(
                        modifier = Modifier
                            .selectableGroup()
                            .padding(start = 40.dp)
                    ) {
                        // Blue theme option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = selectedTheme == AppThemeType.BLUE,
                                    onClick = { 
                                        selectedTheme = AppThemeType.BLUE
                                        saveSettings()
                                        onThemeChanged(AppThemeType.BLUE)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == AppThemeType.BLUE,
                                onClick = null, // null because we're handling the click on the row
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = androidx.compose.ui.graphics.Color(0xFF2962FF), // Fixed blue color
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "Blue",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        // Red theme option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = selectedTheme == AppThemeType.LIGHT,
                                                    onClick = { 
                                                        selectedTheme = AppThemeType.LIGHT
                                                        saveSettings()
                                                        onThemeChanged(AppThemeType.LIGHT)
                                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == AppThemeType.LIGHT,
                                onClick = null, // null because we're handling the click on the row
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = androidx.compose.ui.graphics.Color(0xFF757575), // Fixed gray color
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "Light",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        // Dark theme option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = selectedTheme == AppThemeType.DARK,
                                    onClick = { 
                                        selectedTheme = AppThemeType.DARK
                                        saveSettings()
                                        onThemeChanged(AppThemeType.DARK)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == AppThemeType.DARK,
                                onClick = null, // null because we're handling the click on the row
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = androidx.compose.ui.graphics.Color(0xFFBB86FC), // Fixed purple color
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "Dark",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }


            // Logout button
            Button(
                onClick = {
                    // Reset theme to blue
                    selectedTheme = AppThemeType.BLUE

                    // Save the blue theme setting
                    val sharedPrefs = context.getSharedPreferences("cubespeed_settings", 0)
                    with(sharedPrefs.edit()) {
                        putInt("theme_type", AppThemeType.BLUE.ordinal)
                        apply()
                    }

                    // Notify parent about theme change
                    onThemeChanged(AppThemeType.BLUE)

                    // Configure Google Sign In
                    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("133521252013-m54d42bmbbtgolvjasrhohtcbuh6hs57.apps.googleusercontent.com")
                        .requestEmail()
                        .build()

                    // Get Google Sign In client
                    val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

                    // Sign out from Google
                    googleSignInClient.signOut().addOnCompleteListener {
                        // Then sign out from Firebase
                        auth.signOut()
                        // Navigate to login screen
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (isAppInLightTheme) 2.dp else 0.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Logout")
            }

            // App info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isAppInLightTheme) 2.dp else 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "CubeSpeed v1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "A timer app for speedcubing enthusiasts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

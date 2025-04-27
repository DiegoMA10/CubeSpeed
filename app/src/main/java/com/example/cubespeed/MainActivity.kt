package com.example.cubespeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cubespeed.navigation.NavigationWrapper
import com.example.cubespeed.navigation.Route
import com.example.cubespeed.ui.theme.CubeSpeedTheme
import com.example.cubespeed.ui.theme.LocalThemePreference
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Load theme preference from SharedPreferences
            var isDarkTheme by remember {
                val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
                mutableStateOf(sharedPrefs.getBoolean("dark_theme", false))
            }

            // Listen for theme preference changes
            val themeChangeListener = { newTheme: Boolean ->
                isDarkTheme = newTheme
                val sharedPrefs = getSharedPreferences("cubespeed_settings", 0)
                with(sharedPrefs.edit()) {
                    putBoolean("dark_theme", newTheme)
                    apply()
                }
            }

            // Provide the theme preference to the composition
            CompositionLocalProvider(LocalThemePreference provides isDarkTheme) {
                CubeSpeedTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Check if user is already authenticated
                        val auth = Firebase.auth
                        val startDestination = if (auth.currentUser != null) {
                            Route.Timer.route
                        } else {
                            Route.Login.route
                        }

                        Scaffold { innerPadding ->
                            NavigationWrapper(
                                modifier = Modifier.padding(innerPadding),
                                startDestination = startDestination,
                                onThemeChanged = themeChangeListener
                            )
                        }
                    }
                }
            }
        }
    }
}

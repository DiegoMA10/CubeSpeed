package com.example.cubespeed.ui.screens.game

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

/**
 * Game screen that launches the Unity game activity
 * 
 * @param navController Navigation controller for navigating back
 */
@Composable
fun GameScreen(navController: NavController) {
    val context = LocalContext.current

    // Launch the custom Unity activity from the Unity module when the screen is composed
    LaunchedEffect(Unit) {
        try {
            // Use Class.forName to get the class object for the CustomUnityPlayerActivity from the Unity module
            val customUnityPlayerActivityClass = Class.forName("com.unity3d.player.CustomUnityPlayerActivity")
            val intent = Intent(context, customUnityPlayerActivityClass)
            context.startActivity(intent)
        } catch (e: ClassNotFoundException) {
            // Fallback to the original Unity activity if the custom one is not found
            val intent = Intent(context, Class.forName("com.unity3d.player.UnityPlayerGameActivity"))
            context.startActivity(intent)
        }
    }

    // This composable doesn't render anything visible
    // It just launches the Unity activity and then gets removed from the backstack
}

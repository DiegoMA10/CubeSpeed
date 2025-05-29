package com.example.cubespeed.unity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.unity3d.player.UnityPlayerGameActivity;

/**
 * Custom Unity Player Game Activity that serves as a wrapper for the Unity activity.
 * This class is needed to handle the back button press in the Unity activity.
 */
public class CustomUnityPlayerGameActivity extends Activity {
    private static final String TAG = "CustomUnityGameActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting Unity activity");

        // Launch the Unity activity
        Intent intent = new Intent(this, UnityPlayerGameActivity.class);
        // Add any extras from the original intent
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }


        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Activity resumed, finishing");

        // When this activity resumes (after Unity activity is closed),
        // finish it to return to the main app
        finish();
    }
}
package com.example.cubespeed.unity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import com.unity3d.player.UnityPlayerGameActivity;
import com.example.cubespeed.MainActivity;

/**
 * Custom Activity that launches the Unity player activity
 * and handles back button press to return to the main app.
 * 
 * This activity serves as a bridge between the main app and the Unity activity.
 * When the back button is pressed in the Unity activity, it will navigate back to this activity,
 * which will then navigate back to the main app.
 */
public class CustomUnityPlayerActivity extends Activity {
    private boolean unityLaunched = false;
    private long launchTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launch Unity activity directly
        Intent intent = new Intent(this, UnityPlayerGameActivity.class);
        // Add any extras from the original intent
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        unityLaunched = true;
        launchTime = System.currentTimeMillis();
        startActivity(intent);
    }

    /**
     * Override onKeyDown to handle back button press.
     * This is called when the back button is pressed in this activity.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Launch MainActivity and finish this activity
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Override onResume to launch MainActivity when it becomes visible again.
     * This happens when the user presses the back button in the Unity activity.
     * We only do this if enough time has passed since the Unity activity was launched,
     * to prevent immediate return to MainActivity when the Unity activity is first launched.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Calculate time since Unity activity was launched
        long timeSinceLaunch = System.currentTimeMillis() - launchTime;

        // If Unity was launched and enough time has passed (more than 1 second),
        // assume the user pressed back in the Unity activity
        if (unityLaunched && timeSinceLaunch > 1000) {
            // If this activity becomes visible again (after Unity activity is closed),
            // launch MainActivity and finish this activity
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}

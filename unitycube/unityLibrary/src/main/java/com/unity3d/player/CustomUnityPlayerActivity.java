package com.unity3d.player;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Custom Unity Player Activity that extends the standard Unity player activity
 * and handles back button press to return to the main app.
 * <p>
 * This activity is responsible for handling the back button press in the Unity activity
 * and returning to the main app (MainActivity).
 */
public class CustomUnityPlayerActivity extends UnityPlayerGameActivity {
    private static final String TAG = "CustomUnityActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: CustomUnityPlayerActivity created");
    }

    /**
     * Override dispatchKeyEvent to intercept the back button press before it's handled by the Unity engine.
     * This method is called before onKeyDown and might be able to intercept the back button press
     * before it's handled by the Unity engine.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent: keyCode=" + event.getKeyCode() + ", action=" + event.getAction());
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "dispatchKeyEvent: Back button event, returning to main app");
            returnToMainActivity();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * Override onKeyDown to handle back button press.
     * This is called when the back button is pressed in this activity.
     * <p>
     * The issue description mentions "Handle cmd APP_CMD_KEY_EVENT(19)" which refers to
     * the back button key code (19 is KEYCODE_BACK in Android).
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "onKeyDown: Back button pressed (APP_CMD_KEY_EVENT(19)), returning to main app");
            returnToMainActivity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Override onBackPressed to handle back button press.
     * This is a higher-level method than onKeyDown and provides another opportunity
     * to intercept the back button press.
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: Back button pressed, returning to main app");
        returnToMainActivity();
    }

    /**
     * Override onKeyUp to handle back button press on key up.
     * Some systems might handle the back button press on key up rather than key down.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp: keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "onKeyUp: Back button released, returning to main app");
            returnToMainActivity();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper method to return to the main activity.
     * This centralizes the logic for returning to the main activity.
     */
    private void returnToMainActivity() {
        try {
            // Create an explicit intent to launch the MainActivity using the package name and class name
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("com.example.cubespeed", "com.example.cubespeed.MainActivity"));
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // Add an extra to indicate that the TimerScreen should be shown
            intent.putExtra("SHOW_TIMER_SCREEN", true);

            Log.d(TAG, "returnToMainActivity: Starting MainActivity with SHOW_TIMER_SCREEN extra");
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "returnToMainActivity: Error starting MainActivity", e);
            try {
                // Try a simpler approach as fallback
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.example.cubespeed", "com.example.cubespeed.MainActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } catch (Exception e2) {
                Log.e(TAG, "returnToMainActivity: Error with fallback approach", e2);
                // If all else fails, just finish this activity
                finish();
            }
        }
    }
}

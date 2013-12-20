package com.google.android.apps.pixelperfect;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import android.util.Log;

/**
 * Listens to {@link AccessibilityEvent}s triggered when there's a state
 * transition in the UI.
 */
public class AccessibilityEventService extends AccessibilityService {

    public static final String TAG = "PixelPerfect.AccessibilityEventService";

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
    }

    @Override
    public void onServiceConnected() {
        Log.v(TAG, "onServiceConnected " + getServiceInfo());
    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.v(TAG, "onAccessibilityEvent " + event);
    }

}

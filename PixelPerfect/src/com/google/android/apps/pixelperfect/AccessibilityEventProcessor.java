package com.google.android.apps.pixelperfect;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Processes {@link AccessibilityEvent}s:
 * <ul>
 *   <li> Filters out events that should not be published in Clearcut.
 *   <li> Publishes the surviving events.
 * </ul>
 *
 * <p>This class is state-less and thread safe.
 */
public class AccessibilityEventProcessor {

    private static final String TAG =
            "PixelPerfect.AccessibilityEventProcessor";

    private final ExcludedPackages mExcludedPackages;

    public AccessibilityEventProcessor(ExcludedPackages excludedPackages) {
        mExcludedPackages = excludedPackages;
    }

    /**
     * Processes an {@link AccessibilityEvent}.
     *
     * @param event {@link AccessibilityEvent} to process
     */
    public void process(AccessibilityEvent event) {
        if (mExcludedPackages.isExcluded(event.getPackageName().toString())) {
            Log.v(TAG, "Excluding package " + event.getPackageName());
            return;
        }

        Log.v(TAG, "Including package " + event.getPackageName()
                + " --> " + event.getEventType());
    }
}

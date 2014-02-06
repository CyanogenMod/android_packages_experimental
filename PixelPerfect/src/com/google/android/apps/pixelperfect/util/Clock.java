package com.google.android.apps.pixelperfect.util;

/**
 * Provides current value of time (now).
 */
public interface Clock {
    /**
     * Returns the current, absolute time in milliseconds, according to this clock.
     */
    public long nowMs();
}

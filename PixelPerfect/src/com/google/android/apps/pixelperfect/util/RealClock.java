package com.google.android.apps.pixelperfect.util;

import android.os.SystemClock;

/**
 * Implements {@link Clock} and provides nowMs according to SystemClock.
 */
public class RealClock implements Clock {
    @Override
    public long nowMs() {
        return SystemClock.elapsedRealtime();
    }
}
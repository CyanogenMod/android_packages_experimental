package com.google.android.apps.pixelperfect.testutil;

import com.google.android.apps.pixelperfect.util.Clock;

/**
 * A fake clock that implements {@link Clock} interface. Use for tests.
 */
public class FakeClock implements Clock {
    @Override
    public long nowMs() {
        return nowMs;
    }
    /** Set current time*/
    public void setTime(long nowMs) {
        this.nowMs = nowMs;
    }
    /** Advance current time*/
    public void advanceTime(long diffMs) {
        nowMs += diffMs;
    }
    private long nowMs = 0;
}

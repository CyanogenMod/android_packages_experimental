package com.google.android.apps.pixelperfect;

import android.test.suitebuilder.annotation.SmallTest;
import android.view.Surface;


import junit.framework.TestCase;

/**
 * Tests for {@link ScreenshotGrabber}.
 */
@SmallTest
public class ScreenshotGrabberTest extends TestCase {

    private static final float FLOAT_DELTA = 1e-5f;

    private ScreenshotGrabber mGrabber;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mGrabber = new ScreenshotGrabber();
    }

    public void testGetDegreesForRotation() {
        assertEquals(0f, mGrabber.getDegreesForRotation(Surface.ROTATION_0), FLOAT_DELTA);
        assertEquals(270f, mGrabber.getDegreesForRotation(Surface.ROTATION_90), FLOAT_DELTA);
        assertEquals(180f, mGrabber.getDegreesForRotation(Surface.ROTATION_180), FLOAT_DELTA);
        assertEquals(90f, mGrabber.getDegreesForRotation(Surface.ROTATION_270), FLOAT_DELTA);
    }
}

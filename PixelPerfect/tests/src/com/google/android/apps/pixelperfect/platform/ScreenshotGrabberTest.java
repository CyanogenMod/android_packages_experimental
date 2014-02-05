package com.google.android.apps.pixelperfect.platform;

import android.test.suitebuilder.annotation.SmallTest;
import android.view.Surface;

import com.google.common.logging.RecordedEvent;
import com.google.common.logging.RecordedEvent.Screenshot;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

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

    public void testFillScreenshotProto() throws Exception {
        final int height = 10;
        final int width = 15;
        final int rotation = 100;
        final int quality = 50;
        final byte[] bytes = new byte[] { (byte) 0xca, (byte) 0xfe };

        ByteArrayOutputStream output = new ByteArrayOutputStream(bytes.length);
        output.write(bytes, 0, bytes.length);
        ScreenshotGrabber grabber = new ScreenshotGrabber();
        Screenshot screenshot = grabber.fillScreenshotProto(
                output, height, width, rotation,
                RecordedEvent.Bitmap.BitmapConfig.Config.ARGB_8888,
                RecordedEvent.Bitmap.CompressionConfig.CompressFormat.JPEG,
                quality);

        assertEquals(ByteString.copyFrom(bytes), screenshot.getBitmap().getBitmap());
        assertEquals(height, screenshot.getBitmap().getHeight());
        assertEquals(width, screenshot.getBitmap().getWidth());
        assertEquals(rotation, screenshot.getRotation());

        assertEquals(RecordedEvent.Bitmap.BitmapConfig.Config.ARGB_8888,
                screenshot.getBitmap().getBitmapConfig().getValue());
        assertEquals(RecordedEvent.Bitmap.CompressionConfig.CompressFormat.JPEG,
                screenshot.getBitmap().getCompressionConfig().getFormat());
        assertEquals(quality, screenshot.getBitmap().getCompressionConfig().getQuality());
    }
}

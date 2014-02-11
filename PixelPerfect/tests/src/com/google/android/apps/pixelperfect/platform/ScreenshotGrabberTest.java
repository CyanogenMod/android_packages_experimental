package com.google.android.apps.pixelperfect.platform;

import android.annotation.SuppressLint;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Pair;

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

    private ScreenshotGrabber mGrabber;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mGrabber = new ScreenshotGrabber();
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

    public void testGetScreenshotDimensions() {
        // Base dimensions for the test. baseWidth * baseHeight = MAX_SCREENSHOT_NUM_PIXELS
        float baseWidth = (float) (Math.sqrt(ScreenshotGrabber.MAX_SCREENSHOT_NUM_PIXELS) / 2);
        float baseHeight = (float) (Math.sqrt(ScreenshotGrabber.MAX_SCREENSHOT_NUM_PIXELS) * 2);

        // Exact limits. No rescaling.
        assertDimensions(baseWidth, baseHeight, (int) baseWidth, (int) baseHeight);
        assertDimensions(baseHeight, baseWidth, (int) baseHeight, (int) baseWidth);

        // Under limits. No rescaling.
        final float under = 0.9f;
        float width = baseWidth * under;
        float height = baseHeight * under;
        assertDimensions(width, height, (int) width, (int) height);
        assertDimensions(height, width, (int) height, (int) width);

        // Over limits. Rescaling.
        final float over = 1.1f;
        width = baseWidth * over;
        height = baseHeight * over;
        float ratio = (float) Math.sqrt(
                ScreenshotGrabber.MAX_SCREENSHOT_NUM_PIXELS / width / height);
        assertDimensions(width, height, (int) (width * ratio), (int) (height * ratio));
        assertDimensions(height, width, (int) (height * ratio), (int) (width * ratio));
    }

    @SuppressLint("NewApi")  // For Pair.
    private void assertDimensions(float width, float height, int expectedWidth,
            int expectedHeight) {
        Pair<Integer, Integer> resolution = mGrabber.getScreenshotDimensions(width, height);
        assertEquals(expectedWidth, (Object) resolution.first);
        assertEquals(expectedHeight, (Object) resolution.second);
    }
}

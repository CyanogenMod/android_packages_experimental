
package com.google.android.apps.pixelperfect.platform;

import android.test.suitebuilder.annotation.SmallTest;
import android.view.Surface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.logging.RecordedEvent;
import com.google.common.logging.RecordedEvent.Screenshot;
import com.google.protobuf.ByteString;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

/**
 * Tests for {@link ScreenshotParcel}.
 */
@SmallTest
public class ScreenshotParcelTest extends TestCase {
    private Screenshot mScreenshotProto = null;
    private Parcel mParcel = null;

    private void createTestScreenshotProto() throws Exception {
        final int height = 10;
        final int width = 15;
        final int rotation = 100;
        final int quality = 50;
        final byte[] compressed_bytes = new byte[] {
                (byte) 0xca, (byte) 0xfe };

        ByteArrayOutputStream output = new ByteArrayOutputStream(
                compressed_bytes.length);
        output.write(compressed_bytes, 0, compressed_bytes.length);
        ScreenshotGrabber grabber = new ScreenshotGrabber();
        mScreenshotProto = grabber.fillScreenshotProto(
                output, height, width, rotation,
                RecordedEvent.Bitmap.BitmapConfig.Config.ARGB_8888,
                RecordedEvent.Bitmap.CompressionConfig.CompressFormat.JPEG,
                quality);

        // TODO(mukarram) figure out why ScreenshotParcel(mScreenshotProto)
        // does not work.
        ScreenshotParcel screenshotParcel = new ScreenshotParcel();
        screenshotParcel.screenshotProto = mScreenshotProto;

        Bundle bundle = new Bundle();
        bundle.putParcelable("screenshotParcel", screenshotParcel);
        mParcel = Parcel.obtain();
        bundle.writeToParcel(mParcel, 0);
    }

    public static void assertProtosEqual(Screenshot protoA, Screenshot protoB) {
        assertEquals(protoA.getRotation(), protoB.getRotation());
        assertEquals(protoA.getBitmap().getBitmap(), protoB.getBitmap().getBitmap());
        assertEquals(protoA.getBitmap().getHeight(), protoB.getBitmap().getHeight());
        assertEquals(protoA.getBitmap().getWidth(), protoB.getBitmap().getWidth());
        assertEquals(protoA.getRotation(), protoB.getRotation());

        assertEquals(protoA.getBitmap().getBitmapConfig().getValue(),
                protoB.getBitmap().getBitmapConfig().getValue());
        assertEquals(protoA.getBitmap().getCompressionConfig().getFormat(),
                protoB.getBitmap().getCompressionConfig().getFormat());
        assertEquals(protoA.getBitmap().getCompressionConfig().getQuality(),
                protoB.getBitmap().getCompressionConfig().getQuality());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTestScreenshotProto();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mParcel != null) {
            mParcel.recycle();
        }
    }

    public void testParcelReadWrite() {
        mParcel.setDataPosition(0);
        Bundle testBundle = mParcel.readBundle();
        testBundle.setClassLoader(ScreenshotParcelTest.class.getClassLoader());
        ScreenshotParcel testScreenshotParcel = testBundle.getParcelable("screenshotParcel");
        assertProtosEqual(mScreenshotProto, testScreenshotParcel.screenshotProto);
    }

}

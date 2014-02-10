package com.google.android.apps.pixelperfect.platform;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.logging.RecordedEvent;
import com.google.common.logging.RecordedEvent.Screenshot;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nullable;

/**
 * Takes screenshots of the device and saves them as JPG files or to protocol buffers.
 *
 * Usage:
 *      ScreenshotGrabber grabber = new ScreenshotGrabber();
 *      Pair<Bitmap, Integer> capture = grabber.takeScreenshot();
 *      Screenshot screenshotProto = grabber.makeScreenshotProto(capture);
 */
public class ScreenshotGrabber {

    private static final int JPEG_QUALITY = 50;

    private static final String TAG = "PixelPerfectPlatform.ScreenshotGrabber";

    /**
     * Takes a screenshot of the screen and returns the bitmap and screen rotation.
     *
     * @return an instance of ScreenshotCapture that contains the bitmap and
     *         rotation of screen at the time of capture. The function may
     *         return null if taking screenshot was not successfully.
     *         @throws IllegalStateException if for some reason rotation value
     *         obtained is invalid.
     */
    @Nullable public Pair<Bitmap, Integer> takeScreenshot() throws IllegalStateException {
        Log.v(TAG, "takeScreenshot()");
        Display display = DisplayManagerGlobal.getInstance()
                .getRealDisplay(Display.DEFAULT_DISPLAY);

        Point displaySize = new Point();
        display.getRealSize(displaySize);
        final int displayWidth = displaySize.x;
        final int displayHeight = displaySize.y;

        final float screenshotWidth;
        final float screenshotHeight;

        final int rotation = display.getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                screenshotWidth = displayWidth;
                screenshotHeight = displayHeight;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                screenshotWidth = displayHeight;
                screenshotHeight = displayWidth;
                break;
            default:
                throw new IllegalStateException("Invalid rotation: " + rotation);
        }

        // Take the screenshot
        Bitmap bitmap = SurfaceControl.screenshot((int) screenshotWidth,
                (int) screenshotHeight);
        if (bitmap == null) {
            return null;
        }

        // Optimization
        bitmap.setHasAlpha(false);
        return new Pair<Bitmap, Integer>(bitmap, rotation);
    }

    /**
     * Makes a Screenshot proto from the provided {@link Bitmap} and
     * {@code rotation}. Also performs compression, and saves compression
     * parameters in the proto.
     * @param capture the {@link Pair<Bitmap, Integer>} instance that is
     *            to be compressed and copied to protocol buffer.
     * @return the {@link Screenshot} proto which contains the bitmap. The
     *         function returns null if the provided capture param is null.
     */
    @Nullable public Screenshot makeScreenshotProto(
            @Nullable Pair<Bitmap, Integer> capture) {
        if (capture == null || capture.first == null) {
            return null;
        }

        // TODO(mukarram) Can we make following more efficient? Currently we
        // are writing to in in-memory output stream, then copying into the
        // proto. The copy can be eliminated if we could write directly to the
        // proto's byteString; not obvious how to do that.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        capture.first.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output);
        return fillScreenshotProto(
                output, capture.first.getHeight(), capture.first.getWidth(),
                capture.second,
                RecordedEvent.Bitmap.BitmapConfig.Config.ARGB_8888,
                // Following two are hard-wired for now. If we change our
                // compression methods, we may want to add a map from
                // graphics.Bitmap enums to the proto enums.
                RecordedEvent.Bitmap.CompressionConfig.CompressFormat.JPEG,
                JPEG_QUALITY);
    }

    /**
     * Helper function that creates and fills a Screenshot proto from the
     * provided compressed output stream and additional param about the
     * screenshot. Note: we split the helper to help with testing. Mockito
     * cannot mock graphics.Bitmap (because it is final).
     * @param compressed the {@link ByteArrayOutputStream} that has the
     *            compressed image.
     * @param height of the image
     * @param width of the image
     * @param rotation of the screen when the screenshot is taken
     * @param bitmapConfig is an enum from
     *            {@link RecordedEvent.Bitmap.BitmapConfig.Config} to put in
     *            proto. Not necessary when image is compressed, but we pass it
     *            anyway.
     * @param format is an enum from
     *            {@link RecordedEvent.Bitmap.CompressionConfig.CompressFormat}
     *            representing the compression format used on the bitmap.
     * @param quality is an integer representing the quality of compression
     *            used.
     * @return the {@link Screenshot} proto which contains the bitmap.
     */
    public Screenshot fillScreenshotProto(
            ByteArrayOutputStream compressed,
            int height, int width, int rotation,
            RecordedEvent.Bitmap.BitmapConfig.Config bitmapConfig,
            RecordedEvent.Bitmap.CompressionConfig.CompressFormat format,
            int quality) {
        // To avoid name collision between android.graphics.Bitmap and
        // com.google.common.logging.RecordedEvent.Bitmap, we refer to latter as
        // RecordedEvent.Bitmap
        RecordedEvent.Bitmap.Builder bitmapBuilder = RecordedEvent.Bitmap.newBuilder();
        // Save the compressed bitmap bytes and dimensions
        bitmapBuilder.setBitmap(ByteString.copyFrom(compressed.toByteArray()));
        bitmapBuilder.setHeight(height);
        bitmapBuilder.setWidth(width);

        // Save the bitmap configuration.
        RecordedEvent.Bitmap.BitmapConfig.Builder bitmapConfigBuilder =
                RecordedEvent.Bitmap.BitmapConfig.newBuilder();
        bitmapConfigBuilder.setValue(bitmapConfig);
        bitmapBuilder.setBitmapConfig(bitmapConfigBuilder.build());

        // Save the compression configuration.
        RecordedEvent.Bitmap.CompressionConfig.Builder compressionConfigBuilder =
                RecordedEvent.Bitmap.CompressionConfig.newBuilder();
        compressionConfigBuilder.setFormat(format);
        compressionConfigBuilder.setQuality(quality);
        bitmapBuilder.setCompressionConfig(compressionConfigBuilder.build());

        // Build screenshot.
        Screenshot.Builder screenshotBuilder = Screenshot.newBuilder();
        screenshotBuilder.setBitmap(bitmapBuilder.build());
        screenshotBuilder.setRotation(rotation);
        return screenshotBuilder.build();
    }

    /**
     * Saves a {@link Bitmap} as a JPG file.
     *
     * @param bitmap the {@link Bitmap}
     * @param filepath the file path
     */
    public void saveBitmap(Bitmap bitmap, String filepath) {
        // NOTE(stlafon): For the sake of debugging, if no file path is passed,
        // we save the
        // screenshot on the the external storage, as "screenshot.jpg".
        // TODO(stlafon): Remove this.
        if (filepath == null) {
            filepath = Environment.getExternalStorageDirectory().toString() + "/"
                    + "screenshot.jpg";
        }

        Log.v(TAG, "Saving screenshot at " + filepath);

        OutputStream fout = null;
        File imageFile = new File(filepath);
        try {
            fout = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fout);
            fout.flush();

            Log.v(TAG, "Screenshot saved");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to save screenshot " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Unable to save screenshot " + e.getMessage());
        } finally {
            try {
                fout.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to save screenshot " + e.getMessage());
            }
        }
    }

    @VisibleForTesting
    float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
            default:
                return 0;
        }
    }
}

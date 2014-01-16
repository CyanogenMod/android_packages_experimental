package com.google.android.apps.pixelperfect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Takes screenshots of the device and saves them as JPG files.
 */
public class ScreenshotGrabber {

    private static final int JPEG_QUALITY = 90;
    private static final String TAG = "PixelPerfect.ScreenshotGrabber";

    /**
     * Takes a screenshot of the screen.
     *
     * @return the {@link Bitmap} containing the screenshot, or {@code null} if one couldn't take
     * the screenshot
     */
    public Bitmap takeScreenshot() {

        Log.e(TAG, "takeScreenshot()");

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
                throw new IllegalArgumentException("Invalid rotation: " + rotation);
        }

        // Take the screenshot
        Bitmap screenShot = SurfaceControl.screenshot((int) screenshotWidth,
                (int) screenshotHeight);
        if (screenShot == null) {
            return null;
        }

        // Rotate the screenshot to the current orientation
        if (rotation != Surface.ROTATION_0) {
            Bitmap unrotatedScreenShot = Bitmap.createBitmap(displayWidth, displayHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(unrotatedScreenShot);
            canvas.translate(unrotatedScreenShot.getWidth() / 2,
                    unrotatedScreenShot.getHeight() / 2);
            canvas.rotate(getDegreesForRotation(rotation));
            canvas.translate(- screenshotWidth / 2, - screenshotHeight / 2);
            canvas.drawBitmap(screenShot, 0, 0, null);
            canvas.setBitmap(null);
            screenShot = unrotatedScreenShot;
        }

        // Optimization
        screenShot.setHasAlpha(false);

        return screenShot;
    }

    /**
     * Saves a {@link Bitmap} as a JPG file.
     *
     * @param bitmap the {@link Bitmap}
     * @param filepath the file path
     */
    public void saveBitmap(Bitmap bitmap, String filepath) {
        // NOTE(stlafon): For the sake of debugging, if no file path is passed, we save the
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

package com.google.android.apps.pixelperfect.api;
import com.google.android.apps.pixelperfect.api.ScreenshotParcel;

/**
 * PixelPerfectPlatform service interface.  This service allows appropriately
 * authorized packages to obtain certain signals that are only allowed to
 * platform-signed apps.  The idea is that the service is implemented in a
 * platform-signed apk.
 */
interface IPixelPerfectPlatform {
    /**
     * Takes a screenshot
     *
     * @return screenshot {@link ScreenshotParcel} if successful.
     * Exceptions, such as SecurityException if client is not authorized, are parceled back.
     */
    ScreenshotParcel getScreenshot() = 1;
    // TODO(mukarram) Add an interface that allows obtaining clips of a
    // screenshot and controlling whether compression is performed or not.
}
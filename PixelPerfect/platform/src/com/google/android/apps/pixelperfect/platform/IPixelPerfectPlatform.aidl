// IPixelPerfectPlatform.aidl
// TODO(mukarram) Put aidl files in a common library project that the two apks can share.
package com.google.android.apps.pixelperfect.platform;
import com.google.android.apps.pixelperfect.platform.ScreenshotParcel;

/**
 * PixelPerfectPlatform service interface.  This service allows appropriately
 * authorized packages to obtain certain signals that are only allowed to
 * platform-signed apps.  The idea is that the service is implemented in a
 * platform-signed apk.
 */
interface IPixelPerfectPlatform {
    /**
     * Determines whether the pixel perfect platform interface is available to
     * the calling activity. Specifically, we verify that the package of the
     * calling activity is signed by Google.
     *
     * @return true if available, false if not.
     */
    boolean isPlatformAvailable() = 0;

    /**
     * Takes a screenshot
     *
     * @return screenshot {@link ScreenshotParcel} if successful. @throws
     * RemoteException if not successful, such as because the calling package
     * is not allowed to make the call or if fails to capture the screens.
     */
    ScreenshotParcel getScreenshot() = 1;
    // TODO(mukarram) Add an interface that allows obtaining clips of a
    // screenshot and controlling whether compression is performed or not.
}
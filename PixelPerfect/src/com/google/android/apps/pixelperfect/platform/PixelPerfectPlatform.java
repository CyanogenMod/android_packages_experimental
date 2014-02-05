package com.google.android.apps.pixelperfect.platform;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.google.android.apps.pixelperfect.api.IPixelPerfectPlatform;
import com.google.android.apps.pixelperfect.api.ScreenshotParcel;

/**
 * PixelPerfectPlatform service provides binding to IPixelPerfectPlatform.Stub.
 *
 * @see com.google.android.apps.pixelperfect.platform.IPixelPerfectPlatform for
 *      the interface.
 */
public class PixelPerfectPlatform extends Service {
    public class PixelPerfectPlatformStubImpl extends IPixelPerfectPlatform.Stub {

        @Override
        public boolean isPlatformAvailable() throws RemoteException {
            // TODO(mukarram): verify the certificate of the calling
            // application.
            return false;
        }

        @Override
        public ScreenshotParcel getScreenshot() throws RemoteException {
            Log.v(TAG, "getScreenshot() called.");
            if (!isPlatformAvailable()) {
                throw new RemoteException("Calling package is not allowed.");
            }
            ScreenshotGrabber grabber = new ScreenshotGrabber();
            Pair<Bitmap, Integer> capture = grabber.takeScreenshot();
            if (capture == null) {
                throw new RemoteException("Could not capture screenshot.");
            }
            ScreenshotParcel parcel = new ScreenshotParcel();
            parcel.screenshotProto = grabber.makeScreenshotProto(capture);
            return parcel;
        }
    }

    private final PixelPerfectPlatformStubImpl mBinder = new PixelPerfectPlatformStubImpl();
    private static final String TAG = "PixelPerfect.PlatformService";

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}

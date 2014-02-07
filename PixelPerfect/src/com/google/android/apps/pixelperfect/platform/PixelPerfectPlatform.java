package com.google.android.apps.pixelperfect.platform;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import com.google.android.apps.pixelperfect.api.IPixelPerfectPlatform;
import com.google.android.apps.pixelperfect.api.ScreenshotParcel;

import javax.annotation.Nullable;

/**
 * PixelPerfectPlatform service provides binding to IPixelPerfectPlatform.Stub.
 * @see com.google.android.apps.pixelperfect.api.IPixelPerfectPlatform for
 *      the interface.
 */
public class PixelPerfectPlatform extends Service {
    private final String TAG = "PixelPerfectPlatform.PlatformService";
    public class PixelPerfectPlatformStubImpl extends IPixelPerfectPlatform.Stub {
        @Override
        public boolean isPlatformAvailable() {
            // TODO(mukarram): verify the certificate of the calling
            // application.  Coming in follow up CL.
            return true;
        }

        @Override
        @Nullable
        public ScreenshotParcel getScreenshot() {
            ScreenshotParcel parcel = new ScreenshotParcel();
            Log.v(TAG, "getScreenshot() called.");
            if (!isPlatformAvailable()) {
                Log.e(TAG, "Calling package is not allowed.");
                parcel.setException(new SecurityException("Calling package is not allowed."));
                return parcel;
            }
            ScreenshotGrabber grabber = new ScreenshotGrabber();
            Pair<Bitmap, Integer> capture = grabber.takeScreenshot();
            if (capture == null) {
                return parcel;
            }

            parcel.screenshotProto = grabber.makeScreenshotProto(capture);
            return parcel;
        }
    }

    private final PixelPerfectPlatformStubImpl mBinder = new PixelPerfectPlatformStubImpl();

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return mBinder;
    }

}

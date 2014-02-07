package com.google.android.apps.pixelperfect.platform;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.google.android.apps.pixelperfect.api.IPixelPerfectPlatform;
import com.google.android.apps.pixelperfect.api.ScreenshotParcel;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.collect.Sets;

import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * PixelPerfectPlatform service provides binding to IPixelPerfectPlatform.Stub.
 * @see com.google.android.apps.pixelperfect.api.IPixelPerfectPlatform for
 *      the interface.
 */
public class PixelPerfectPlatform extends Service {
    private final String TAG = "PixelPerfectPlatform.PlatformService";
    private static final Set<String> PACKAGE_WHITELIST = Sets.newHashSet(
            "com.google.android.apps.pixelperfect");
    /**
     * Check whether the provided package is in the hard-coded white list.
     * @param callingPackage {@link String} with fully qualified package name.
     * @return true if the package name is in the white list.
     */
    private boolean packageIsWhitelisted(String callingPackage) {
        return PACKAGE_WHITELIST.contains(callingPackage.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Using {@link GooglePlayServicesUtil}, verifies that the calling package
     * has a google certificate.
     * @param callingPackage {@link String} with fully qualified package name.
     * @returns true if the calling package has the requisite certificate.
     * @throws SecurityException if package is not Google signed.
     */
    private boolean verifyPackageIsGoogleSigned(String callingPackage) throws SecurityException {
        // Note: we are violating @ShowFirstParty annotation on verifyPackageIsGoogleSigned
        GooglePlayServicesUtil.verifyPackageIsGoogleSigned(
                getPackageManager(), callingPackage);
        return true;
    }

    public class PixelPerfectPlatformStubImpl extends IPixelPerfectPlatform.Stub {
        private String getCallingPackageName() {
            // Android security model:
            // "...each application runs with a distinct system identity
            // (Linux user ID and group ID)"
            // http://developer.android.com/guide/topics/security/permissions.html
            return getPackageManager().getNameForUid(getCallingUid());
        }

        public boolean ensureCallingPackageIsAuthorized() {
            final String callingPackage = getCallingPackageName();
            return packageIsWhitelisted(callingPackage) &&
                    verifyPackageIsGoogleSigned(callingPackage);
        }

        @Override
        @Nullable
        public ScreenshotParcel getScreenshot() {
            final String callingPackage = getCallingPackageName();
            Log.v(TAG, "getScreenshot() called by " + callingPackage);
            ScreenshotParcel parcel = new ScreenshotParcel();
            try {
                ensureCallingPackageIsAuthorized();
            } catch (SecurityException e) {
                Log.e(TAG, "Call from non-google-signed package: " + callingPackage, e);
                parcel.setException(new SecurityException("Calling package is not authorized."));
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
        super.onCreate();
        Log.v(TAG, "onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return mBinder;
    }

}

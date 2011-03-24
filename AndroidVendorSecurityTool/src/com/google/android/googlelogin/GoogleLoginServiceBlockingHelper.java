/**
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.google.android.googlelogin;

import com.google.android.googleapps.IGoogleLoginService;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.Looper;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A helper designed for use by services and background tasks.
 * This class provides blocking calls that wrap the binding to the
 * {@link IGoogleLoginService GoogleLoginService} and the various
 * calls to it.  (Be sure not to call these blocking methods
 * from your main thread, though; you always need to create a separate
 * worker thread for operations that may block.)
 * <p>
 * It's best to instantiate this class once
 * and make calls on that instance when necessary.
 * The helper does not unbind from the GoogleLoginService after
 * each call.
 * <p>
 * When you are done with this object, call close() to unbind from the
 * GoogleLoginService.
 */
public class GoogleLoginServiceBlockingHelper {
    private static final String TAG = "GoogleLoginServiceBlockingHelper";

    private final Context mContext;
    private volatile IGoogleLoginService mGoogleLoginService = null;
    private Lock mGoogleLoginServiceLock = new ReentrantLock();
    private Condition mBindWaitCondition = mGoogleLoginServiceLock.newCondition();
    private ServiceConnection mServiceConnection;

    private final int mMinDelaySecs;
    private final int mMaxDelaySecs;
    private final double mBackoffFactor;
    private int mDelay;

    /**
     * Whether the Google login service we've bound to is the
     * Google-provided service. This will be set after we get a callback on the
     * service connection, so the value is only valid if
     * {@link #mGoogleLoginService} is not null.
     * <p>
     * Locked with the {@link #mGoogleLoginServiceLock} also.
     */
    private boolean mGlsVerified;

    /**
     * Initializes the helper.
     * @param context the Context in which this helper is running
     * @throws GoogleLoginServiceNotFoundException if the Google login service cannot be found.
     */
    public GoogleLoginServiceBlockingHelper(Context context)
            throws GoogleLoginServiceNotFoundException {
        mMinDelaySecs = 5;
        mMaxDelaySecs = 5 * 60;   // 5 minutes
        mBackoffFactor = 2.0;
        mDelay = mMinDelaySecs;
        mContext = context;

        // Ensure the Google Login Service is available
        if (!GoogleAppsVerifier.isServiceAvailable(context,
                GoogleLoginServiceConstants.FULLY_QUALIFIED_SERVICE_NAME)) {
            throw new GoogleLoginServiceNotFoundException(
                    GoogleLoginServiceConstants.ERROR_CODE_GLS_NOT_FOUND);
        }
        
        mGoogleLoginServiceLock.lock();
        try {
            mServiceConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder service) {
                    try {
                        mGoogleLoginServiceLock.lock();

                        /*
                         * Verify that the service we just connected to is
                         * provided by Google. Eventually, this will manifest
                         * into an exception, but we can't throw it here because
                         * our client isn't in the call stack right now.
                         */
                        mGlsVerified = GoogleAppsVerifier.isGoogleAppsVerified(mContext);
                        
                        mGoogleLoginService = IGoogleLoginService.Stub.asInterface(service);
                        
                        mBindWaitCondition.signalAll();
                    } finally {
                        mGoogleLoginServiceLock.unlock();
                    }
                }

                public void onServiceDisconnected(ComponentName className) {
                    mGoogleLoginServiceLock.lock();
                    mGoogleLoginService = null;
                    mGoogleLoginServiceLock.unlock();
                }
            };

            if (!mContext.bindService(GoogleLoginServiceConstants.SERVICE_INTENT,
                                 mServiceConnection, Context.BIND_AUTO_CREATE)) {
                throw new GoogleLoginServiceNotFoundException(
                        GoogleLoginServiceConstants.ERROR_CODE_GLS_NOT_FOUND);
            }
        } finally {
            mGoogleLoginServiceLock.unlock();
        }
    }

    /**
     * Releases the binding to the GoogleLoginService (if one exists). This object
     * is no longer usable one this method is invoked.
     */
    public void close() {
        mGoogleLoginServiceLock.lock();
        try {
            if (mServiceConnection != null) {
                mContext.unbindService(mServiceConnection);
                mServiceConnection = null;
                mGoogleLoginService = null;
            }
        } finally {
            mGoogleLoginServiceLock.unlock();
        }
    }

    /**
     * Sleep for an exponentially-increasing length of time (bounded
     * by mMinDelaySecs and mMaxDelaySecs).
     */
    private void delay() {
        try {
            Thread.sleep(mDelay * 1000L);
        } catch (InterruptedException ignore) {
            // just delay for less time
        }
        mDelay *= mBackoffFactor;
        if (mDelay > mMaxDelaySecs) mDelay = mMaxDelaySecs;
    }

    /**
     * Reset the sleep time used by delay() to the minimum.
     */
    private void resetDelay() {
        mDelay = mMinDelaySecs;
    }

    /**
     * Waits for mGoogleLoginService to be nun-null and then returns it. It is set in the
     * onServiceConnected that is called as a result of the bind that is called by the
     * constructor.
     * @return the GoogleLoginService, guaranteed to be non-null
     * @throws GoogleLoginServiceNotFoundException if the Google login service cannot be found.
     */
    private IGoogleLoginService getLoginService() throws GoogleLoginServiceNotFoundException {
        if (Looper.myLooper() == mContext.getMainLooper()) {
            throw new IllegalStateException("calling GoogleLoginServiceBlockingHelper methods "
                    + "from your main thread can lead to deadlock");
        }
        try {
            mGoogleLoginServiceLock.lock();
            while (mGoogleLoginService == null) {
                try {
                    mBindWaitCondition.await();
                } catch (InterruptedException e) {
                    // keep waiting
                }
            }

            checkGoogleLoginServiceVerificationLocked();
            
            return mGoogleLoginService;
        } finally {
            mGoogleLoginServiceLock.unlock();
        }
    }

    private void checkGoogleLoginServiceVerificationLocked()
            throws GoogleLoginServiceNotFoundException {
        if (mGoogleLoginService != null && !mGlsVerified) {
            throw new GoogleLoginServiceNotFoundException(
                    GoogleLoginServiceConstants.ERROR_CODE_GLS_VERIFICATION_FAILED);
        }
    }

    /**
     * Gets the login service via getLoginService, which may block, and then
     * invokes getAndroidId on it.
     *
     * @see IGoogleLoginService#getAndroidId()
     * @return the Android ID for this device (a 64-bit value unique to this
     * device); 0 if the device is not registered with google or if the Android
     * ID is otherwise unavailable.
     * @throws GoogleLoginServiceNotFoundException if the Google login service cannot be found.
     */
    public static long getAndroidId(Context context) throws GoogleLoginServiceNotFoundException {
        GoogleLoginServiceBlockingHelper h = new GoogleLoginServiceBlockingHelper(context);
        try {
            return h.getAndroidId();
        } finally {
            h.close();
        }
    }

    /**
     * Gets the login service via getLoginService, which may block, and then
     * invokes getAndroidId on it.
     *
     * @see IGoogleLoginService#getAndroidId()
     * @return the Android ID for this device (a 64-bit value unique to this
     * device); 0 if the device is not registered with google or if the Android
     * ID is otherwise unavailable.
     * @throws GoogleLoginServiceNotFoundException if the Google login service cannot be found.
     */
    public long getAndroidId() throws GoogleLoginServiceNotFoundException {
        resetDelay();
        while (true) {
            IGoogleLoginService loginService = getLoginService();
            try {
                return loginService.getAndroidId();
            } catch (RemoteException e) {
                // the next call to getLoginService will wait until the service
                // is reconnected
                delay();
            }
        }
    }
}

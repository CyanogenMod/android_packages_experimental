package com.google.android.apps.pixelperfect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.apps.pixelperfect.util.Clock;
import com.google.android.apps.pixelperfect.api.IPixelPerfectPlatform;
import com.google.android.apps.pixelperfect.api.ScreenshotParcel;
import com.google.common.base.Preconditions;
import com.google.common.logging.RecordedEvent.Screenshot;

import javax.annotation.Nullable;

/**
 * Client for interacting with PixelPerfectPlatform service.
 */
public class PlatformServiceClient {
    /**
     * Constructor. Creates a new client object and binds PixelPerfectPlatform
     * service.
     * @param context {@link Context} of the calling application.
     * @param clock {@link Clock} used for time stamps of connection and disconnection times.
     */
    public PlatformServiceClient(Context context, Clock clock) {
        // TODO(mukarram) figure out whether a singleton would suffice.
        // TODO(mukarram) figure out whether we need to synchronize on mPlatformService
        Preconditions.checkNotNull(
                context, "Context must not be null.",
                clock, "Clock must not be null");
        mContext = context;
        mClock = clock;
        initializeTimestamps();
        connectAndBindPlatformService();
    }

    /**
     * Obtains a screenshot by making an IPC to the PixelPerfectPlatform service.
     *
     * Note: calling package must be google signed AND white-listed in
     * PixelPerfectPlatform, else a security exception is parceled and thrown
     * while reading the screenshot.
     *
     * @return {@link Screenshot} proto instance that is returned by the
     * PixelPerfectPlatformService. May return null if the returned parcel is as such.
     */
    public @Nullable Screenshot obtainScreenshot() throws RemoteException {
        Log.v(TAG, "Attempting getScreenshot()");
        // TODO(mukarram) Consider adding throttling for screenshots.
        // Also consider whether such throttling makes sense here on the platform service itself.
          ScreenshotParcel parcel = getPlatformServiceIfAvailable().getScreenshot();
          if (parcel == null) {
              return null;
          }
          return parcel.screenshotProto;
    }

    private static final String TAG = "PixelPerfect.PlatformServiceClient";

    /** Whether we have binding to the platform service. */
    private final Context mContext;
    /** Reference to PixelPerfectPlatform service interface. */
    private IPixelPerfectPlatform mPlatformService = null;

    // Following time stamps are used for throttling the re-connection attempts
    // should we loose connection to the service.
    /** Time stamp for when the object is created. */
    private long mLastConnectionAttemptTimeMs = 0;
    /** Time stamp for when we are last connected from the platform service. */
    private long mLastConnectTimeMs = 0;
    /** Time stamp for when we are last disconnected to the platform service. */
    private long mLastDisconnectTimeMs = 0;
    /** Clock used for time stamps.*/
    private Clock mClock = null;
    /** Minimum time (in milliseconds) before attempting a reconnect.*/
    private int mMinTimeMillsecToReconnect = 0;

    /**
     * Implements ServiceConnection to PixelPerfectPlatformService. Uses the
     * onServiceConnected() and onServiceDisconnected callbacks to update a
     * reference to PixelPerfectPlatform service interface.
     */
    class PlatformServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mPlatformService = IPixelPerfectPlatform.Stub.asInterface(service);
            mLastConnectTimeMs = mClock.nowMs();
            Log.v(TAG, "onServiceConnected: " + className.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mPlatformService = null;
            mLastDisconnectTimeMs = mClock.nowMs();
            Log.v(TAG, "onServiceDisconnected " + className.toString());
        }
    }

    /** Connection to the PixelPerfectPlatform service. */
    private PlatformServiceConnection mConnection = null;

    /**
     * Helper to bind to PixelPerfectPlatform service if not already bound.
     */
    private void bindPlatformService() {
        Log.v(TAG, "Attempting a bind to platform service at: " + mLastConnectionAttemptTimeMs);
        if (mPlatformService != null) {
            Log.v(TAG, "We are already bound to platform service. Do nothing.");
            return;
        }

        mContext.bindService(
                // TODO(mukarram) Use explicit intent.
                // We are getting warnings of sort:
                // 02-05 18:07:08.907 W/ContextImpl(16804):
                // Implicit intents with startService are not safe:
                // Intent { act=com.google.android.apps.pixelperfect.api.IPixelPerfectPlatform }
                // android.content.ContextWrapper.bindService:513
                // com.google.android.apps.pixelperfect.
                // PlatformServiceClient.bindPlatformService:51
                // com.google.android.apps.pixelperfect.PlatformServiceClient.<init>:71
                //
                // We tried:
                // Intent intent = new Intent();
                // intent.setClass(mContext, IPixelPerfectPlatform.class);
                // and use that instead of new Intent(IPixelPerfectPlatform.class.getName()),
                // but that did not work out.
                // Still investigating.
                new Intent(IPixelPerfectPlatform.class.getName()),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Gets binding to PixelPerfectPlatform service if available. If service was
     * for some reason disconnected, then attempts to reconnect.
     * @return binding to PixelPerfectPlatform service if connected and bound.
     * @throws IllegalStateException if not bound.
     */
    private IPixelPerfectPlatform getPlatformServiceIfAvailable() {
        if (mPlatformService != null) {
            return mPlatformService;
        }
        // We do not have a stub to platform service.
        // See if it is okay to attempt a reconnect, if so fire off a reconnect attempt.
        if (isThrottledConnectionRetryOkay()) {
            Log.v(TAG, "Attempting a reconnect to platform service.");
            // Note: following call does not block for connection to
            // complete, therefore we cannot return a reference to
            // platform service here, so although we are initiating a
            // connection here, we still throw an exception.
            // Recall from above that when connection completes,
            // onServiceConnected() would be called in PlatformServiceConnection
            connectAndBindPlatformService();
        } else {
            Log.v(TAG,
                    "Disconnected from platform service, but not ready to reconnect yet."
                    + "mLastConnectAttemptMs :" + mLastConnectionAttemptTimeMs
                    + " time since:" + (mClock.nowMs() - mLastConnectionAttemptTimeMs));
        }
        // Throw an exception anyway, because we were not ready when get..() was called.
        throw new IllegalStateException(
                "Client not connected to platform service."
                + " Connected at: " + mLastConnectTimeMs
                + " Disconnected at: " + mLastDisconnectTimeMs);
    }

    /**
     * Create a connection to PixelPerfectPlatform service if not already
     * connected, and then bind, if not already bound.
     */
    private void connectAndBindPlatformService() {
        // TODO(mukarram) Figure out whether we need to reestablish connection,
        // or whether we can reuse the connection.
        if (mConnection == null) {
            mConnection = new PlatformServiceConnection();
        }
        mLastConnectionAttemptTimeMs = mClock.nowMs();
        bindPlatformService();
    }

    private void initializeTimestamps() {
        Resources res = mContext.getResources();
        mMinTimeMillsecToReconnect = res.getInteger(
                R.integer.min_time_ms_between_platform_service_reconnect);
        mLastConnectTimeMs = 0;
        mLastDisconnectTimeMs = 0;
    }

    /**
     * Returns true if it is okay to attempt a reconnect.  We keep track of
     * time stamps of when connection attempts are made and when connection
     * disconnections occur, and throttle reconnection attempts such that there
     * is at least mMinTimeMillsecToReconnect
     * (R.integer.min_time_ms_between_platform_service_reconnect) is elapsed
     * since last disconnect or between reconnect attempts.
     * @return true if it is okay to retry, false if not.
     */
    private boolean isThrottledConnectionRetryOkay() {
        final long nowMs = mClock.nowMs();
        // If there has been at least one disconnect
        // (i.e., mLastDisconnectTime > 0), then see if enough time has elapsed
        // since last disconnect.
        if (mLastDisconnectTimeMs > 0 &&
            nowMs - mLastDisconnectTimeMs < mMinTimeMillsecToReconnect) {
            return false;
        }
        // See if enough time has elapsed since last connection attempt.
        if (nowMs - mLastConnectionAttemptTimeMs < mMinTimeMillsecToReconnect) {
            return false;
        }
        return true;
    }
}

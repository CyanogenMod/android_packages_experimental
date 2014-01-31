package com.google.android.apps.pixelperfect;

import com.google.android.apps.pixelperfect.preferences.PreferencesActivity;
import com.google.android.gms.playlog.PlayLogger;

import android.accessibilityservice.AccessibilityService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;

/**
 * Listens to {@link AccessibilityEvent}s triggered when there's a state
 * transition in the UI.
 */
public class AccessibilityEventService extends AccessibilityService
        implements PlayLogger.LoggerCallbacks {

    private static final String TAG = "PixelPerfect.AccessibilityEventService";

    /** Action for pausing the recording. */
    @VisibleForTesting
    static final String ACTION_PAUSE = "com.google.android.apps.pixelperfect.PAUSE";

    /** Action for resuming the recording. */
    @VisibleForTesting
    static final String ACTION_RESUME = "com.google.android.apps.pixelperfect.RESUME";

    /** Type used to identify a Google corp account. */
    private static final String CORP_ACCOUNT_TYPE = "com.google";

    /** The unique id for the sticky notification. */
    private static final int NOTIFICATION_ID = 0;

    /**
     * If true, then we don't record anything (screenshots, accessibility events...). This allows
     * users to go enter the incognito mode.
     */
    private static boolean sIsPaused;

    /** A {@link Toast} shown when the service is paused or resumed. */
    private static Toast sToast;

    /**
     * The event processor. If the device doesn't have a Google corp account,
     * then this variable will be {@code null}, and no publishing in Clearcut
     * will occur.
     */
    private AccessibilityEventProcessor mProcessor;

    /** The excluded packages. */
    private ExcludedPackages mExcludedPackages;

    /** Used to create the sticky notification. */
    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        // The publishing of accessibility events in Clearcut is only enabled
        // if the device has a Google corp account.
        String accountName = getCorpAccountName();
        if (accountName != null) {
            // No need to synchronize this as the #onCreate method will only be
            // called when the service is enabled in the settings, so it should
            // be safe from concurrency issues.
            try {
                mExcludedPackages = ExcludedPackages.getInstance(this);
                mProcessor = new AccessibilityEventProcessor(this, accountName, mExcludedPackages,
                    this);
            } catch (Exception e) {
                int msgId = ((e instanceof IllegalStateException)
                        && e.getMessage().contains("com.google.android.gms.version"))
                                ? R.string.gmscore_version_error
                                : R.string.initialization_error;
                Log.e(TAG, "Failed to initialize PixelPerfect", e);
                showToast(msgId);
                stopSelf();
                return;
            }
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            setIsPaused(false);
            showToast(R.string.pixelperfect_running);
        }
    }

    // TODO(stlafon): Understand why this method is never called after the onStartCommand() method
    // is invoked (e.g. when the user clicks on pause/resume in the notification). The problem is
    // that the notification doesn't get canceled if the following sequence happens:
    // Enable service in settings -> Click on pause in notification -> Disable service in settings
    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        // Cancel the sticky notification.
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }

        // Inform the user that PixelPerfect is no longer running.
        showToast(R.string.pixelperfect_not_running);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        if (intent != null) {
            if (ACTION_PAUSE.equals(intent.getAction())) {
                setIsPaused(true);
            } else if (ACTION_RESUME.equals(intent.getAction())) {
                setIsPaused(false);
            }
        }

        // The following will not stop the service, as the latter is not started by a call to
        // startService(). Rather, it is enabled from the system settings. Calling stopSelf() here
        // undoes the effect of onStartCommand(), which gets called each time the user clicks on
        // pause/resume in the notification.
        stopSelf();

        // We want this service to continue running until it is explicitly stopped,
        // so return sticky.
        return START_STICKY;
    }

    @Override
    public void onServiceConnected() {
        Log.v(TAG, "onServiceConnected " + getServiceInfo());
    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mProcessor != null && !getIsPaused()) {
            mProcessor.process(event);
        }
    }

    /**
     * Atomically:
     * <ul>
     *   <li> sets the value of {@link #sIsPaused},
     *   <li> updates the sticky notification,
     *   <li> shows a toast informing the user of the new state.
     * </ul>
     *
     * <p>This does not stop the {@link AccessibilityService}. It just prevents us from recording
     * any event. To stop the service, users need to go to the system settings and manually disable.
     * Note, calling {@link #stopSelf()} in this method doesn't seem to do anything.
     *
     * @param isPaused whether the recording is paused or not
     */
    @VisibleForTesting
    synchronized void setIsPaused(boolean isPaused){
        sIsPaused = isPaused;
        mNotificationManager.notify(NOTIFICATION_ID, createNotification(sIsPaused));

        int msgId = sIsPaused ? R.string.user_in_incognito : R.string.user_not_incognito;
        showToast(msgId);
    }

    @VisibleForTesting
    synchronized boolean getIsPaused() {
        return sIsPaused;
    }

    /**
     * If a Google Corp account is linked to this device, returns the account name. Otherwise,
     * returns {@code null}.
     */
    private String getCorpAccountName() {
        AccountManager manager = AccountManager.get(this);
        return getCorpAccountNameImpl(manager.getAccounts());
    }

    /** Same as {@link #getCorpAccountName()}, but acts on an array of {@link Account}s. */
    @VisibleForTesting
    String getCorpAccountNameImpl(Account[] accounts) {
        String accountName = null;
        for (Account account : accounts) {
            if (CORP_ACCOUNT_TYPE.equals(account.type)) {
                accountName = account.name;
            }
        }
        return accountName;
    }

    /**
     * Creates and returns a permanent (sticky) notification that features two buttons:
     * <ul>
     *   <li> A button to pause/resume data recording (pausing == going incognito).
     *   <li> A button to launch the preferences activity, where users can set a blacklist of apps.
     * </ul>
     *
     * @param isPaused whether recording is currently paused or not
     * @return the {@link Notification}
     */
    @VisibleForTesting
    Notification createNotification(boolean isPaused) {
        int smallIconId = isPaused ? R.drawable.ic_no_cat : R.drawable.ic_cat;

        Intent intent = new Intent(this, PreferencesActivity.class);
        PendingIntent prefsPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        intent = new Intent(this, AccessibilityEventService.class)
            .setAction(isPaused ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, 0);

        CharSequence pauseOrResume = isPaused ? getText(R.string.resume) : getText(R.string.pause);
        int pauseOrResumeIcon = isPaused ? R.drawable.ic_resume : R.drawable.ic_pause;
        CharSequence notifDescription =
                isPaused ? getText(R.string.is_paused) : getText(R.string.is_running);

        return new Notification.Builder(this)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(notifDescription)
            .setSmallIcon(smallIconId)
            .setOngoing(true)  // cannot be dismissed
            .addAction(pauseOrResumeIcon, pauseOrResume, servicePendingIntent)
            .addAction(R.drawable.ic_preferences, getText(R.string.notif_preferences),
                    prefsPendingIntent)
            .build();
    }

    /** Shows a {@link Toast}. */
    private void showToast(int msgId) {
        if (sToast == null) {
            sToast = Toast.makeText(this, msgId, Toast.LENGTH_SHORT);
        } else {
            sToast.setText(msgId);
        }
        sToast.show();
    }

    @Override
    public void onLoggerConnected() {
        Log.v(TAG, "PlayLogger connected");
    }

    @Override
    public void onLoggerFailedConnectionWithResolution(PendingIntent resolutionIntent) {
        // TODO(dprothro): call the PendingIntent to see if the user will approve
        // usage of the PlayLogger.
        Log.e(TAG, "Failed to initialize PixelPerfect - loggerFailedConnectionWithResolution");
        showToast(R.string.initialization_error);
        stopSelf();
    }

    @Override
    public void onLoggerFailedConnection() {
        Log.e(TAG, "Failed to initialize PixelPerfect - loggerFailedConnection");
        showToast(R.string.initialization_error);
        stopSelf();
    }
}

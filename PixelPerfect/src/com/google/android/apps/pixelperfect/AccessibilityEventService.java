package com.google.android.apps.pixelperfect;

import android.accessibilityservice.AccessibilityService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.view.accessibility.AccessibilityEvent;

import android.util.Log;

/**
 * Listens to {@link AccessibilityEvent}s triggered when there's a state
 * transition in the UI.
 */
public class AccessibilityEventService extends AccessibilityService {

    private static final String TAG = "PixelPerfect.AccessibilityEventService";

    /** Type used to identify a Google corp account. */
    private static final String CORP_ACCOUNT_TYPE = "com.google";

    /**
     * The event processor. If the device doesn't have a Google corp account,
     * then this variable will be {@code null}, and no publishing in Clearcut
     * will occur.
     */
    private AccessibilityEventProcessor mProcessor;

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        // The publishing of accessibility events in Clearcut is only enabled
        // if the device has a Google corp account.
        if (getCorpAccountName() != null) {
            // No need to synchronize this as the #onCreate method will only be
            // called when the service is enabled in the settings, so it should
            // be safe from concurrency issues.
            mProcessor = new AccessibilityEventProcessor(new ExcludedPackages());
        }
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
        Log.v(TAG, "onAccessibilityEvent " + event);

        if (mProcessor != null) {
            mProcessor.process(event);
        }
    }

    /**
     * If a Google Corp account is linked to this device, returns the account
     * name. Otherwise, returns {@code null}.
     */
    private String getCorpAccountName() {
        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccounts();
        String accountName = null;
        for (Account account : accounts) {
          if (CORP_ACCOUNT_TYPE.equals(account.type)) {
            accountName = account.name;
          }
        }
        return accountName;
    }

}

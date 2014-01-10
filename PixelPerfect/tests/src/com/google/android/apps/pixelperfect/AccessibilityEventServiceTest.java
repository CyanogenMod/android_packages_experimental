package com.google.android.apps.pixelperfect;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Notification.Action;
import android.content.Intent;
import android.test.ServiceTestCase;

/**
 * Tests for {@link AccessibilityEventService}.
 */
public class AccessibilityEventServiceTest extends ServiceTestCase<AccessibilityEventService> {

    private static final String ACCOUNT_NAME = "John Doe";

    public AccessibilityEventServiceTest() {
        super(AccessibilityEventService.class);
    }

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      startService(new Intent(getContext(), AccessibilityEventService.class));
    }

    public void testPauseAndResume() {
        assertFalse(getService().getIsPaused());

        Intent intent = new Intent(getService(), AccessibilityEventService.class)
            .setAction(AccessibilityEventService.ACTION_PAUSE);
        startService(intent);
        assertTrue(getService().getIsPaused());

        intent = new Intent(getContext(), AccessibilityEventService.class);  // No action
        startService(intent);
        assertTrue(getService().getIsPaused());

        intent = new Intent(getContext(), AccessibilityEventService.class)
            .setAction(AccessibilityEventService.ACTION_RESUME);
        startService(intent);
        assertFalse(getService().getIsPaused());
    }

    @TargetApi(19)  // for Notification#actions
    public void testCreateNotification() {
        // This test is pretty limited because it's not possible to look at the Intent inside a
        // PendingIntent.
        Notification notification = getService().createNotification(false);
        Action[] actions = notification.actions;
        assertEquals(2, actions.length);
    }

    public void testGetCorpAccountNameImpl() {
        Account googleCorp = new Account(ACCOUNT_NAME, "com.google");
        Account gmail = new Account("Alexander Hamilton", "com.gmail");
        Account someOther = new Account("Bob Dylan", "com.mystuff.doesnt.exist");

        Account[] accountsNoCorp = { gmail, someOther };
        assertNull(getService().getCorpAccountNameImpl(accountsNoCorp));

        Account[] accountsHasCorp = { googleCorp, gmail, someOther };
        assertEquals(ACCOUNT_NAME, getService().getCorpAccountNameImpl(accountsHasCorp));
    }
}

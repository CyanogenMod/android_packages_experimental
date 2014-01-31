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

    private static final String ACCOUNT_NAME = "stlafon@google.com";

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
        Account gmail = new Account("foo@gmail.com", "com.gmail");

        Account[] accountsNoCorp = { gmail };
        assertNull(getService().getAccountNameImpl(accountsNoCorp));

        Account[] accountsHasCorp = { googleCorp, gmail };
        assertEquals(ACCOUNT_NAME, getService().getAccountNameImpl(accountsHasCorp));
    }
}

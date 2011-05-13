package com.android.testing.uiautomation;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class InteractionProvider {

    private static final String LOGTAG = "InteractionProvider";
    private IWindowManager mWm;
    private long mEventThrottle = 10;

    public InteractionProvider() {
        mWm = IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
        if (mWm == null) {
            throw new RuntimeException("Unable to connect to WindowManager, "
                    + "is the system running?");
        }
    }

    public boolean tap(int x, int y) {
        MotionEvent event = MotionEvent.obtain(
                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, x, y, 0);

        boolean ret = true;
        try {
            mWm.injectPointerEvent(event, false);
        } catch (RemoteException e) {
            Log.w(LOGTAG, "failed to inject DOWN event", e);
            ret = false;
        }
        doEventThrottle();
        event.setAction(MotionEvent.ACTION_UP);
        try {
            mWm.injectPointerEvent(event, false);
        } catch (RemoteException e) {
            Log.w(LOGTAG, "failed to inject UP event", e);
            ret = false;
        }
        return ret;
    }

    public boolean sendText(String text) {
        if (text == null) {
            return false;
        }
        boolean ret = true;
        KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

        KeyEvent[] events = keyCharacterMap.getEvents(text.toCharArray());

        if (events != null) {
            for (int i = 0; i < events.length; i++) {
                ret &= sendKey(events[i]);
                doEventThrottle();
            }
        }
        return ret;
    }

    public boolean sendKey(KeyEvent event) {
        boolean ret = true;
        try {
            mWm.injectKeyEvent(event, false);
        } catch (RemoteException e) {
            ret = false;
        }
        return ret;
    }

    public void setEventThrottle(long millis) {
        mEventThrottle = millis;
    }

    private void doEventThrottle() {
        try {
            Thread.sleep(mEventThrottle);
        } catch (InterruptedException e) {
        }
    }
}

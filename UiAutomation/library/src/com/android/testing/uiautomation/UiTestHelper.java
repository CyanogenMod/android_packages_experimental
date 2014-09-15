
package com.android.testing.uiautomation;

import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class UiTestHelper {

    private static final String LOGTAG = "UiTestHelper";

    private static final long DEFAULT_WAIT_TIMEOUT = 5000;

    private static final long POLL_INTERVAL = 250;

    private Context mContext;
    private AutomationProvider mProvider;
    private Map<String, Intent> mLauncherAppList;

    public UiTestHelper(Context context, AutomationProvider provider) {
        mContext = context;
        mProvider = provider;
        reloadLauncherAppList();
    }

    public boolean waitForWindow(String title) {
        return waitForWindow(title, DEFAULT_WAIT_TIMEOUT);
    }

    public boolean waitForWindow(String title, long timeout) {
        long startMills = SystemClock.uptimeMillis();
        boolean titleMatch = false;
        while (SystemClock.uptimeMillis() - startMills < timeout) {
            try {
                titleMatch = title.equals(mProvider.getCurrentActivityName());
            } catch (RemoteException e) {
                Log.e(LOGTAG, "failed to get current activity name", e);
                break;
            }
            if (titleMatch)
                break;
            try {
                Thread.sleep(POLL_INTERVAL);
            } catch (InterruptedException e) {
            }
        }
        return titleMatch;
    }

    public void reloadLauncherAppList() {
        mLauncherAppList = getLauncherAppList();
    }

    public boolean launchApplication(String appName) {
        Intent intent = mLauncherAppList.get(appName);
        if (intent == null)
            return false;
        mContext.startActivity(intent);
        return true;
    }

    private Map<String, Intent> getLauncherAppList() {
        final Intent queryIntent = new Intent();
        final Map<String, Intent> launchIntents = new TreeMap<String, Intent>();
        // get package manager and query pm for intents declared by apps as
        // launcher and main
        // basically those shown as icons in all apps screen
        IPackageManager mPm = IPackageManager.Stub
                .asInterface(ServiceManager.getService("package"));
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        queryIntent.setAction(Intent.ACTION_MAIN);
        final List<ResolveInfo> results;
        try {
            results = mPm.queryIntentActivities(queryIntent, null, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        for (ResolveInfo info : results) {
            Intent tmpIntent = new Intent(queryIntent);
            tmpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            tmpIntent.setClassName(info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);
            String appName = info.activityInfo.loadLabel(mContext.getPackageManager()).toString();
            launchIntents.put(appName, tmpIntent);
        }
        return launchIntents;
    }
}

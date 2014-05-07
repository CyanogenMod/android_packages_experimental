/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.example.notificationlistener;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Listener extends NotificationListenerService {
    private static final String TAG = "SampleListener";

    // Message tags
    private static final int MSG_NOTIFY = 1;
    private static final int MSG_CANCEL = 2;
    private static final int MSG_STARTUP = 3;
    private static final int MSG_ORDER = 4;
    private static final int MSG_DISMISS = 5;
    private static final int MSG_LAUNCH = 6;
    private static final int PAGE = 10;

    static final String ACTION_DISMISS = "com.android.example.notificationlistener.DISMISS";
    static final String ACTION_LAUNCH = "com.android.example.notificationlistener.LAUNCH";
    static final String ACTION_REFRESH = "com.android.example.notificationlistener.REFRESH";
    static final String EXTRA_KEY = "key";

    private static ArrayList<StatusBarNotification> sNotifications;

    public static List<StatusBarNotification> getNotifications() {
        return sNotifications;
    }

    private class Delta {
        final StatusBarNotification mSbn;
        final Ranking mRanking;

        public Delta(StatusBarNotification sbn, Ranking update) {
            mSbn = sbn;
            mRanking = update;
        }
    }

    private final Comparator<StatusBarNotification> mRankingComparator =
            new Comparator<StatusBarNotification>() {
                @Override
                public int compare(StatusBarNotification lhs, StatusBarNotification rhs) {
                    return Integer.compare(mRanking.getIndexOfKey(lhs.getKey()),
                            mRanking.getIndexOfKey(rhs.getKey()));
                }
            };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String key = intent.getStringExtra(EXTRA_KEY);
            int what = MSG_DISMISS;
            if (ACTION_LAUNCH.equals(intent.getAction())) {
                what = MSG_LAUNCH;
            }
            Log.d(TAG, "received an action broadcast " + intent.getAction());
            if (!TextUtils.isEmpty(key)) {
                Log.d(TAG, "  on " + key);
                Message.obtain(mHandler, what, key).sendToTarget();
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Delta delta = null;
            if (msg.obj instanceof Delta) {
                delta = (Delta) msg.obj;
            }
            switch (msg.what) {
                case MSG_NOTIFY:
                    Log.i(TAG, "notify: " + delta.mSbn.getKey());
                    synchronized (sNotifications) {
                        int position = mRanking.getIndexOfKey(delta.mSbn.getKey());
                        if (position == -1) {
                            sNotifications.add(delta.mSbn);
                        } else {
                            sNotifications.set(position, delta.mSbn);
                        }
                        mRanking = delta.mRanking;
                        Collections.sort(sNotifications, mRankingComparator);
                        Log.i(TAG, "finish with: " + sNotifications.size());
                    }
                    LocalBroadcastManager.getInstance(Listener.this)
                            .sendBroadcast(new Intent(ACTION_REFRESH)
                            .putExtra(EXTRA_KEY, delta.mSbn.getKey()));
                    break;

                case MSG_CANCEL:
                    Log.i(TAG, "remove: " + delta.mSbn.getKey());
                    synchronized (sNotifications) {
                        int position = mRanking.getIndexOfKey(delta.mSbn.getKey());
                        if (position != -1) {
                            sNotifications.remove(position);
                        }
                        mRanking = delta.mRanking;
                        Collections.sort(sNotifications, mRankingComparator);
                    }
                    LocalBroadcastManager.getInstance(Listener.this)
                            .sendBroadcast(new Intent(ACTION_REFRESH));
                    break;

                case MSG_ORDER:
                    Log.i(TAG, "reorder");
                    synchronized (sNotifications) {
                        mRanking = delta.mRanking;
                        Collections.sort(sNotifications, mRankingComparator);
                    }
                    LocalBroadcastManager.getInstance(Listener.this)
                            .sendBroadcast(new Intent(ACTION_REFRESH));
                    break;

                case MSG_STARTUP:
                    fetchActive();
                    Log.i(TAG, "start with: " + sNotifications.size() + " notifications.");
                    LocalBroadcastManager.getInstance(Listener.this)
                            .sendBroadcast(new Intent(ACTION_REFRESH));
                    break;

                case MSG_DISMISS:
                    if (msg.obj instanceof String) {
                        final String key = (String) msg.obj;
                        StatusBarNotification sbn = sNotifications.get(mRanking.getIndexOfKey(key));
                        if ((sbn.getNotification().flags & Notification.FLAG_AUTO_CANCEL) != 0 &&
                                sbn.getNotification().contentIntent != null) {
                            try {
                                sbn.getNotification().contentIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                                Log.d(TAG, "failed to send intent for " + sbn.getKey(), e);
                            }
                        }
                        cancelNotification(key);
                    }
                    break;

                case MSG_LAUNCH:
                    if (msg.obj instanceof String) {
                        final String key = (String) msg.obj;
                        StatusBarNotification sbn = sNotifications.get(mRanking.getIndexOfKey(key));
                        if (sbn.getNotification().contentIntent != null) {
                            try {
                                sbn.getNotification().contentIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                                Log.d(TAG, "failed to send intent for " + sbn.getKey(), e);
                            }
                        }
                        if ((sbn.getNotification().flags & Notification.FLAG_AUTO_CANCEL) != 0) {
                            cancelNotification(key);
                        }
                    }
                    break;
            }
        }
    };

    private Ranking mRanking;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "registering broadcast listener");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DISMISS);
        intentFilter.addAction(ACTION_LAUNCH);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onListenerConnected(String[] notificationKeys) {
        Message.obtain(mHandler, MSG_STARTUP).sendToTarget();
    }

    @Override
    public void onNotificationRankingUpdate() {
        Message.obtain(mHandler, MSG_ORDER,
                new Delta(null, getCurrentRanking())).sendToTarget();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Message.obtain(mHandler, MSG_NOTIFY,
                new Delta(sbn, getCurrentRanking())).sendToTarget();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Message.obtain(mHandler, MSG_CANCEL,
                new Delta(sbn, getCurrentRanking())).sendToTarget();
    }

    private void fetchActive() {
        mRanking = getCurrentRanking();
        String[] keys = mRanking.getOrderedKeys();
        sNotifications = new ArrayList<StatusBarNotification>();
        sNotifications.clear();
        for (int i = 0; i < keys.length; i += PAGE) {
            final int j = (i + PAGE < keys.length ? i + PAGE : keys.length);
            String[] fetchKeys = Arrays.copyOfRange(keys, i, j);
            StatusBarNotification[] sbns = getActiveNotifications(fetchKeys);
            for(int s = 0; s < sbns.length; s++) {
                // unfortunately cloneLight() is hidden
                sNotifications.add(sbns[s]);
            }
        }
        Collections.sort(sNotifications, mRankingComparator);
    }
}

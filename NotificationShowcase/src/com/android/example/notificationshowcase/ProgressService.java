/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.example.notificationshowcase;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class ProgressService extends IntentService {

    private static final String TAG = "ProgressService";

    private static final String ACTION_PROGRESS = "progress";
    private static final String ACTION_SILENT = "silent";

    private static ProgressService.UpdateRunnable mUpdateRunnable;

    private Handler mHandler;

    public ProgressService() {
        super(TAG);
    }
    public ProgressService(String name) {
        super(name);
    }

    class UpdateRunnable implements Runnable {

        private final int mId;
        private final long mWhen;
        private int mProgress;

        UpdateRunnable(int id, long when, int progress) {
            mId = NotificationService.NOTIFICATION_ID + id;
            mWhen = when;
            mProgress = progress;
        }

        @Override
        public void run() {
            NotificationManager noMa = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            if (mUpdateRunnable != null) {
                Log.v(TAG, "id: " + mId + " when: " + mWhen + " progress: " + mProgress);
                noMa.notify(mId, NotificationService.makeUploadNotification(
                        ProgressService.this, mProgress, mWhen));
                mProgress += 2;
                if (mProgress <= 100) {
                    mHandler.postDelayed(mUpdateRunnable, 100);
                }
            } else {
                noMa.cancel(mId);
                Log.d(TAG, "mUpdateRunnable is null ");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler = new Handler();
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent " + intent.getAction());
        if (ACTION_PROGRESS.equals(intent.getAction())) {
            final int id = intent.getIntExtra("id", 0);
            final long when = intent.getLongExtra("when", 0L);
            int progress = intent.getIntExtra("progress", 0);
            mUpdateRunnable = new UpdateRunnable(id, when, progress);
            mHandler.postDelayed(mUpdateRunnable, 1000);
        } else if (ACTION_SILENT.equals(intent.getAction())) {
            Log.d(TAG, "cancelling ");
            if (mUpdateRunnable != null) {
                mUpdateRunnable = null;
            }
        }
    }

    public static void startProgressUpdater(Context context, int id, long when, int progress) {
        Intent progressIntent = new Intent(context, ProgressService.class);
        progressIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        progressIntent.setAction(ACTION_PROGRESS);
        progressIntent.putExtra("id", id);
        progressIntent.putExtra("when", when);
        progressIntent.putExtra("progress", progress);
        context.startService(progressIntent);
    }

    public static PendingIntent getSilencePendingIntent(Context context) {
        Log.d(TAG, "getSilencePendingIntent ");
        Intent silenceIntent = new Intent(context, ProgressService.class);
        silenceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        silenceIntent.setAction(ACTION_SILENT);
        PendingIntent pi = PendingIntent.getService(
                context, 0, silenceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }
}

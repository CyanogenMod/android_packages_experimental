/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.strictmodetest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AndroidException;
import android.util.Config;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class StrictModeActivity extends Activity {

    private static final String TAG = "StrictModeActivity";
    private static final Uri SYSTEM_SETTINGS_URI = Uri.parse("content://settings/system");

    private ContentResolver cr;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cr = getContentResolver();
        final SQLiteDatabase db = openOrCreateDatabase("foo.db", MODE_PRIVATE, null);

        final Button readButton = (Button) findViewById(R.id.read_button);
        readButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Cursor c = null;
                    try {
                        c = db.rawQuery("SELECT * FROM foo", null);
                    } finally {
                        if (c != null) c.close();
                    }
                }
            });

        final Button writeButton = (Button) findViewById(R.id.write_button);
        writeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS FOO (a INT)");
                }
            });

        final CheckBox checkNoWrite = (CheckBox) findViewById(R.id.policy_no_write);
        final CheckBox checkNoRead = (CheckBox) findViewById(R.id.policy_no_reads);
        final CheckBox checkNoNetwork = (CheckBox) findViewById(R.id.policy_no_network);
        final CheckBox checkPenaltyLog = (CheckBox) findViewById(R.id.policy_penalty_log);
        final CheckBox checkPenaltyDialog = (CheckBox) findViewById(R.id.policy_penalty_dialog);
        final CheckBox checkPenaltyDeath = (CheckBox) findViewById(R.id.policy_penalty_death);
        final CheckBox checkPenaltyDropBox = (CheckBox) findViewById(R.id.policy_penalty_dropbox);

        View.OnClickListener changePolicy = new View.OnClickListener() {
                public void onClick(View v) {
                    int newPolicy = 0;
                    if (checkNoWrite.isChecked()) newPolicy |= StrictMode.DISALLOW_DISK_WRITE;
                    if (checkNoRead.isChecked()) newPolicy |= StrictMode.DISALLOW_DISK_READ;
                    if (checkNoNetwork.isChecked()) newPolicy |= StrictMode.DISALLOW_NETWORK;
                    if (checkPenaltyLog.isChecked()) newPolicy |= StrictMode.PENALTY_LOG;
                    if (checkPenaltyDialog.isChecked()) newPolicy |= StrictMode.PENALTY_DIALOG;
                    if (checkPenaltyDeath.isChecked()) newPolicy |= StrictMode.PENALTY_DEATH;
                    if (checkPenaltyDropBox.isChecked()) newPolicy |= StrictMode.PENALTY_DROPBOX;
                    Log.v(TAG, "Changing policy to: " + newPolicy);
                    StrictMode.setThreadBlockingPolicy(newPolicy);
                }
            };
        checkNoWrite.setOnClickListener(changePolicy);
        checkNoRead.setOnClickListener(changePolicy);
        checkNoNetwork.setOnClickListener(changePolicy);
        checkPenaltyLog.setOnClickListener(changePolicy);
        checkPenaltyDialog.setOnClickListener(changePolicy);
        checkPenaltyDeath.setOnClickListener(changePolicy);
        checkPenaltyDropBox.setOnClickListener(changePolicy);
    }

    private void fileReadLoop() {
        RandomAccessFile raf = null;
        File filename = getFileStreamPath("test.dat");
        try {
            long sumNanos = 0;
            byte[] buf = new byte[512];

            //raf = new RandomAccessFile(filename, "rw");
            //raf.write(buf);
            //raf.close();
            //raf = null;

            // The data's almost certainly cached -- it's not clear what we're testing here
            raf = new RandomAccessFile(filename, "r");
            raf.seek(0);
            raf.read(buf);
        } catch (IOException e) {
            Log.e(TAG, "File read failed", e);
        } finally {
            try { if (raf != null) raf.close(); } catch (IOException e) {}
        }
    }

    // Returns milliseconds taken, or -1 on failure.
    private long settingsWrite(int mode) {
        Cursor c = null;
        long startTime = SystemClock.uptimeMillis();
        // The database will take care of replacing duplicates.
        try {
            ContentValues values = new ContentValues();
            values.put("name", "dummy_for_testing");
            values.put("value", "" + startTime);
            Uri uri = cr.insert(SYSTEM_SETTINGS_URI, values);
            Log.v(TAG, "inserted uri: " + uri);
        } catch (SQLException e) {
            Log.w(TAG, "sqliteexception during write: " + e);
            return -1;
        }
        long duration = SystemClock.uptimeMillis() - startTime;
        return duration;
    }
}

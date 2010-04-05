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


// TODO: find AIDL for ContentResolver.  add queryExtensible() returning struct{Cursor?,String? singleRow}
//    -- would have to lazily do real requery(), registerContentObserver(), etc

package com.android.rpc_performance;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AndroidException;
import android.util.Config;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProviderPerfActivity extends Activity {

    private static final String TAG = "ProviderPerfActivity";
    private static final Uri SYSTEM_SETTINGS_URI = Uri.parse("content://settings/system");

    // No-op provider URLs:
    private static final Uri CROSS_PROC_PROVIDER_URI = Uri.parse("content://com.android.rpc_performance/");
    private static final Uri IN_PROC_PROVIDER_URI = Uri.parse("content://com.android.rpc_performance.local/");

    private final Handler mHandler = new Handler();
    private final static int LOOP_TIME_MILLIS = 2000;
    private final static long LOOP_TIME_NANOS = (long) LOOP_TIME_MILLIS * 1000000L;

    private IService mServiceStub = null;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceStub = IService.Stub.asInterface(service);
            Log.v(TAG, "Service bound");
        }

        public void onServiceDisconnected(ComponentName name) {
            mServiceStub = null;
            Log.v(TAG, "Service unbound");
        };
    };


    ContentResolver cr;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        cr = getContentResolver();

        setButtonAction(R.id.lookup_button, new Runnable() {
                public void run() {
                    final float avgTime = settingsProviderLoop(100, MODE_READ, 0);
                    endAsyncOp(R.id.lookup_button, R.id.lookup_text, avgTime);
                }});

        setButtonAction(R.id.bg_read_button, new Runnable() {
                public void run() {
                    final float avgTime =
                        settingsProviderLoop(50, MODE_READ, 100);
                    endAsyncOp(R.id.bg_read_button, R.id.bg_read_text, avgTime);
                }});

        setButtonAction(R.id.bg_write_button, new Runnable() {
                public void run() {
                    final float avgTime =
                        settingsProviderLoop(20, MODE_WRITE, 0);
                    endAsyncOp(R.id.bg_write_button, R.id.bg_write_text, avgTime);
                }});

        setButtonAction(R.id.bg_writedup_button, new Runnable() {
                public void run() {
                    final float avgTime =
                        settingsProviderLoop(20, MODE_WRITE_DUP, 0);
                    endAsyncOp(R.id.bg_writedup_button, R.id.bg_writedup_text, avgTime);
                }});

        setButtonAction(R.id.dummy_lookup_button, new Runnable() {
                public void run() {
                    final float avgTime = noOpProviderLoop(CROSS_PROC_PROVIDER_URI);
                    endAsyncOp(R.id.dummy_lookup_button, R.id.dummy_lookup_text, avgTime);
                }});

        setButtonAction(R.id.dummy_local_lookup_button, new Runnable() {
                public void run() {
                    final float avgTime = noOpProviderLoop(IN_PROC_PROVIDER_URI);
                    endAsyncOp(R.id.dummy_local_lookup_button,
                               R.id.dummy_local_lookup_text, avgTime);
                }});

        setButtonAction(R.id.localsocket_button, new Runnable() {
                public void run() {
                    final float avgTime = localSocketLoop();
                    endAsyncOp(R.id.localsocket_button, R.id.localsocket_text, avgTime);
                }});

        setButtonAction(R.id.service_button, new Runnable() {
                public void run() {
                    final float avgTime = serviceLoop(0);
                    endAsyncOp(R.id.service_button, R.id.service_text, avgTime);
                }});

        setButtonAction(R.id.service2_button, new Runnable() {
                public void run() {
                    final float avgTime = serviceLoop(1);
                    endAsyncOp(R.id.service2_button, R.id.service2_text, avgTime);
                }});

        setButtonAction(R.id.proc_button, new Runnable() {
                public void run() {
                    final float avgTime = procLoop();
                    endAsyncOp(R.id.proc_button, R.id.proc_text, avgTime);
                }});

        setButtonAction(R.id.call_button, new Runnable() {
                public void run() {
                    final float avgTime = callLoop("ringtone");
                    endAsyncOp(R.id.call_button, R.id.call_text, avgTime);
                }});

        setButtonAction(R.id.call2_button, new Runnable() {
                public void run() {
                    final float avgTime = callLoop("XXXXXXXX");  // non-existent
                    endAsyncOp(R.id.call2_button, R.id.call2_text, avgTime);
                }});
    }

    @Override public void onResume() {
        super.onResume();

        bindService(new Intent(this, MiscService.class),
                    serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override public void onPause() {
        super.onPause();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }

    private void setButtonAction(int button_id, final Runnable r) {
        final Button button = (Button) findViewById(button_id);
        if (button == null) {
            Log.w(TAG, "Bogus button ID: " + button_id);
            return;
        }
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    button.setEnabled(false);
                    new Thread(r).start();
                }
            });
    }

    private void endAsyncOp(final int button_id, final int text_id, final float avgTime) {
        mHandler.post(new Runnable() {
                public void run() {
                    Button button = (Button) findViewById(button_id);
                    button.setEnabled(true);
                    setTextTime(text_id, avgTime);
                }});
    }

    private void setTextTime(int id, float avgTime) {
        TextView tv = (TextView) findViewById(id);
        if (tv == null) return;
        String text = tv.getText().toString();
        text = text.substring(0, text.indexOf(':') + 1) + "\n" + avgTime + " ms avg";
        tv.setText(text);
    }

    // Returns average cross-process dummy query time in milliseconds.
    private float noOpProviderLoop(Uri uri) {
        long sumNanos = 0;
        int failures = 0;
        int total = 0;
        long startTime = System.nanoTime();
        long endTime = startTime + LOOP_TIME_NANOS;
        while (System.nanoTime() < endTime) {
            long duration = doNoOpLookup(uri);
            if (duration < 0) {
                failures++;
            } else {
                total++;
                sumNanos += duration;
            }
        }
        float averageMillis = (float) sumNanos /
            (float) (total != 0 ? total : 1) /
            1000000.0f;
        Log.v(TAG, "dummy loop: fails=" + failures + "; total=" + total + "; goodavg ms=" + averageMillis);
        return averageMillis;
    }

    // Returns average cross-process dummy query time in milliseconds.
    private float callLoop(String key) {
        IContentProvider cp = cr.acquireProvider(SYSTEM_SETTINGS_URI.getAuthority());

        long sumNanos = 0;
        int total = 0;

        long lastTime = System.nanoTime();
        long endTime = lastTime + LOOP_TIME_NANOS;
        try {
            while (lastTime < endTime) {
                Bundle b = cp.call("GET_system", key, null);
                long nowTime = System.nanoTime();
                total++;
                sumNanos += (nowTime - lastTime);
                lastTime = nowTime;
            }
        } catch (RemoteException e) {
            return -999.0f;
        }
        float averageMillis = (float) sumNanos /
            (float) (total != 0 ? total : 1) /
            1000000.0f;
        Log.v(TAG, "call loop: avg_ms=" + averageMillis + "; calls=" + total);
        return averageMillis;
    }

    // Returns average cross-process dummy query time in milliseconds.
    private float procLoop() {
        long sumNanos = 0;
        int total = 0;
        long lastTime = System.nanoTime();
        long endTime = lastTime + LOOP_TIME_NANOS;
        File f = new File("/proc/self/cmdline");
        byte[] buf = new byte[100];
        String value = null;
        try {
            while (lastTime < endTime) {
                FileInputStream is = new FileInputStream(f);
                int readBytes = is.read(buf, 0, 100);
                is.close();
                //value = new String(buf, 0, readBytes);
                long nowTime = System.nanoTime();
                total++;
                sumNanos += (nowTime - lastTime);
                lastTime = nowTime;
            }
        } catch (IOException e) {
            return -999.0f;
        }
        float averageMillis = (float) sumNanos /
            (float) (total != 0 ? total : 1) /
            1000000.0f;
        Log.v(TAG, "proc loop: total: " + total + "; avg_ms=" + averageMillis + "; value=" + value);
        return averageMillis;
    }

    private static final String[] IGNORED_COLUMN = {"ignored"};

    // Returns nanoseconds.
    private long doNoOpLookup(Uri uri) {
        Cursor c = null;
        try {
            long startTime = System.nanoTime();
            c = cr.query(uri,
                         IGNORED_COLUMN,  //new String[]{"ignored"},  // but allocate it for apples-to-apples
                         "name=?",
                         IGNORED_COLUMN,  // new String[]{"also_ignored"},  // also for equality in benchmarking
                         null /* sort order */);
            if (c == null) {
                Log.w(TAG, "cursor null");
                return -1;
            }
            String value = c.moveToNext() ? c.getString(0) : null;
            long duration = System.nanoTime() - startTime;
            //Log.v(TAG, "got value: " + value + " in " + duration);
            return duration;
        } catch (SQLException e) {
            Log.w(TAG, "sqlite exception: " + e);
            return -1;
        } finally {
            if (c != null) c.close();
        }
    }

    // Returns average cross-process dummy query time in milliseconds.
    private float serviceLoop(int amtEncoding) {
        if (mServiceStub == null) {
            Log.v(TAG, "No service stub.");
            return -999;
        }
        String dummy = null;
        try {
            long sumNanos = 0;
            int count = 0;
            long lastTime = System.nanoTime();
            long endTime = lastTime + LOOP_TIME_NANOS;
            while (lastTime < endTime) {
                if (amtEncoding == 0) {
                    mServiceStub.pingVoid();
                } else {
                    dummy = mServiceStub.pingString(dummy);
                }
                long curTime = System.nanoTime();
                long duration = curTime - lastTime;
                lastTime = curTime;
                count++;
                sumNanos += duration;
            }
            float averageMillis = (float) sumNanos / (float) (count != 0 ? count : 1) / 1000000.0f;
            Log.v(TAG, "service loop: total: " + count + "; avg_ms=" + averageMillis);
            return averageMillis;
        } catch (RemoteException e) {
            Log.e(TAG, "error in service loop: " + e);
            return -999.0f;
        }
    }

    // Returns average milliseconds.
    private float localSocketLoop() {
        LocalSocket socket = null;
        try {
            socket = new LocalSocket();
            Log.v(TAG, "Connecting to socket...");
            socket.connect(new LocalSocketAddress(MiscService.SOCKET_NAME));
            Log.v(TAG, "Connected to socket.");
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            long nowTime = System.nanoTime();
            long endTime = nowTime + LOOP_TIME_NANOS;
            int count = 0;
            long sumNanos = 0;
            while (nowTime < endTime) {
                int expectByte = count & 0xff;
                os.write(expectByte);
                int gotBackByte = is.read();

                long afterTime = System.nanoTime();
                sumNanos += (afterTime - nowTime);
                nowTime = afterTime;

                if (gotBackByte != expectByte) {
                    Log.w(TAG, "Got wrong byte back.  Got: " + gotBackByte
                          + "; wanted=" + expectByte);
                    return -999.00f;
                }
                count++;
            }
            return count == 0 ? 0.0f : ((float) sumNanos / (float) count / 1000000.0f);
        } catch (IOException e) {
            Log.v(TAG, "error in localSocketLoop: " + e);
            return -1.0f;
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }

    // Returns average milliseconds.
    private static final int MODE_READ = 0;
    private static final int MODE_WRITE = 1;
    private static final int MODE_WRITE_DUP = 2;

    private float settingsProviderLoop(int iters, int mode, long innerSleep) {
        long sumMillis = 0;
        int total = 0;
        long startTime = System.nanoTime();
        long endTime = startTime + LOOP_TIME_NANOS;
        while (System.nanoTime() < endTime) {
            long duration = mode == MODE_READ ? doRead(innerSleep) : doWrite(mode);
            if (duration < 0) {
                return -999.0f;
            }
            total++;
            sumMillis += duration;
        }
        float averageMillis = ((float) sumMillis / (float) (total != 0 ? total : 1));
        Log.v(TAG, "settings provider; mode=" + mode + "; total=" + total +
              "; goodavg_ms=" + averageMillis);
        return averageMillis;
    }

    // Returns milliseconds taken, or -1 on failure.
    private long doRead(long innerSleep) {
        Cursor c = null;
        try {
            long startTime = SystemClock.uptimeMillis();
            c = cr.query(SYSTEM_SETTINGS_URI,
                         new String[]{"value"},
                         "name=?",
                         new String[]{"airplane_mode_on"},
                         null /* sort order */);
            if (c == null) {
                Log.w(TAG, "cursor null");
                return -1;
            }
            String value = c.moveToNext() ? c.getString(0) : null;
            long duration = SystemClock.uptimeMillis() - startTime;
            if (innerSleep > 0) {
                try {
                    Thread.sleep(innerSleep);
                } catch (InterruptedException e) {}
            }
            return duration;
        } catch (SQLException e) {
            Log.w(TAG, "sqlite exception: " + e);
            return -1;
        } finally {
            if (c != null) c.close();
        }
    }

    // Returns milliseconds taken, or -1 on failure.
    private long doWrite(int mode) {
        Cursor c = null;
        long startTime = SystemClock.uptimeMillis();
        // The database will take care of replacing duplicates.
        try {
            ContentValues values = new ContentValues();
            values.put("name", "dummy_for_testing");
            values.put("value", (mode == MODE_WRITE ? (""+startTime) : "foo"));
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

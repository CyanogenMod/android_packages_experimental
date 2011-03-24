// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.vending.sectool.v1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GoogleSecurityToolReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (GoogleSecurityToolActivity.DEBUG)
            Log.d(GoogleSecurityToolActivity.TAG, 
                    "Got an intent, starting service" + intent.getAction());
        intent = new Intent(intent);
        intent.setClass(context, GoogleSecurityToolActivity.class);
        if (context.startService(intent) == null) {
            if (GoogleSecurityToolActivity.DEBUG)
                Log.e(GoogleSecurityToolActivity.TAG, "Can't start service");
        }
    }
}

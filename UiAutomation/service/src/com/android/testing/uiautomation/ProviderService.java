package com.android.testing.uiautomation;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ProviderService extends Service {

    private static final String LOGTAG = "ProviderService";
    private ProviderImpl mProviderImpl;

    @Override
    public IBinder onBind(Intent intent) {
        if (mProviderImpl == null) {
            try {
                mProviderImpl = new ProviderImpl(this);
            } catch (RemoteException e) {
                Log.e(LOGTAG, "Failed to initialize implementation.");
                return null;
            }
        }
        return mProviderImpl;
    }
}

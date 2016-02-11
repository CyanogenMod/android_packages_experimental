/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.printservicestubs.stubs.mdnsFilter;

import android.annotation.NonNull;
import android.annotation.StringRes;
import android.content.Context;
import android.net.Uri;
import com.android.internal.util.Preconditions;
import com.android.printservicestubs.PrintServiceStub;
import com.android.printservicestubs.R;
import com.android.printservicestubs.servicediscovery.DiscoveryListener;
import com.android.printservicestubs.servicediscovery.NetworkDevice;
import com.android.printservicestubs.servicediscovery.NetworkDiscovery;
import com.android.printservicestubs.servicediscovery.mdns.MDNSUtils;

import java.util.HashSet;
import java.util.List;

/**
 * A stub listening for mDNS results and only adding the ones that {@link MDNSUtils#isVendorPrinter
 * match} configured list
 */
public class MDNSFilterStub implements PrintServiceStub, DiscoveryListener {
    /**
     * Name of the print service this stub is for
     */
    private final @StringRes int mName;

    /**
     * Uri to install the print service this stub is for
     */
    private final @NonNull Uri mInstallPackage;

    /**
     * mDNS names handled by the print service this stub is for
     */
    private final @NonNull HashSet<String> mMDNSNames;

    /**
     * Printer identifiers of the printers found.
     */
    private final @NonNull HashSet<String> printers;

    /**
     * Context of the user of this stub
     */
    private final @NonNull Context mContext;

    /**
     * Call backs to report the number of printers found.
     */
    private PrinterDiscoveryCallback mCallback;

    /**
     * Create new stub that assumes that a print service can be used to print on all printers
     * matching some mDNS names.
     *
     * @param context     The context the plugin runs in
     * @param name        The user friendly name of the print service
     * @param packageName The package name of the print service
     * @param mDNSNames   The mDNS names of the printer.
     */
    public MDNSFilterStub(@NonNull Context context, @NonNull String name,
            @NonNull String packageName, @NonNull List<String> mDNSNames) {
        mContext = Preconditions.checkNotNull(context, "context");
        mName = mContext.getResources().getIdentifier(Preconditions.checkNotNull(name, "name"),
                null, mContext.getPackageName());
        mInstallPackage = Uri.parse(context.getString(R.string.uri_package_details,
                Preconditions.checkNotNull(packageName, "packageName")));
        mMDNSNames = new HashSet<>(Preconditions
                .checkCollectionNotEmpty(Preconditions.checkCollectionElementsNotNull(mDNSNames,
                        "mDNSNames"), "mDNSNames"));

        printers = new HashSet<>();
    }

    @Override
    public @NonNull Uri getInstallUri() {
        return mInstallPackage;
    }

    @Override
    public void start(@NonNull PrinterDiscoveryCallback callback)
            throws Exception {
        mCallback = callback;
        NetworkDiscovery.onListenerAdded(this, mContext);
    }

    @Override
    public @StringRes int getName() {
        return mName;
    }

    @Override
    public void stop() throws Exception {
        NetworkDiscovery.removeDiscoveryListener(this);
    }

    @Override
    public void onDeviceRemoved(@NonNull NetworkDevice networkDevice) {
        if (MDNSUtils.isVendorPrinter(networkDevice, mMDNSNames)) {
            synchronized (printers) {
                boolean removed = printers.remove(networkDevice.getDeviceIdentifier());

                if (removed) {
                    mCallback.onChanged(printers.size());
                }
            }
        }
    }

    @Override
    public void onDeviceFound(@NonNull NetworkDevice networkDevice) {
        if (MDNSUtils.isVendorPrinter(networkDevice, mMDNSNames)) {
            synchronized (printers) {
                boolean added = printers.add(networkDevice.getDeviceIdentifier());

                if (added) {
                    mCallback.onChanged(printers.size());
                }
            }
        }
    }
}

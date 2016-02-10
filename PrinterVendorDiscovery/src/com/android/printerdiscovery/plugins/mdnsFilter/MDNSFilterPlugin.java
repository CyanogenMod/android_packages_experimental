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

package com.android.printerdiscovery.plugins.mdnsFilter;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.android.internal.util.Preconditions;
import com.android.printerdiscovery.PrinterDiscoveryPlugin;
import com.android.printerdiscovery.R;
import com.android.printerdiscovery.servicediscovery.DiscoveryListener;
import com.android.printerdiscovery.servicediscovery.NetworkDevice;
import com.android.printerdiscovery.servicediscovery.NetworkDiscovery;
import com.android.printerdiscovery.servicediscovery.mdns.MDNSUtils;

import java.util.HashSet;
import java.util.List;

/**
 * A plugin listening for mDNS results and only adding the ones that match configured list
 */
public class MDNSFilterPlugin implements PrinterDiscoveryPlugin, DiscoveryListener {
    private final @NonNull String mName;
    private final @NonNull Intent mInstallPackage;
    private final @NonNull HashSet<String> mMDNSNames;
    private final @NonNull HashSet<String> printers;
    private final @NonNull Context mContext;
    private PrinterDiscoveryCallback mCallback;

    /**
     * Create new plugin that assumes that a print service can be used to print on all printers
     * matching some mDNS names.
     *
     * @param context     The context the plugin runs in
     * @param name        The user friendly name of the print service
     * @param packageName The package name of the print service
     * @param mDNSNames   The mDNS names of the printer.
     */
    public MDNSFilterPlugin(@NonNull Context context, @NonNull String name,
            @NonNull String packageName, @NonNull List<String> mDNSNames) {
        mContext = Preconditions.checkNotNull(context, "context");
        mName = Preconditions.checkNotNull(name, "name");
        mInstallPackage = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                context.getString(R.string.uri_package_details,
                        Preconditions.checkNotNull(packageName, "packageName"))));
        mMDNSNames = new HashSet<>(Preconditions
                .checkCollectionNotEmpty(Preconditions.checkCollectionElementsNotNull(mDNSNames,
                        "mDNSNames"), "mDNSNames"));

        printers = new HashSet<>();
    }

    @Override
    public @NonNull Intent getAction() {
        return mInstallPackage;
    }

    @Override
    public void start(@NonNull PrinterDiscoveryCallback callback)
            throws Exception {
        mCallback = callback;
        NetworkDiscovery.onListenerAdded(this, mContext);
    }

    @Override
    public @NonNull String getName() {
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

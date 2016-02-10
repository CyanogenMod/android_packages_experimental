/*
 * Copyright (C) 2016 Mopria Alliance, Inc.
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

package com.android.printerdiscovery.plugins.gcp;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import com.android.printerdiscovery.IPrinterDiscoveryPlugin;
import com.android.printerdiscovery.R;
import com.android.printerdiscovery.VendorConfig;
import com.android.printerdiscovery.servicediscovery.IDiscoveryListener;
import com.android.printerdiscovery.servicediscovery.NetworkDevice;
import com.android.printerdiscovery.servicediscovery.NetworkDiscovery;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * A plugin listening for mDNS results and only adding the ones that are Google Cloud Print
 * printers
 */
public class GCPPlugin implements IPrinterDiscoveryPlugin, IDiscoveryListener {
    private static final String PRIVET_TYPE_PRITNER = "printer";

    private final @NonNull String mName;
    private final @NonNull Intent mInstallPackage;
    private final @NonNull HashSet<String> printers;
    private final @NonNull Context mContext;
    private PrinterDiscoveryCallback mCallback;

    /**
     * Create new plugin that finds all Google Cloud Print printers.
     *
     * @param context The context the plugin runs in
     *
     * @throws IOException            If the configuration file cannot be read
     * @throws XmlPullParserException If the configuration file is corrupt
     */
    public GCPPlugin(@NonNull Context context) throws IOException, XmlPullParserException {
        mContext = Preconditions.checkNotNull(context, "context");

        mName = context.getString(R.string.plugin_vendor_gcp);
        VendorConfig config = VendorConfig.getConfig(context, mName);
        mInstallPackage = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                context.getString(R.string.uri_package_details, config.getPackageName())));

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

    /**
     * Get all service names for the device
     *
     * @param networkDevice The device
     *
     * @return The list of service names
     */
    private ArrayList<String> getServicesNames(NetworkDevice networkDevice) {
        ArrayList<String> mDnsServices = new ArrayList<>();

        ArrayList<NetworkDevice> instances = networkDevice.getAllDiscoveryInstances();
        final int numInstances = instances.size();
        for (int i = 0; i < numInstances; i++) {
            mDnsServices.add(instances.get(i).getBonjourService());
        }

        return mDnsServices;
    }

    /**
     * Check if a network device is a GCP printer
     *
     * @param networkDevice The device that might be a GCP printer
     *
     * @return true iff the device is a GCP printer
     */
    private boolean isGCPPrinter(@NonNull NetworkDevice networkDevice) {
        final String privetService = mContext.getString(R.string.mdns_service_privet);

        if (!getServicesNames(networkDevice).contains(privetService)) {
            return false;
        }

        String type = networkDevice.getTxtAttributes(privetService).getString("type");

        return !TextUtils.isEmpty(type) && (type.contains(PRIVET_TYPE_PRITNER));
    }

    @Override
    public void onDeviceRemoved(@NonNull NetworkDevice networkDevice) {
        if (isGCPPrinter(networkDevice)) {
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
        if (isGCPPrinter(networkDevice)) {
            synchronized (printers) {
                boolean added = printers.add(networkDevice.getDeviceIdentifier());

                if (added) {
                    mCallback.onChanged(printers.size());
                }
            }
        }
    }
}

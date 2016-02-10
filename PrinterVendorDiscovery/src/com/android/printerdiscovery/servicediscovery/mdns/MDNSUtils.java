/*
 * (c) Copyright 2016 Mopria Alliance, Inc.
 * (c) Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.printerdiscovery.servicediscovery.mdns;

import android.annotation.NonNull;
import android.os.Bundle;

import com.android.internal.util.Preconditions;
import com.android.printerdiscovery.servicediscovery.NetworkDevice;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

/**
 * Utils for dealing with mDNS attributes
 */
public class MDNSUtils {
    public static final String ATTRIBUTE__TY = "ty";
    public static final String ATTRIBUTE__PRODUCT = "product";
    public static final String ATTRIBUTE__USB_MFG = "usb_MFG";
    public static final String ATTRIBUTE__MFG = "mfg";

    /**
     * Check if the device has any of a set of vendor names.
     *
     * @param networkDevice The device
     * @param vendorNames   The vendors
     *
     * @return true iff the has any of the set of vendor names
     */
    public static boolean isVendorPrinter(@NonNull NetworkDevice networkDevice,
            @NonNull Set<String> vendorNames) {
        networkDevice = Preconditions.checkNotNull(networkDevice);
        vendorNames = Preconditions.checkCollectionElementsNotNull(vendorNames, "vendorNames");

        ArrayList<NetworkDevice> instances = networkDevice.getAllDiscoveryInstances();

        final int numInstances = instances.size();
        for (int i = 0; i < numInstances; i++) {
            Bundle attributes = instances.get(i).getTxtAttributes();

            String product = attributes.getString(ATTRIBUTE__PRODUCT);
            String ty = attributes.getString(ATTRIBUTE__TY);
            String usbMfg = attributes.getString(ATTRIBUTE__USB_MFG);
            String mfg = attributes.getString(ATTRIBUTE__MFG);

            if (containsVendor(product, vendorNames) || containsVendor(ty, vendorNames) ||
                    containsVendor(usbMfg, vendorNames) || containsVendor(mfg, vendorNames)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the attribute matches any of the vendor names, ignoreing capitalization.
     *
     * @param attr        The attribute
     * @param vendorNames The vendor names
     *
     * @return true iff the attribute matches any of the vendor names
     */
    private static boolean containsVendor(String attr, @NonNull Set<String> vendorNames) {
        if (attr == null) {
            return false;
        }

        for (String name : vendorNames) {
            if (containsString(attr, name) ||
                    containsString(attr.toLowerCase(Locale.US), name.toLowerCase(Locale.US)) ||
                    containsString(attr.toUpperCase(Locale.US), name.toUpperCase(Locale.US)))
                return true;
        }
        return false;
    }

    /**
     * Check if a string in another string
     *
     * @param container The string that contains the string
     * @param contained The string that is contained
     *
     * @return true if the string is contained in the other.
     */
    private static boolean containsString(@NonNull String container, @NonNull String contained) {
        return container.equalsIgnoreCase(contained) || container.contains(contained + " ");
    }
}

/*
 * (c) Copyright 2016 Mopria Alliance, Inc.
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

import android.os.Bundle;
import android.text.TextUtils;

import com.android.printerdiscovery.servicediscovery.NetworkDevice;

import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class MDnsUtils {
    public static final String ATTRIBUTE__TY = "ty";
    public static final String ATTRIBUTE__PRODUCT = "product";
    public static final String ATTRIBUTE__USB_MFG = "usb_MFG";
    public static final String ATTRIBUTE__MFG = "mfg";
    public static final String ATTRIBUTE__PDL = "pdl";
    public static final String ATTRIBUTE__NOTE = "note";
    public static final String ATTRIBUTE__PRIORITY = "priority";
    public static final String ATTRIBUTE__RESOURCE_PATH = "rp";

    public static boolean isVendorPrinter(NetworkDevice networkDevice, String[] vendorValues) {
        boolean isVendorPrinter = false;

        List<NetworkDevice> allInstances = networkDevice.getAllDiscoveryInstances();

        for (NetworkDevice instance : allInstances) {
            Bundle attributes = instance.getTxtAttributes();
            String product = attributes.getString(ATTRIBUTE__PRODUCT);
            String ty = attributes.getString(ATTRIBUTE__TY);
            String usbMfg = attributes.getString(ATTRIBUTE__USB_MFG);
            String mfg = attributes.getString(ATTRIBUTE__MFG);
            isVendorPrinter = containsVendor(product, vendorValues) || containsVendor(ty, vendorValues) || containsVendor(usbMfg, vendorValues) || containsVendor(mfg, vendorValues);
            if (isVendorPrinter) {
                break;
            }
        }
        return isVendorPrinter;
    }

    public static String getVendor(NetworkDevice networkDevice) {
        String vendor = null;
        List<NetworkDevice> allInstances = networkDevice.getAllDiscoveryInstances();

        for (NetworkDevice instance : allInstances) {
            Bundle attributes = instance.getTxtAttributes();
            vendor = attributes.getString(ATTRIBUTE__MFG);
            if (!TextUtils.isEmpty(vendor)) break;
            vendor = attributes.getString(ATTRIBUTE__USB_MFG);
            if (!TextUtils.isEmpty(vendor)) break;
            vendor = null;
        }
        return vendor;
    }

    private static boolean containsVendor(String container, String[] vendorValues) {
        if ((container == null) || (vendorValues == null)) return false;
        for (String value : vendorValues) {
            if (containsString(container, value)
                || containsString(container.toLowerCase(Locale.US), value.toLowerCase(Locale.US))
                || containsString(container.toUpperCase(Locale.US), value.toUpperCase(Locale.US)))
                return true;
        }
        return false;
    }

    private static boolean containsString(String container, String contained) {
        return (container != null) && (contained != null) && (container.equalsIgnoreCase(contained) || container.contains(contained + " "));
    }
}

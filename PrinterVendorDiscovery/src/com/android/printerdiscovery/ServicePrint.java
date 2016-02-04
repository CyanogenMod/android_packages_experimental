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
package com.android.printerdiscovery;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.print.PrintManager;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.printerdiscovery.servicediscovery.IDiscoveryListener;
import com.android.printerdiscovery.servicediscovery.mdns.MDnsDiscovery;
import com.android.printerdiscovery.servicediscovery.mdns.MDnsUtils;
import com.android.printerdiscovery.servicediscovery.NetworkDevice;
import com.android.printerdiscovery.servicediscovery.NetworkDiscovery;

import org.xmlpull.v1.XmlPullParser;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicePrint extends PrintService {

    private static final String EXTRA_PRINT_SERVICE_COMPONENT_NAME =
            "EXTRA_PRINT_SERVICE_COMPONENT_NAME";

    private PrintManager mPrintManager;
    private Method mGetEnabledServices;
    private Method mGetResolveInfo;

    private static final class PrinterHashMap extends HashMap<String, NetworkDevice> {
        public void addPrinter(NetworkDevice device) {
            put(device.getBonjourDomainName(), device);
        }
    }

    private static final class VendorInfo {

        public final String mPackageName;
        public final String mVendorID;
        public final String[] mDNSValues;

        public VendorInfo(String packageName, String id, String[] dnsValues) {
            mPackageName = packageName;
            mVendorID = id;
            mDNSValues = dnsValues;
        }

        public VendorInfo(String packageName, VendorInfo vendorInfo) {
            mPackageName = packageName;
            mVendorID = vendorInfo.mVendorID;
            mDNSValues = vendorInfo.mDNSValues;
        }

        public VendorInfo(Resources resources, int vendor_info_id) {
            String[] data = resources.getStringArray(vendor_info_id);
            if ((data == null) || (data.length < 2)) {
                data = new String[] { null, null };
            }
            mPackageName = data[0];
            mVendorID = data[1];
            mDNSValues = (data.length > 2) ? Arrays.copyOfRange(data, 2, data.length) : new String[]{};
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrintManager = (PrintManager)getSystemService(PRINT_SERVICE);
        try {
            mGetEnabledServices = mPrintManager.getClass().getMethod("getEnabledPrintServices");
            Class printServiceInfo = Class.forName("android.printservice.PrintServiceInfo");
            //noinspection unchecked
            mGetResolveInfo = printServiceInfo.getMethod("getResolveInfo");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String PDL__PDF = "application/pdf";
    private static final String PDL__PCLM = "application/PCLm";
    private static final String PDL__PWG_RASTER = "image/pwg-raster";
    private static final String PRIVET_TYPE__PRITNER = "printer";

    private static class DiscoveryHandler extends Handler implements IDiscoveryListener {
        private final WeakReference<AddPrinterCallback> mCallback;

        public interface AddPrinterCallback {
            void addPrinter(NetworkDevice networkDevice);
        }

        public DiscoveryHandler(Looper looper, AddPrinterCallback callback) {
            super(looper);
            mCallback = new WeakReference<>(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            AddPrinterCallback callback = mCallback.get();
            if (callback == null) return;
            if (msg == null) return;
            if (!(msg.obj instanceof NetworkDevice)) return;

            callback.addPrinter((NetworkDevice)msg.obj);
        }

        @Override
        public void onDiscoveryFailed() {
        }

        @Override
        public void onDeviceRemoved(NetworkDevice networkDevice) {

        }

        @Override
        public void onDeviceFound(NetworkDevice networkDevice) {
            Message msg = obtainMessage();
            if (msg != null) {
                msg.obj = networkDevice;
                sendMessage(msg);
            }
        }

        @Override
        public void onActiveDiscoveryFinished() {
        }

        @Override
        public void onDiscoveryFinished() {
        }
    }

    private class DiscoverySession extends PrinterDiscoverySession {

        NetworkDiscovery mDiscoveryTask = null;
        // known vendor information
        HashMap<String, VendorInfo> mVendorInfoHashMap = new HashMap<>();
        // installed plugins info
        HashMap<String, ResolveInfo> mPluginResolveInfo = new HashMap<>();
        // reported printers information
        HashMap<String, PrinterInfo> mVendorHash = new HashMap<>();

        // Printer Hash maps
        HashMap<String, PrinterHashMap> mVendorHashMap = new HashMap<>();
        HashMap<String, PrinterHashMap> mMopriaHashMap = new HashMap<>();
        HashMap<String, PrinterHashMap> mCloudHashMap = new HashMap<>();

        private final DiscoveryHandler mHandler;

        public DiscoverySession() {
            mHandler = new DiscoveryHandler(getMainLooper(), new DiscoveryHandler.AddPrinterCallback() {

                @Override
                public void addPrinter(NetworkDevice networkDevice) {
                    List<PrinterInfo> printerList = new ArrayList<>(1);

                    String vendor = MDnsUtils.getVendor(networkDevice);
                    final Resources resources = getResources();
                    if (vendor == null) vendor = "";
                    for(Map.Entry<String,VendorInfo> entry : mVendorInfoHashMap.entrySet()) {
                        for(String vendorValues : entry.getValue().mDNSValues) {
                            if (vendor.equalsIgnoreCase(vendorValues)) {
                                vendor = entry.getValue().mVendorID;
                                break;
                            }
                        }
                        // intentional pointer check
                        //noinspection StringEquality
                        if ((vendor != entry.getValue().mVendorID) &&
                                MDnsUtils.isVendorPrinter(networkDevice, entry.getValue().mDNSValues)) {
                            vendor = entry.getValue().mVendorID;
                        }
                        // intentional pointer check
                        //noinspection StringEquality
                        if (vendor == entry.getValue().mVendorID) break;
                    }

                    if (TextUtils.isEmpty(vendor)) {
                        return;
                    }

                    PrinterHashMap vendorHash = mVendorHashMap.get(vendor);
                    if (vendorHash == null) {
                        vendorHash = new PrinterHashMap();
                    }

                    vendorHash.addPrinter(networkDevice);
                    mVendorHashMap.put(vendor, vendorHash);

                    if (isMopriaPrinter(networkDevice)) {
                        vendorHash = mMopriaHashMap.get(vendor);
                        if (vendorHash == null) {
                            vendorHash = new PrinterHashMap();
                        }
                        vendorHash.addPrinter(networkDevice);
                        mMopriaHashMap.put(vendor, vendorHash);
                    }

                    if (isGCPPrinter(networkDevice)) {
                        vendorHash = mCloudHashMap.get(vendor);
                        if (vendorHash == null) {
                            vendorHash = new PrinterHashMap();
                        }
                        vendorHash.addPrinter(networkDevice);
                        mCloudHashMap.put(vendor, vendorHash);
                    }

                    ResolveInfo resolveInfo = mPluginResolveInfo.get(getString(R.string.plugin_vendor_morpia));
                    boolean isMopriaPluginInstalled = (resolveInfo != null);
                    boolean isMopriaPluginEnabled = (isMopriaPluginInstalled
                            && (mPluginResolveInfo.get(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name).flattenToString()) != null));

                    resolveInfo = mPluginResolveInfo.get(getString(R.string.plugin_vendor_gcp));
                    boolean isGCPPluginInstalled = (resolveInfo != null);
                    boolean isGCPPluginEnabled = (isGCPPluginInstalled
                            && (mPluginResolveInfo.get(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name).flattenToString()) != null));

                    for(Map.Entry<String, PrinterHashMap> entry : mVendorHashMap.entrySet()) {
                        PrinterInfo printerInfo = null;

                        vendor = entry.getKey();

                        resolveInfo = mPluginResolveInfo.get(vendor);
                        boolean isPluginInstalled = (resolveInfo != null);
                        boolean isPluginEnabled = (isPluginInstalled
                                && (mPluginResolveInfo.get(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name).flattenToString()) != null));

                        boolean addEntry;
                        if (isPluginInstalled) {
                            addEntry = !isPluginEnabled;
                        } else {
                            PrinterHashMap mopriaPrinters = mMopriaHashMap.get(entry.getKey());
                            addEntry = (mopriaPrinters == null) || (entry.getValue().size() > mopriaPrinters.size()) || !isMopriaPluginInstalled;
                        }

                        if (addEntry) {
                            int printerCount = entry.getValue().size();
                            PrinterInfo previousInfo = mVendorHash.get(vendor);
                            if (previousInfo != null) {
                                printerInfo = new PrinterInfo.Builder(previousInfo)
                                        .setName(resources.getQuantityString(R.plurals.vendor_printers_found, printerCount, printerCount, vendor))
                                        .build();
                            } else {
                                printerInfo = new PrinterInfo.Builder(
                                        generatePrinterId(vendor),
                                        resources.getQuantityString(R.plurals.vendor_printers_found, printerCount, printerCount, vendor),
                                        PrinterInfo.STATUS_IDLE)
                                        .build();
                            }
                        }

                        if (printerInfo != null) {
                            mVendorHash.put(vendor, printerInfo);
                            printerList.add(printerInfo);
                        }
                    }

                    boolean showMopria;
                    if (isMopriaPluginInstalled) {
                        showMopria = (!isMopriaPluginEnabled && !mMopriaHashMap.isEmpty());
                    } else {
                        showMopria = false;
                        for(String mopriaVendor : mMopriaHashMap.keySet()) {
                            showMopria |= (mVendorHash.get(mopriaVendor) != null);
                        }
                        showMopria &= (mMopriaHashMap.size() > 1);
                    }

                    if (showMopria) {
                        PrinterInfo printerInfo;

                        vendor = getString(R.string.plugin_vendor_morpia);
                        int printerCount = 0;
                        for(PrinterHashMap printers : mMopriaHashMap.values()) {
                            printerCount += printers.size();
                        }

                        PrinterInfo previousInfo = mVendorHash.get(vendor);
                        if (previousInfo != null) {
                            printerInfo = new PrinterInfo.Builder(previousInfo)
                                    .setName(resources.getQuantityString(R.plurals.vendor_printers_found, printerCount, printerCount, vendor))
                                    .build();
                        } else {
                            printerInfo = new PrinterInfo.Builder(
                                    generatePrinterId(vendor),
                                    resources.getQuantityString(R.plurals.vendor_printers_found, printerCount, printerCount, vendor),
                                    PrinterInfo.STATUS_IDLE)
                                    .build();
                        }
                        if (printerInfo != null) {
                            mVendorHash.put(vendor, printerInfo);
                            printerList.add(printerInfo);
                        }
                    }

                    boolean showGCP;
                    if (isGCPPluginInstalled) {
                        showGCP = (!isGCPPluginEnabled && !mCloudHashMap.isEmpty());
                    } else {
                        showGCP = !mCloudHashMap.isEmpty();
                    }

                    if (showGCP) {
                        PrinterInfo printerInfo;

                        vendor = getString(R.string.plugin_vendor_gcp);
                        int printerCount = 0;
                        for(PrinterHashMap printers : mCloudHashMap.values()) {
                            printerCount += printers.size();
                        }

                        PrinterInfo previousInfo = mVendorHash.get(vendor);
                        if (previousInfo != null) {
                            printerInfo = new PrinterInfo.Builder(previousInfo)
                                    .setName(resources.getQuantityString(R.plurals.vendor_printers_found, printerCount, printerCount, vendor))
                                    .build();
                        } else {
                            printerInfo = new PrinterInfo.Builder(
                                    generatePrinterId(vendor),
                                    resources.getQuantityString(R.plurals.vendor_printers_found, printerCount, printerCount, vendor),
                                    PrinterInfo.STATUS_IDLE)
                                    .build();
                        }
                        if (printerInfo != null) {
                            mVendorHash.put(vendor, printerInfo);
                            printerList.add(printerInfo);
                        }
                    }

                    if (!printerList.isEmpty()) addPrinters(printerList);
                }
            });
        }

        List<String> getServicesNames(NetworkDevice networkDevice) {
            List<String> mDnsServices = new ArrayList<>();
            for(NetworkDevice device : networkDevice.getAllDiscoveryInstances()) {
                mDnsServices.add(device.getBonjourService());
            }
            return mDnsServices;
        }

        private boolean isMopriaPrinter(NetworkDevice networkDevice) {
            final String ippService = getString(R.string.mdns_service__ipp);

            List<String> mdnsServices = getServicesNames(networkDevice);

            if (!mdnsServices.contains(ippService)) return false;
            Bundle ippAttributes = networkDevice.getTxtAttributes(ippService);
            String pdls = ippAttributes.getString("pdl");
            return (!TextUtils.isEmpty(pdls)
                    && (pdls.contains(PDL__PDF)
                    || pdls.contains(PDL__PCLM)
                    || pdls.contains(PDL__PWG_RASTER)));
        }

        private boolean isGCPPrinter(NetworkDevice networkDevice) {
            final String privetService = getString(R.string.mdns_service__privet);
            List<String> mdnsServices = getServicesNames(networkDevice);

            if (!mdnsServices.contains(privetService)) return false;
            Bundle privetAttributes = networkDevice.getTxtAttributes(privetService);
            String type = privetAttributes.getString("type");
            return (!TextUtils.isEmpty(type)
                    && (type.contains(PRIVET_TYPE__PRITNER)));
        }

        @Override
        public void onStartPrinterDiscovery(List<PrinterId> priorityList) {

            List<Object> enabledServices;

            removePrinters(priorityList);
            mPluginResolveInfo.clear();

            mVendorHash.clear();
            mVendorInfoHashMap.clear();

            TypedArray testArray = getResources().obtainTypedArray(R.array.known_print_plugin_vendors);
            for(int i = 0; i < testArray.length(); i++) {
                int arrayID = testArray.getResourceId(i, 0);
                if (arrayID != 0) {
                    VendorInfo info = new VendorInfo(getResources(), arrayID);
                    mVendorInfoHashMap.put(info.mVendorID, info);
                    mVendorInfoHashMap.put(info.mPackageName, info);
                }
            }
            testArray.recycle();

            List<Object> ourServiceRefs = new ArrayList<>();
            ResolveInfo ourInfo = getPackageManager().resolveService(
                    new Intent(PrintService.SERVICE_INTERFACE)
                        .setPackage(getPackageName()),
                    PackageManager.GET_META_DATA
            );

            List<ResolveInfo> installedPrintServices =
                    getPackageManager().
                            queryIntentServices(
                                    new Intent(PrintService.SERVICE_INTERFACE),
                                    PackageManager.GET_META_DATA);
            if (installedPrintServices == null) {
                installedPrintServices = Collections.emptyList();
            }

            try {
                //noinspection unchecked
                enabledServices = (List<Object>) mGetEnabledServices.invoke(mPrintManager);
            } catch (Exception ignored) {
                enabledServices = Collections.emptyList();
            }

            // process installed services
            for(ResolveInfo printService : installedPrintServices) {
                try {
                    String vendorName;
                    // check if we're processing ourselves
                    if (TextUtils.equals(ourInfo.serviceInfo.name, printService.serviceInfo.name)
                            && TextUtils.equals(ourInfo.serviceInfo.packageName, printService.serviceInfo.packageName)) {
                        ourServiceRefs.add(printService);
                        continue;
                    }
                    // extract vendor name information if available
                    vendorName = null;
                    XmlResourceParser parser = printService.serviceInfo.loadXmlMetaData(getPackageManager(),
                            PrintService.SERVICE_META_DATA);
                    if (parser != null) {
                        try {
                            int type = 0;
                            while (type != XmlPullParser.END_DOCUMENT && type != XmlPullParser.START_TAG) {
                                type = parser.next();
                            }
                            Resources resources = getPackageManager().getResourcesForApplication(
                                    printService.serviceInfo.applicationInfo);
                            vendorName = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "vendor");
                            if (!TextUtils.isEmpty(vendorName) && vendorName.startsWith("@")) {
                                vendorName = resources.getString(Integer.valueOf(vendorName.substring(1)));
                                if (TextUtils.equals(vendorName, getResources().getStringArray(R.array.known_print_vendor_info_for_mopria)[1]))
                                    vendorName = getString(R.string.plugin_vendor_morpia);
                            }
                            if (TextUtils.equals(printService.serviceInfo.packageName, getResources().getStringArray(R.array.known_print_vendor_info_for_gcp)[0])) {
                                vendorName = getString(R.string.plugin_vendor_gcp);
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        } finally {
                            parser.close();
                        }
                    }
                    if (!TextUtils.isEmpty(vendorName)) {
                        VendorInfo vendorInfo = mVendorInfoHashMap.get(vendorName);
                        if (vendorInfo != null) {
                            if (!TextUtils.equals(printService.serviceInfo.packageName, vendorInfo.mPackageName)) {
                                vendorInfo = new VendorInfo(ourInfo.serviceInfo.packageName, vendorInfo);
                            }
                        } else {
                            vendorInfo = new VendorInfo(printService.serviceInfo.packageName, vendorName, new String[] {vendorName});
                        }
                        mVendorInfoHashMap.put(vendorInfo.mVendorID, vendorInfo);

                        mPluginResolveInfo.put(vendorName, printService);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // process enabled services
            for(Object printService : enabledServices) {
                try {
                    ResolveInfo other = (ResolveInfo) mGetResolveInfo.invoke(printService);
                    // check if we're processing ourselves
                    if (TextUtils.equals(ourInfo.serviceInfo.name, other.serviceInfo.name)
                            && TextUtils.equals(ourInfo.serviceInfo.packageName, other.serviceInfo.packageName)) {
                        ourServiceRefs.add(printService);
                        continue;
                    }
                    String pluginName = new ComponentName(other.serviceInfo.packageName, other.serviceInfo.name).flattenToString();
                    mPluginResolveInfo.put(pluginName, other);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // remove ourselves from the lists
            for(Object printService : ourServiceRefs) {
                //noinspection SuspiciousMethodCalls
                installedPrintServices.remove(printService);
                enabledServices.remove(printService);
            }

            mVendorInfoHashMap.put(getString(R.string.plugin_vendor_morpia), mVendorInfoHashMap.get(getResources().getStringArray(R.array.known_print_vendor_info_for_mopria)[1]));

            // start a discovery task
            stopDiscoveryTask();
            mDiscoveryTask = new NetworkDiscovery(ServicePrint.this, true, null, 0);
            mDiscoveryTask.addDiscoveryMethod(new MDnsDiscovery(getResources().getStringArray(R.array.array__mdns_services)));
            mDiscoveryTask.addDiscoveryListener(mHandler);
            mDiscoveryTask.startDiscovery();
        }

        @Override
        public void onStopPrinterDiscovery() {
            stopDiscoveryTask();

            mPluginResolveInfo.clear();

            mVendorHash.clear();

            mMopriaHashMap.clear();
            mVendorHashMap.clear();
            mCloudHashMap.clear();
        }

        private void stopDiscoveryTask() {
            if (mDiscoveryTask != null) {
                mDiscoveryTask.removeDiscoveryListener(mHandler);
                mDiscoveryTask.stopDiscovery();
                mDiscoveryTask = null;

            }
        }

        @Override
        public void onValidatePrinters(List<PrinterId> printerIds) {
        }

        @Override
        public void onStartPrinterStateTracking(PrinterId printerId) {

            final ResolveInfo installedResolveInfo = mPluginResolveInfo.get(printerId.getLocalId());
            final ResolveInfo enabledResolveInfo =
                    (installedResolveInfo != null) ? mPluginResolveInfo.get(
                            new ComponentName(installedResolveInfo.serviceInfo.packageName,
                                    installedResolveInfo.serviceInfo.name).flattenToString()
                    ) : null;

            if (enabledResolveInfo != null) {
                List<PrinterId> list = new ArrayList<>(1);
                list.add(printerId);
                removePrinters(list);
                return;
            }
            AlertDialog dialog;
            if (installedResolveInfo != null) {
                dialog =
                        new AlertDialog.Builder(ServicePrint.this)
                                .setTitle(R.string.dialog_title__enable_service)
                                .setMessage(R.string.dialog_body__enable_service)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        startActivity(new Intent(Settings.ACTION_PRINT_SETTINGS)
                                                .putExtra(EXTRA_PRINT_SERVICE_COMPONENT_NAME,
                                                        new ComponentName(installedResolveInfo.serviceInfo.packageName, installedResolveInfo.serviceInfo.name).flattenToString())
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            } else {

                final VendorInfo info = mVendorInfoHashMap.get(printerId.getLocalId());

                dialog =
                        new AlertDialog.Builder(ServicePrint.this)
                                .setTitle(R.string.dialog_title__install_service)
                                .setMessage(R.string.dialog_body__install_service)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        startActivity(
                                                new Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(((info != null) ?
                                                                getString(R.string.uri__package_details, info.mPackageName) :
                                                                getString(R.string.url__print_serivces))))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            }
            try {
                dialog.show();
            } catch(Exception ignored) {
                Toast.makeText(ServicePrint.this, R.string.toast_system_alert_permission, Toast.LENGTH_LONG).show();

            }

        }

        @Override
        public void onStopPrinterStateTracking(PrinterId printerId) {
        }

        @Override
        public void onDestroy() {
            onStopPrinterDiscovery();
        }
    }

    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        return new DiscoverySession();
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        printJob.complete();
    }

    @Override
    protected void onPrintJobQueued(PrintJob printJob) {
        printJob.cancel();
    }
}

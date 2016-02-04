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

package com.android.printerdiscovery.servicediscovery;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Common class to perform network-related tests. For example:
 * <ul>
 * <li>isConnected: returns true if network connectivity exists
 * <li>isMobileNetworkConnected: returns true if Mobile Network is active
 * <li>isWiFiConnected: returns true if WiFi is active
 * <li>[... Your network-related test here]
 * </ul>
 */
@SuppressWarnings("unused")
public class NetworkUtils {
    public static final String TAG = NetworkUtils.class.getSimpleName();

    private static final int MULTICAST_TTL = 255;
    private static boolean mIsDebuggable = false;

    // Need a fake SSID for ethernet.  Create one that is longer than 32 characters
    // as SSID must be 0-32 chars long and we don't want anything that could really
    // be returned by a wireless SSID.  This one is 35 characters.
    // Should match the value in shared/NetworkUtilities.
    @SuppressWarnings("FieldCanBeLocal")
    private static String ETHERNET_SSID = "Ethernet901234567890123456789012345";
    private static String ETHERNET_NETIFC = "eth0";

    public static boolean isWifiConnected(Context context) {
        boolean connected = false;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info != null) {
            if ((info.getSSID() != null) && (info.getIpAddress() != 0)) {
                connected = true;
            }
        }
        return connected;
    }

    public static boolean isConnectedToEthernet(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ethInfo = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2)
            ethInfo = getEthernetInfo(connMgr);
        return ethInfo != null && ethInfo.isConnected();
    }

    public static boolean isConnectedToWifiOrEthernet(Context context) {
        return isWifiConnected(context) || isConnectedToEthernet(context);
    }

    /**
     * Returns an instance of the ConnectivityManager for handling management of
     * network connections.
     *
     * @param context Context running discovery
     * @return an instance of the ConnectivityManager for handling management of network connections
     */
    private static ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private static NetworkInfo getEthernetInfo(ConnectivityManager connMgr) {
        //noinspection deprecation
        return connMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
    }

    @SuppressLint("NewApi")
    public static InetAddress getEthernetBroadcast() {
        NetworkInterface netIf;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
            return null;

        try {
            netIf = NetworkInterface.getByName(ETHERNET_NETIFC);
        } catch (SocketException e) {
            netIf = null;
        }
        if (netIf == null)
            return null;

        InetAddress broadcastAddress = null;
        List<InterfaceAddress> addresses = netIf.getInterfaceAddresses();
        if ((addresses != null) && !addresses.isEmpty()) {

            for (InterfaceAddress address : addresses) {
                InetAddress bAddr = address.getBroadcast();
                if (bAddr != null) {
                    broadcastAddress = bAddr;
                }
            }
        }
        return broadcastAddress;
    }

    /**
     * @return If there is Wi-Fi and DHCP info returns Broadcast address
     * else returns null
     * @throws UnknownHostException
     */
    public static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
        if (connectedToEthernet(context)) {
            return getEthernetBroadcast();
        } else {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

            if ((wifiInfo == null) || (dhcpInfo == null)) {
                return null;
            }
            int hostAddr = wifiInfo.getIpAddress();
            int broadcastAddress = (hostAddr & dhcpInfo.netmask) | ~dhcpInfo.netmask;
            byte[] broadcastAddressBytes = {
                    (byte) (broadcastAddress & 0xFF),
                    (byte) ((broadcastAddress >> 8) & 0xFF),
                    (byte) ((broadcastAddress >> 16) & 0xFF),
                    (byte) ((broadcastAddress >> 24) & 0xFF)};

            return InetAddress.getByAddress(broadcastAddressBytes);
        }
    }

    public static NetworkInterface getNetworkIFC(Context context, String networkIFC) throws IOException {
        NetworkInterface netIf = null;

        if (!TextUtils.isEmpty(networkIFC)) {
            netIf = NetworkInterface.getByName(networkIFC);
        } else if (connectedToEthernet(context)) {
            netIf = NetworkInterface.getByName(ETHERNET_NETIFC);
        } else {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int intaddr = wifiInfo.getIpAddress();
                byte[] byteaddr = new byte[]{
                        (byte) (intaddr & 0xff),
                        (byte) (intaddr >> 8 & 0xff),
                        (byte) (intaddr >> 16 & 0xff),
                        (byte) (intaddr >> 24 & 0xff)};
                InetAddress addr = InetAddress.getByAddress(byteaddr);
                netIf = NetworkInterface.getByInetAddress(addr);
            }
        }
        return netIf;
    }

    /**
     * Ensures that the caller receives an usable multicast socket.
     * In case the network configuration does not have a default gateway
     * set, the multicast socket might not work. This is the case when
     * the device is connected to a Wireless Direct printer. In that
     * cases, force a network interface into the socket.
     *
     * @return A ready-to-use multicast socket.
     */
    public static MulticastSocket createMulticastSocket(Context context, String networkIFC)
            throws IOException {

        NetworkInterface netIf = getNetworkIFC(context, networkIFC);
        MulticastSocket multicastSocket = new MulticastSocket();

        if (netIf != null) {
            multicastSocket.setNetworkInterface(netIf);
        }

        multicastSocket.setTimeToLive(MULTICAST_TTL);
        return multicastSocket;
    }

    public static MulticastSocket createMulticastSocket(Context context)
            throws IOException {
        return createMulticastSocket(context, null);
    }


    /**
     * When Mobile Network is active, all data traffic will use this connection
     * by default. Should not coexist with other connections.
     *
     * @param context Context running discovery
     * @return true if Mobile Network is active
     */
    public static boolean isMobileNetworkConnected(Context context) {
        NetworkInfo netInfo = getConnectivityManager(context).getActiveNetworkInfo();

        return (netInfo != null) && (netInfo.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean connectedToEthernet(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//      NetworkInfo ethInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
//      ethInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        NetworkInfo ethInfo = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2)
            ethInfo = getEthernetInfo(connMgr);
        return ethInfo != null && ethInfo.isConnectedOrConnecting();
    }

    public static boolean isWirelessDirect(Context context) {
        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connManager.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnected() && (netInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
            WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

            if ((dhcpInfo != null) && (dhcpInfo.gateway == 0)) {
                if (mIsDebuggable) Log.d(TAG, "isWirelessDirect: probably wireless direct.");
                return true;
            }
        }
        return false;
    }

    public static boolean isConnectedAndNotWirelessDirect(Context context) {
        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connManager.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnected()) {
            if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

                if ((dhcpInfo != null) && (dhcpInfo.gateway == 0)) {
                    if (mIsDebuggable)
                        Log.d(TAG, "isConnectedButNotWirelessDirect: probably wireless direct.");
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    public static String getCurrentSSID(Context context) {
        String currentSSID = null;
        if (isWifiConnected(context)) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            currentSSID = (wifiInfo != null) ? wifiInfo.getSSID() : null;
        } else if (isConnectedToEthernet(context)) {
            currentSSID = ETHERNET_SSID;
        }
        return currentSSID;
    }

}

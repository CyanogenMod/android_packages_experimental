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

package org.mopria.servicediscovery;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class NetworkDiscovery {

    public static final int FALLBACK_DELAY = 5000;
    private static final int MAX_ACTIVE_QUERIES = 10;
    private static final int MAX_DELAY_SECONDS = 60;
    private static final String TAG = "NetworkDiscovery";
    private static final int BUFFER_LENGTH = 4 * 1024;

    private final List<IDiscoveryListener> mListeners = new ArrayList<>();
    private final Object mLock = new Object();
    private final LinkedHashMap<String, NetworkDevice> mDiscoveredPrinters = new LinkedHashMap<>();
    private final HashMap<String, List<NetworkDevice>> mDiscoveredPrintersByIP = new HashMap<>();
    private final List<IDiscovery> mDiscoveryMethods = new ArrayList<>();
    private final Context mContext;
    private final String mNetworkIFC;

    private Thread mQueryThread = null;
    private int mQueriesSent = 0;
    private ListenerThread mListenerThread = null;
    private boolean mIsActiveDiscovery = false;
    private final int mFallbackDelay;
    private final DuplicateAddressArbitrator mArbitrator;

    private final List<DiscoveryFilter> mFilters = new ArrayList<>();

    public NetworkDiscovery(Context context) {
        this(context, true, null, FALLBACK_DELAY);
    }

    public NetworkDiscovery(Context context,
                            boolean activeDiscovery,
                            String networkIFCName,
                            int fallbackDelay) {
        this(context, activeDiscovery, networkIFCName, fallbackDelay, null);
    }

    public NetworkDiscovery(Context context,
                            boolean activeDiscovery,
                            String networkIFCName,
                            int fallbackDelay,
                            DuplicateAddressArbitrator arbitrator) {
        mContext = context.getApplicationContext();
        mIsActiveDiscovery = activeDiscovery;
        mNetworkIFC = (TextUtils.isEmpty(networkIFCName) ? null : networkIFCName);
        mFallbackDelay = fallbackDelay;
        mArbitrator = arbitrator;
    }

    public void addDiscoveryMethod(IDiscovery discoveryMethod) {
        mDiscoveryMethods.add(discoveryMethod);
    }

    public void stopDiscovery() {
        synchronized (mLock) {
            if (mListenerThread != null) {
                mListenerThread.releaseSocket();
                mListenerThread.cancel(true);
                mListenerThread = null;
            }
            stopQueryThread();
        }
    }

    public void rediscover(boolean isActive) {
        synchronized (mLock) {
            stopDiscovery();
            startDiscovery();
        }
    }

    public void startDiscovery() {
        synchronized (mLock) {
            mDiscoveredPrinters.clear();
            mDiscoveredPrintersByIP.clear();
            for (IDiscovery discoverMethod : mDiscoveryMethods) {
                discoverMethod.clear();
            }
            if (mListenerThread == null) {
                mListenerThread = new ListenerThread();
                mListenerThread.start();
            }
        }
    }

    public int getNumberOfDiscoveredPrinters() {
        return mDiscoveredPrinters.size();
    }

    public ArrayList<NetworkDevice> getDiscoveredDevices() {
        return new ArrayList<>(mDiscoveredPrinters.values());
    }

    public boolean haveDevicesBeenFound() {
        return !mDiscoveredPrinters.isEmpty();
    }

    public void addDiscoveryListener(IDiscoveryListener listener) {
        synchronized (mLock) {
            if (!this.mListeners.contains(listener))
                this.mListeners.add(listener);
                for(NetworkDevice device : mDiscoveredPrinters.values()) {
                    listener.onDeviceFound(device);
                }
        }
    }

    public void removeDiscoveryListener(IDiscoveryListener listener) {
        synchronized (mLock) {
            this.mListeners.remove(listener);
        }
    }

    public void addDeviceFilter(DiscoveryFilter filter) {
        synchronized (mLock) {
            mFilters.add(filter);
        }
    }

    public void removeDeviceFilter(DiscoveryFilter filter) {
        synchronized (mLock) {
            mFilters.remove(filter);
        }
    }

    public void clearDeviceFilter() {
        synchronized (mLock) {
            mFilters.clear();
        }
    }

    public void setDeviceFilters(List<DiscoveryFilter> filters) {
        synchronized (mLock) {
            mFilters.clear();
            mFilters.addAll(filters);
        }
    }

    private class ListenerThread extends Thread {

        private DatagramSocket mSocket = null;

        private DatagramSocket createSocket() throws IOException {
            releaseSocket();

            // SNMP requires a broadcast socket, while mDNS requires a multicast socket with a TTL set.
            // Let's create an hybrid one and make everyone happy.
            MulticastSocket socket = NetworkUtils.createMulticastSocket(mContext, mNetworkIFC);
            socket.setBroadcast(true);
            socket.setReuseAddress(true);
            socket.setSoTimeout(0);
            mSocket = socket;
            return socket;
        }

    private void releaseSocket() {
            if ((mSocket != null) && !mSocket.isClosed()) {
                mSocket.close();
            }
            mSocket = null;
        }

    private DatagramSocket getSocket() {
            return mSocket;
        }

        AtomicBoolean mCancelled = new AtomicBoolean(false);
        public void cancel(boolean interrupt) {
            mCancelled.set(true);
            if (interrupt) interrupt();
        }

        public boolean isCancelled() {
            return mCancelled.get();
        }

        private boolean setupSocket() {
            try {
                createSocket();
            } catch (IOException e) {
                releaseSocket();
                e.printStackTrace();
            }
            return (getSocket() != null);
        }

        @Override
        public void run() {
            if (setupSocket()) {
                startQueryThread();
                receiveResponsePackets(getSocket());
                fireDiscoveryFinished();
            } else {
                fireDiscoveryFailed();
            }
        }

        /*
         * The algorithm for receiving the response packets will decrease the
         * timeout according to the search results. Socket timeout starts with a
         * value of 8s. If no printer is found, the socket timeout is decreased by
         * 2s until it reaches 0 and the algorithm stops listening for new
         * responses. So when no printer is found, the sequence of socket timeouts
         * is 8, 6, 4, and 2s, adding up to 20s of wait time. When a printer is
         * found, both timeout and decay are set to 5s, which means that the thread
         * will receive new packets with a timeout of 5s until no packet is
         * received. The first time the receive method reaches the timeout without
         * receiving any packets, the algorithm finishes.
         */
        private void receiveResponsePackets(final DatagramSocket socket) {
            byte buffer[] = new byte[BUFFER_LENGTH];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            if (socket == null) return;

            while (!Thread.interrupted() && !isCancelled()) {
                try {
                    socket.receive(packet);
                    if (!Thread.interrupted()) {
                        processIncomingPacket(packet);
                        // Resets the packet length to reuse the packet.
                        packet.setLength(BUFFER_LENGTH);
                    }
                } catch (IOException ioe) {
                    if (socket.isClosed() || !socket.isConnected()) {
                        break;
                    }
                }
            }
        }

    }

    private DatagramSocket getSocket() {
        synchronized (mLock) {
            return mListenerThread != null ? mListenerThread.getSocket() : null;
        }
    }


    boolean mUseFallback = false;
    private void sendQueryPacket() throws IOException {
        final DatagramSocket socket = getSocket();
        if (socket == null) return;

        if (mDiscoveryMethods.isEmpty()) return;

        ArrayList<DatagramPacket> datagramList = new ArrayList<>();
        for (IDiscovery discoverMethod : mDiscoveryMethods) {
            if (!discoverMethod.isFallback() || mUseFallback)
            Collections.addAll(datagramList, discoverMethod.createQueryPackets());
        }

        for(DatagramPacket packet : datagramList) {
            socket.send(packet);
        }

        mQueriesSent++;
    }

    /*
    * Returns the next wait interval, in milliseconds, using an exponential
    * backoff algorithm.
    */
    private int getQueryDelayInMillis() {
        int delayInSeconds = 1;

        int first = 1;
        int second = 1;
        int index;

        if (mQueriesSent > MAX_ACTIVE_QUERIES) {
            delayInSeconds = MAX_DELAY_SECONDS;
        } else {

            for (index = 1; index < mQueriesSent; index++) {
                if (index <= 1) {
                    delayInSeconds = index;
                } else {
                    delayInSeconds = first + second;
                    first = second;
                    second = delayInSeconds;
                }
            }
        }

        if (delayInSeconds >= MAX_DELAY_SECONDS) {
            delayInSeconds = MAX_DELAY_SECONDS;
            if (mIsActiveDiscovery) {
                mIsActiveDiscovery = false;
                fireActiveDiscoveryFinished();
            }
        }
        // convert seconds to millis
        return delayInSeconds * 1000;
    }

    private void startQueryThread() {
        synchronized (mLock) {
        stopQueryThread();
            final ListenerThread listenerThread = mListenerThread;
        mQueryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                    if (listenerThread == null) return;
                mQueriesSent = (mIsActiveDiscovery ? 0 : MAX_ACTIVE_QUERIES);
                boolean retry = true;
                    final long startTime = System.currentTimeMillis();
                    mUseFallback = false;
                    while (retry && !Thread.currentThread().isInterrupted() && !listenerThread.isCancelled()) {
                    try {
                        sendQueryPacket();
                        Thread.sleep(getQueryDelayInMillis());
                            if (!mUseFallback && ((System.currentTimeMillis() - startTime) > mFallbackDelay)) {
                                mUseFallback = mDiscoveredPrinters.isEmpty();
                            }
                            // coverity[FB.REC_CATCH_EXCEPTION]
                    } catch (Exception e) {
                        retry = false;
                    }
                }
            }
        });
        mQueryThread.start();
    }
    }
    private void stopQueryThread() {
        if (mQueryThread != null) {
            if (mQueryThread.isAlive()) mQueryThread.interrupt();
            mQueryThread = null;
        }
    }

    private void processIncomingPacket(DatagramPacket packet) {
        List<DiscoveryFilter> filters = new ArrayList<>();
        List<ServiceParser> serviceParsers = new ArrayList<>();
        int port = packet.getPort();
        for (IDiscovery discoveryMethod : mDiscoveryMethods) {
            if (port == discoveryMethod.getPort()) {
                serviceParsers.addAll(discoveryMethod.parseResponse(packet));
                break;
            }
        }

        synchronized (mLock) {
            filters.addAll(mFilters);
        }

        boolean satisfiesFilters;

        for (ServiceParser parser : serviceParsers) {
            NetworkDevice device = new NetworkDevice(parser);

            satisfiesFilters = true;
            for(DiscoveryFilter filter : filters) {
                satisfiesFilters = filter.meetsFilterCriteria(device);
                if (!satisfiesFilters) break;
            }
            if (!satisfiesFilters) {
                continue;
            }

            final String key = device.getDeviceIdentifier();
            NetworkDevice discoveredNetworkDevice = mDiscoveredPrinters.get(key);

            if ((discoveredNetworkDevice != null) && !device.getInetAddress().equals(discoveredNetworkDevice.getInetAddress())) {
                fireDeviceRemoved(discoveredNetworkDevice);
                discoveredNetworkDevice = null;
            }

            List<NetworkDevice> ipList = mDiscoveredPrintersByIP.get(device.getInetAddress().toString());
            if (ipList == null) ipList = new ArrayList<>();

            if (!ipList.isEmpty() && (mArbitrator != null)) {
                DuplicateAddressResolution resolution = mArbitrator.duplicateResolution(ipList, device);

                if (resolution == null) {
                    continue;
                }
                if (resolution.mExistingDeviceToRemove != null) {
                    mDiscoveredPrinters.remove(resolution.mExistingDeviceToRemove.getDeviceIdentifier());
                    ipList.remove(resolution.mExistingDeviceToRemove);
                    fireDeviceRemoved(resolution.mExistingDeviceToRemove);
                }
                if (!resolution.mAddNewDevice) {
                    device = null;
                }
            }

            if (device == null) continue;

            // updating the previously stored networkDevice object
            if (discoveredNetworkDevice != null) {
                discoveredNetworkDevice.addDiscoveryInstance(device);
                device = discoveredNetworkDevice;
            } else {
                mDiscoveredPrinters.put(key, device);
            }
            if (!ipList.contains(device)) {
                ipList.add(device);
                mDiscoveredPrintersByIP.put(device.getInetAddress().toString(), ipList);
            }

            fireDeviceFound(device);

        }
    }

    private void fireDeviceRemoved(NetworkDevice networkDevice) {
        synchronized (mLock) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onDeviceRemoved(networkDevice);
            }
        }
    }

    private void fireDeviceFound(NetworkDevice networkDevice) {
        synchronized (mLock) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onDeviceFound(networkDevice);
            }
        }
    }

    private void fireDiscoveryFinished() {
        synchronized (mLock) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onDiscoveryFinished();
            }
        }
    }

    private void fireActiveDiscoveryFinished() {
        synchronized (mLock) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onActiveDiscoveryFinished();
            }
        }
    }

    private void fireDiscoveryFailed() {
        synchronized (mLock) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onDiscoveryFailed();
            }
        }
    }
}

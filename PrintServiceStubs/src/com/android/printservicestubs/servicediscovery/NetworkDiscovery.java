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

package com.android.printservicestubs.servicediscovery;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.printservicestubs.R;
import com.android.printservicestubs.servicediscovery.mdns.MDnsDiscovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Discovers devices on the network and notifies the listeners about those devices. The session is
 * automatically started and stopped if listeners come and go. For the first {@value
 * QueryThread#FAST_QUERY_PERIOD_MS} ms the printers are queried eagerly, after wards with much
 * lower priority. To make the querying eager again, call {@link #reactivate()}.
 */
public class NetworkDiscovery {
    private static final String LOG_TAG = "NetworkDiscovery";

    /**
     * Currently active clients of the discovery session.
     */
    private static final @NonNull ArrayList<DiscoveryListener> sListeners = new ArrayList<>();

    /**
     * If the network discovery is running, this stores the instance. There can always be at most
     * once instance running. The instance is automatically created when the first {@link
     * #sListeners listener} is added and destroyed once the last one is removed.
     */
    private static @Nullable NetworkDiscovery sInstance = null;

    /**
     * Method used for discovering printers
     */
    private final @NonNull MDnsDiscovery mDiscoveryMethod;

    /**
     * List of discovered printers indexed by device identifier
     */
    private final @NonNull LinkedHashMap<String, NetworkDevice> mDiscoveredPrinters;

    /**
     * The context the discovery session runs in
     */
    private final Context mContext;

    /**
     * Thread sending the broadcasts that should make the device announce them self
     */
    private final @NonNull QueryThread mQueryThread;

    /**
     * Thread that receives the announcements of new devices and processes them
     */
    private final @NonNull ListenerThread mListenerThread;

    /**
     * Socket used to discover devices (both query and listen) if the discovery session is active.
     */
    private @NonNull MulticastSocket mSocket;

    /**
     * Create and start a new network discovery session.
     *
     * @param context The context requesting the start of the session.
     *
     * @throws IOException If the discovery session could not be started
     */
    private NetworkDiscovery(@NonNull Context context) throws IOException {
        mContext = context;
        mDiscoveredPrinters = new LinkedHashMap<>();

        mDiscoveryMethod = new MDnsDiscovery(
                context.getResources().getStringArray(R.array.mdns_services));

        mListenerThread = new ListenerThread();
        mQueryThread = new QueryThread();

        startDiscovery();

        // Stop or start session when network connection changes
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    stopDiscovery();
                } catch (InterruptedException e) {
                    // We cannot recover for this
                    throw new RuntimeException("Cannot stop session", e);
                }

                startDiscovery();
            }
        };

        context.registerReceiver(receiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    /**
     * Register a new listener for network devices. If this is the first listener, this starts a new
     * discovery session.
     *
     * @param listener Listener to register.
     * @param context  Context the listener is running in.
     *
     * @throws IOException If the discovery session could not be started
     */
    public static void onListenerAdded(@NonNull DiscoveryListener listener,
            @NonNull Context context) throws IOException {
        listener = Preconditions.checkNotNull(listener, "listener");
        context = Preconditions.checkNotNull(context, "context");

        synchronized (sListeners) {
            sListeners.add(listener);

            if (sInstance == null) {
                sInstance = new NetworkDiscovery(context);
            } else {
                sInstance.onListenerAdded(listener);
            }
        }
    }

    /**
     * Remove a previously registered listener for network devices. If this is the last listener,
     * the discovery session is terminated.
     *
     * @param listener The listener to remove
     *
     * @throws InterruptedException If the thread was interrupted while waiting for the session to
     *                              finish.
     */
    public static void removeDiscoveryListener(@NonNull DiscoveryListener listener)
            throws InterruptedException {
        listener = Preconditions.checkNotNull(listener, "listener");

        synchronized (sListeners) {
            sListeners.remove(listener);

            if (sListeners.isEmpty()) {
                sInstance.stopDiscovery();
                sInstance = null;
            }
        }
    }

    /**
     * Reactivate discovery, i.e. poll quickly for network printers.
     */
    public static void reactivate() {
        // creation of the instance if synchronized by sListeners, see onListenerAdded and
        // removeDiscoveryListener
        synchronized (sListeners) {
            if (sInstance != null) {
                sInstance.mQueryThread.reactivate();
            }
        }
    }

    /**
     * Start the session if the network is connected.
     */
    private void startDiscovery() {
        synchronized (this) {
            // if mSocket exist the discovery is running.
            if (mSocket == null) {
                ConnectivityManager cm =
                        (ConnectivityManager) mContext
                                .getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();

                if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                    // Recreate socket
                    try {
                        mSocket = NetworkUtils.createMulticastSocket(mContext, null);

                        mSocket.setBroadcast(true);
                        mSocket.setReuseAddress(true);
                        mSocket.setSoTimeout(0);

                        // Restart session
                        mListenerThread.start();
                        mQueryThread.start();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Cannot create socket", e);
                        mSocket = null;
                    }

                }
            }
        }
    }

    /**
     * Stop the discovery session if a session is running.
     *
     * @throws InterruptedException If the current thread was interrupted while the session was
     *                              cleaned up.
     */
    private void stopDiscovery() throws InterruptedException {
        synchronized (this) {
            if (mSocket != null) {
                // Closing the socket causes IOExceptions on operations on this socket. This in turn
                // will end the threads.
                mSocket.close();

                mListenerThread.join();
                mQueryThread.join();

                mSocket = null;

                synchronized (mDiscoveredPrinters) {
                    for (NetworkDevice device : mDiscoveredPrinters.values()) {
                        onDeviceRemoved(device);
                    }

                    mDiscoveredPrinters.clear();
                }
            }
        }
    }

    /**
     * If a new listener was added while the session was already running, announce all already found
     * devices to the new listener.
     *
     * @param listener The listener that was just added.
     */
    private void onListenerAdded(@NonNull DiscoveryListener listener) {
        synchronized (mDiscoveredPrinters) {
            for (NetworkDevice device : mDiscoveredPrinters.values()) {
                listener.onDeviceFound(device);
            }
        }
    }

    /**
     * Notify all currently registered listeners that a new device was removed.
     *
     * @param networkDevice The device that was removed
     */
    private void onDeviceRemoved(@NonNull NetworkDevice networkDevice) {
        synchronized (sListeners) {
            final int numListeners = sListeners.size();
            for (int i = 0; i < numListeners; i++) {
                sListeners.get(i).onDeviceRemoved(networkDevice);
            }
        }
    }

    /**
     * Notify all currently registered listeners that a new device was found.
     *
     * @param networkDevice The device that was found
     */
    private void onDeviceFound(@NonNull NetworkDevice networkDevice) {
        synchronized (sListeners) {
            final int numListeners = sListeners.size();
            for (int i = 0; i < numListeners; i++) {
                sListeners.get(i).onDeviceFound(networkDevice);
            }
        }
    }

    /**
     * Thread receiving and processing the packets that announce network devices.
     * <p/>
     * If devices are found or removed {@link #onDeviceFound(NetworkDevice)} and {@link
     * #onDeviceRemoved(NetworkDevice)} are called.
     */
    private class ListenerThread extends Thread {
        private static final int BUFFER_LENGTH = 4 * 1024;

        @Override
        public void run() {
            while (true) {
                byte[] buffer = new byte[BUFFER_LENGTH];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    mSocket.receive(packet);

                    if (packet.getPort() != mDiscoveryMethod.getPort()) {
                        continue;
                    }

                    ArrayList<ServiceParser> serviceParsers = mDiscoveryMethod
                            .parseResponse(packet);

                    final int numParsers = serviceParsers.size();
                    for (int i = 0; i < numParsers; i++) {
                        ServiceParser parser = serviceParsers.get(i);
                        NetworkDevice device = new NetworkDevice(parser);
                        String key = device.getDeviceIdentifier();
                        NetworkDevice discoveredNetworkDevice = mDiscoveredPrinters.get(key);

                        if (discoveredNetworkDevice != null) {
                            if (!device.getInetAddress()
                                    .equals(discoveredNetworkDevice.getInetAddress())) {
                                onDeviceRemoved(discoveredNetworkDevice);
                            } else {
                                discoveredNetworkDevice.addDiscoveryInstance(device);
                                device = discoveredNetworkDevice;
                            }
                        } else {
                            synchronized (mDiscoveredPrinters) {
                                mDiscoveredPrinters.put(key, device);
                            }
                        }

                        onDeviceFound(device);
                    }
                } catch (IOException e) {
                    // Socket got closed
                    break;
                }
            }
        }
    }

    /**
     * Thread that sends out the packages that make the devices announce them self. The announcement
     * are the received by the {@link ListenerThread listener thread}.
     * <p/>
     * For the fist {@value #FAST_QUERY_PERIOD_MS} ms the thread sends a query packet every {@value
     * #FAST_DELAY_MS} ms. After that every {@value #SLOW_DELAY_MS} ms. The make the thread send the
     * packets quickly again, {@link #reactivate()} can be called.
     */
    private class QueryThread extends Thread {
        private static final int FAST_DELAY_MS = 5_000;
        private static final int FAST_QUERY_PERIOD_MS = 5 * 60_000;
        private static final int SLOW_DELAY_MS = 20_000;

        /**
         * Last time {@link #reactivate} thread was reactivated or if never reactivated, the time
         * the thread was created.
         */
        private long mLastActivity;

        public QueryThread() {
            mLastActivity = System.currentTimeMillis();
        }

        /**
         * Reactivate thread, i.e. make the thread send out the query packets quickly again
         */
        public void reactivate() {
            synchronized (this) {
                mLastActivity = System.currentTimeMillis();
                notifyAll();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ArrayList<DatagramPacket> datagramList = new ArrayList<>();
                    Collections.addAll(datagramList, mDiscoveryMethod.createQueryPackets());

                    final int numPackets = datagramList.size();
                    for (int i = 0; i < numPackets; i++) {
                        mSocket.send(datagramList.get(i));
                    }

                    synchronized (this) {
                        if (System.currentTimeMillis() < mLastActivity + FAST_QUERY_PERIOD_MS) {
                            wait(FAST_DELAY_MS);
                        } else {
                            wait(SLOW_DELAY_MS);
                        }
                    }
                } catch (Exception e) {
                    // Socket got closed
                    break;
                }
            }
        }
    }
}

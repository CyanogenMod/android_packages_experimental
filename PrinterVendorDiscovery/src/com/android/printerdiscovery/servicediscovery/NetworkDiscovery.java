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

package com.android.printerdiscovery.servicediscovery;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import com.android.internal.util.Preconditions;
import com.android.printerdiscovery.R;
import com.android.printerdiscovery.servicediscovery.mdns.MDnsDiscovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Discovers devices on the network and notifies the listeners about those devices.
 */
public class NetworkDiscovery {
    /**
     * Currently active clients of the discovery
     */
    private static final @NonNull ArrayList<IDiscoveryListener> sListeners = new ArrayList<>();

    /**
     * If the network discovery is running, this stores the instance. There can always be at most
     * once instance running.
     */
    private static @Nullable NetworkDiscovery sInstance = null;

    /**
     * Method used for discovering printers
     */
    private final @NonNull MDnsDiscovery mDiscoveryMethod;

    /**
     * List of discovered printers sorted by device identifier
     */
    private final @NonNull LinkedHashMap<String, NetworkDevice> mDiscoveredPrinters;

    /**
     * Socket used to discover devices (both query and listen).
     */
    private @NonNull MulticastSocket mSocket;

    /**
     * Thread sending the broadcasts that should make the device announce them self
     */
    private @NonNull QueryThread mQueryThread;

    /**
     * Thread that receives the announcements of new devices and processes them
     */
    private @NonNull ListenerThread mListenerThread;

    /**
     * Register a new listener for network devices. If this is the first listener, this starts a new
     * {@link NetworkDiscovery discovery session}.
     *
     * @param listener Listener to register.
     * @param context  Context the listener is running in.
     *
     * @throws IOException If the discovery session could not be started
     */
    public static void onListenerAdded(@NonNull IDiscoveryListener listener,
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
    public static void removeDiscoveryListener(@NonNull IDiscoveryListener listener)
            throws InterruptedException {
        listener = Preconditions.checkNotNull(listener, "listener");

        synchronized (sListeners) {
            sListeners.remove(listener);

            if (sListeners.isEmpty()) {
                sInstance.close();
                sInstance = null;
            }
        }
    }

    /**
     * Create and start a new network discovery session.
     *
     * @param context The context requesting the start of the session.
     *
     * @throws IOException If the discovery session could not be started
     */
    private NetworkDiscovery(@NonNull Context context) throws IOException {
        mDiscoveredPrinters = new LinkedHashMap<>();

        mDiscoveryMethod = new MDnsDiscovery(
                context.getResources().getStringArray(R.array.mdns_services));

        mSocket = NetworkUtils.createMulticastSocket(context, null);
        mSocket.setBroadcast(true);
        mSocket.setReuseAddress(true);
        mSocket.setSoTimeout(0);

        mListenerThread = new ListenerThread();
        mQueryThread = new QueryThread();

        mListenerThread.start();
        mQueryThread.start();
    }

    /**
     * If a new listener was added while the session was already running, announce all already found
     * devices to the new listener.
     *
     * @param listener The listener that was just added.
     */
    private void onListenerAdded(@NonNull IDiscoveryListener listener) {
        synchronized (mDiscoveredPrinters) {
            for (NetworkDevice device : mDiscoveredPrinters.values()) {
                listener.onDeviceFound(device);
            }
        }
    }

    /**
     * Clean up discovery session.
     *
     * @throws InterruptedException If the current thread was interrupted while the session was
     *                              cleaned up.
     */
    private void close() throws InterruptedException {
        // Closing the socket causes IOExceptions on operations on this socket. This in turn will
        // end the threads.
        mSocket.close();

        mListenerThread.join();
        mQueryThread.join();
    }

    /**
     * Notify all currently registered listeners that a new device was removed.
     *
     * @param networkDevice The device that was removed
     */
    private void fireDeviceRemoved(@NonNull NetworkDevice networkDevice) {
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
    private void fireDeviceFound(@NonNull NetworkDevice networkDevice) {
        synchronized (sListeners) {
            final int numListeners = sListeners.size();
            for (int i = 0; i < numListeners; i++) {
                sListeners.get(i).onDeviceFound(networkDevice);
            }
        }
    }

    /**
     * Thread receiving and processing the packets that announce network devices
     */
    private class ListenerThread extends Thread {
        private static final int BUFFER_LENGTH = 4 * 1024;

        @Override
        public void run() {
            while (true) {
                byte buffer[] = new byte[BUFFER_LENGTH];
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
                            if(!device.getInetAddress()
                                        .equals(discoveredNetworkDevice.getInetAddress())) {
                                fireDeviceRemoved(discoveredNetworkDevice);
                            } else {
                                discoveredNetworkDevice.addDiscoveryInstance(device);
                                device = discoveredNetworkDevice;
                            }
                        } else {
                            synchronized (mDiscoveredPrinters) {
                                mDiscoveredPrinters.put(key, device);
                            }
                        }

                        fireDeviceFound(device);
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
     */
    private class QueryThread extends Thread {
        private static final int MAX_ACTIVE_QUERIES = 10;
        private static final int MAX_DELAY_SECONDS = 60;

        private boolean mUseFallback = false;
        private boolean mIsActiveDiscovery = true;
        private int mQueriesSent = 0;

        /**
         * @return the next wait interval, in milliseconds, using an exponential backoff algorithm.
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
                }
            }

            return delayInSeconds * 1000;
        }

        @Override
        public void run() {
            mQueriesSent = (mIsActiveDiscovery ? 0 : MAX_ACTIVE_QUERIES);
            mUseFallback = false;

            while (true) {
                try {
                    ArrayList<DatagramPacket> datagramList = new ArrayList<>();

                    if (!mDiscoveryMethod.isFallback() || mUseFallback) {
                        Collections.addAll(datagramList, mDiscoveryMethod.createQueryPackets());
                    }

                    for (DatagramPacket packet : datagramList) {
                        mSocket.send(packet);
                    }

                    mQueriesSent++;

                    Thread.sleep(getQueryDelayInMillis());

                    if (!mUseFallback) {
                        mUseFallback = mDiscoveredPrinters.isEmpty();
                    }
                } catch (Exception e) {
                    // Socket got closed
                    break;
                }
            }
        }
    }
}

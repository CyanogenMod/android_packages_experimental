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

import android.text.TextUtils;
import android.util.Pair;

import com.android.printerdiscovery.servicediscovery.IDiscovery;
import com.android.printerdiscovery.servicediscovery.ServiceParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MDnsDiscovery implements IDiscovery {

    private static final AtomicInteger mTrasactionID = new AtomicInteger(0);
    static final String MDNS_GROUP_ADDRESS = "224.0.0.251";
    static final int MDNS_PORT = 5353;

    private int mQueryCount = 0;
    private static final int ANSWER_RESET_THRESHOLD = 3; // 4 requests, 3 with answers

    private static final byte[] REQ_TRANSACTION_ID = { 0x00, 0x00 };
    private static final byte[] REQ_FLAGS = { 0x00, 0x00 };
    private static final byte[] REQ_NO_ANSWERS = { 0x00, 0x00 };
    private static final byte[] REQ_NUM_AUTHORITY_RRS = { 0x00, 0x00 };
    private static final byte[] REQ_NUM_ADDITIONAL_RRS = { 0x00, 0x00 };
    private static final byte[] REQ_QUESTION__LOCAL = { 0x05, 0x6C, 0x6F, 0x63, 0x61, 0x6C, 0x00 };
    private static final byte[] REQ_PTR_TYPE = { 0x00, 0x0C };
    private static final byte[] REQ_IN_CLASS_QU_TRUE = { (byte)0x80, 0x01 };
    private static final byte[] REQ_IN_CLASS_QU_FALSE = { (byte)0x00, 0x01 };
    private final HashMap<String, Pair<String,String>> mPrinters = new LinkedHashMap<>();

    private final String[] mServiceList;

    public MDnsDiscovery(String[] serviceList) {
        mServiceList = serviceList;
    }

    @Override
    public void clear() {
        synchronized (mPrinters) {
            mPrinters.clear();
            mQueryCount = 0;
        }
    }

    @Override
    public DatagramPacket[] createQueryPackets() throws UnknownHostException {


        InetAddress group = InetAddress.getByName(MDNS_GROUP_ADDRESS);

        List<DatagramPacket> packetList = new ArrayList<>();

        HashMap<String,List<Pair<String,String>>> previouslyFoundPrinters = new HashMap<>(mServiceList.length);
        for(String serviceName : mServiceList) {
            previouslyFoundPrinters.put(serviceName, new ArrayList<Pair<String,String>>());
        }

        synchronized (mPrinters) {
            mQueryCount++;
            if(mQueryCount > ANSWER_RESET_THRESHOLD){
                mQueryCount = 0;
                mPrinters.clear();
            }
            for(Pair<String,String> val : mPrinters.values()) {
                List<Pair<String,String>> list = previouslyFoundPrinters.get(val.first);
                list.add(val);
            }
        }


        for(String serviceName : mServiceList) {
            List<Pair<String,String>> list = previouslyFoundPrinters.get(serviceName);

        ByteArrayOutputStream mdnsBuffer = new ByteArrayOutputStream();
        try {
                mdnsBuffer.write(shortToBytes((short)mTrasactionID.getAndIncrement()));
            mdnsBuffer.write(REQ_FLAGS);

                mdnsBuffer.write(shortToBytes((short)1));
                mdnsBuffer.write(shortToBytes((short)list.size()));
            mdnsBuffer.write(REQ_NUM_AUTHORITY_RRS);
            mdnsBuffer.write(REQ_NUM_ADDITIONAL_RRS);
                String[] serviceSplits = serviceName.split("\\.");
                for(String servicePart : serviceSplits) {
                    // coverity[FB.DM_DEFAULT_ENCODING]
                    byte[] serviceBytes = servicePart.getBytes();
                    mdnsBuffer.write(serviceBytes.length);
                    mdnsBuffer.write(serviceBytes);
                }
                mdnsBuffer.write(REQ_QUESTION__LOCAL);
                mdnsBuffer.write(REQ_PTR_TYPE);
                mdnsBuffer.write(REQ_IN_CLASS_QU_TRUE);
                for (Pair<String,String> printerAnswer : list) {
                    addAnswer(new String[] { serviceName }, mdnsBuffer, printerAnswer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] mdnsBytes = mdnsBuffer.toByteArray();
            packetList.add(new DatagramPacket( mdnsBytes, mdnsBytes.length, group, MDNS_PORT));
        }
        return packetList.toArray(new DatagramPacket[packetList.size()]);


    }

    private void addAnswer(String[] mdnsServices, ByteArrayOutputStream mdnsBuffer, Pair<String,String> printerAnswer) throws IOException {
        short questionIndex = (short)(REQ_TRANSACTION_ID.length +
                REQ_FLAGS.length +
                REQ_NO_ANSWERS.length +
                REQ_NO_ANSWERS.length +
                REQ_NUM_AUTHORITY_RRS.length +
                REQ_NUM_ADDITIONAL_RRS.length);
        for(String serviceName : mdnsServices) {
            if (TextUtils.equals(serviceName, printerAnswer.first)) break;
            // coverity[FB.DM_DEFAULT_ENCODING]
            String[] serviceSplits = serviceName.split("\\.");
            for(String servicePart : serviceSplits) {
                questionIndex += servicePart.getBytes().length + 1;
            }
            questionIndex += REQ_QUESTION__LOCAL.length + REQ_PTR_TYPE.length + REQ_IN_CLASS_QU_FALSE.length;
        }

        byte[] serviceNamePtr = shortToBytes((short)(0xC000 | questionIndex));
        mdnsBuffer.write(serviceNamePtr); // Pointer to service name
        mdnsBuffer.write(REQ_PTR_TYPE); // Type (PTR)
        mdnsBuffer.write(REQ_IN_CLASS_QU_FALSE); // Class (IN)
        mdnsBuffer.write(intToBytes(3600)); // TTL (1 hour)

        String bonjourName = printerAnswer.second;
        byte[] nameLength = shortToBytes((short) (bonjourName.length() + serviceNamePtr.length + 1));
        mdnsBuffer.write(nameLength); // Data length, includes first byte (which denotes the length of the name) and service name pointer length

        mdnsBuffer.write((byte) bonjourName.length()); // Domain name, includes first byte, which denotes the length of the name, and service name pointer
        mdnsBuffer.write(bonjourName.getBytes());
        mdnsBuffer.write(serviceNamePtr);
    }

    private byte[] shortToBytes(short s) {
        return new byte[]{(byte)((s & 0xFF00)>>8),(byte)(s & 0x00FF)};
    }

    private byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(i).array();
    }

    @Override
    public ArrayList<ServiceParser> parseResponse(DatagramPacket packet) {
        ArrayList<ServiceParser> networkDevices = new ArrayList<>();

        synchronized (mPrinters) {
        try {
            DnsPacket dnsPacket = new DnsParser().parse(packet);
            DnsService[] services = new DnsSdParser().parse(dnsPacket);

            for (DnsService service : services) {
                BonjourParser bonjourParser = new BonjourParser(service);
                mPrinters.put(service.getHostname() + "." + service.getNameSuffix(),
                        Pair.create(bonjourParser.getBonjourServiceName(), bonjourParser.getBonjourName()));
                networkDevices.add(bonjourParser);
            }
        } catch (Exception ignored) {
        }
        }
        return networkDevices;
    }

    @Override
    public int getPort() {
        return MDnsDiscovery.MDNS_PORT;
    }

    @Override
    public boolean isFallback() {
        return false;
    }

}

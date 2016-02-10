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

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class DnsSdParser {
    private static final String TAG = DnsSdParser.class.getSimpleName();

    private static final char SEPARATOR = '=';

    private DnsPacket packet;

    public DnsService[] parse(DnsPacket aPacket) throws DnsException {
        this.packet = aPacket;
        return this.parseServices();
    }

    private DnsService[] parseServices() throws DnsException {
        ArrayList<DnsService> serviceList = new ArrayList<>();

        for (DnsPacket.Entry answerEntry : this.packet.getAnswers()) {
            if (DnsPacket.ResourceType.PTR == answerEntry.getType()) {
                DnsPacket.Ptr ptr = (DnsPacket.Ptr) answerEntry;

                try {
                    serviceList.add(this.buildService(ptr));
                } catch (DnsSdException ignore) {
                }
            }
        }
        return serviceList.toArray(new DnsService[serviceList.size()]);
    }

    private DnsService buildService(DnsPacket.Ptr ptr) throws DnsSdException {
        DnsPacket.Name serviceNameSuffix = ptr.getName();
        DnsPacket.Name serviceName = ptr.getPointedName();
        DnsPacket.Srv srvEntry = this.findSrv(serviceName);
        DnsPacket.Txt txtEntry = this.findTxt(serviceName);
        DnsPacket.Name hostname = srvEntry.getTarget();
        DnsPacket.Address[] addressEntries = this.findAddresses(hostname);
        int port = srvEntry.getPort();
        Map<String, byte[]> attributes = parseAttributes(txtEntry.getText());
        byte[][] addresses = new byte[addressEntries.length][];

        for (int i = 0; i < addressEntries.length; i++) {
            addresses[i] = addressEntries[i].getAddress();
        }
        return new DnsService(serviceName, serviceNameSuffix, hostname, addresses, port, attributes);
    }

    private DnsPacket.Srv findSrv(DnsPacket.Name serviceName) throws DnsSdException {
        for (DnsPacket.Entry additionalEntry : this.packet.getAdditionals()) {
            if (DnsPacket.ResourceType.SRV == additionalEntry.getType()) {
                DnsPacket.Srv srv = (DnsPacket.Srv) additionalEntry;

                if (srv.getName().equals(serviceName)) {
                    return srv;
                }
            }
        }
        throw new DnsSdException("Service does not contain correspondent srv entry.");
    }

    private DnsPacket.Txt findTxt(DnsPacket.Name serviceName) throws DnsSdException {
        for (DnsPacket.Entry additionalEntry : this.packet.getAdditionals()) {
            if (DnsPacket.ResourceType.TXT == additionalEntry.getType()) {
                DnsPacket.Txt txt = (DnsPacket.Txt) additionalEntry;

                if (txt.getName().equals(serviceName)) {
                    return txt;
                }
            }
        }
        throw new DnsSdException("Service does not contain correspondent txt entry.");
    }

    private DnsPacket.Address[] findAddresses(DnsPacket.Name hostname) throws DnsSdException {
        ArrayList<DnsPacket.Address> addressEntries = new ArrayList<>();

        for (DnsPacket.Entry additionalEntry : this.packet.getAdditionals()) {
            if ((DnsPacket.ResourceType.A == additionalEntry.getType())
                    || (DnsPacket.ResourceType.AAAA == additionalEntry.getType())) {
                DnsPacket.Address address = (DnsPacket.Address) additionalEntry;

                if (address.getName().equals(hostname)) {
                    addressEntries.add(address);
                }
            }
        }
        if (addressEntries.isEmpty()) {
            throw new DnsSdException("Service does not contain correspondent address entry.");
        }
        return addressEntries.toArray(new DnsPacket.Address[addressEntries.size()]);
    }

    private static Map<String, byte[]> parseAttributes(byte[] txtData) {
        Map<String, byte[]> attributes = new HashMap<>();
        int offset = 0;

        while (offset < txtData.length) {
            int attrLength = txtData[offset++];
            if (attrLength < 0) {
                attrLength += 256;
            }
            int sepIndex;
            int keyLength;
            String key;
            byte[] value = null;

            if ((attrLength < 0) || ((offset + attrLength) > txtData.length)) {
                return attributes;
            }
            sepIndex = findSeparator(txtData, offset, attrLength);
            keyLength = (sepIndex > 0) ? (sepIndex - offset) : attrLength;
            if (keyLength == 0) {
                return attributes;
            }
            try {
                key = new String(txtData, offset, keyLength, "US-ASCII");
            } catch (Exception e) {
                Log.e(TAG, "Exception: parsing attribute: " + e);
                e.printStackTrace();
                continue;
            }

            if (sepIndex > 0) {
                int valueLength = (attrLength - keyLength - 1);

                value = new byte[valueLength];
                System.arraycopy(txtData, sepIndex + 1, value, 0, valueLength);
            }
            attributes.put(key, value);
            offset += attrLength;
        }
        return attributes;
    }

    private static int findSeparator(byte[] txtData, int offset, int length) {
        for (int i = offset; i < (offset + length); i++) {
            if (txtData[i] == (byte) SEPARATOR) {
                return i;
            }
        }
        return -1;
    }
}

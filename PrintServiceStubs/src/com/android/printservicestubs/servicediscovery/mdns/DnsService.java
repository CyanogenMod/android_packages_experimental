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

package com.android.printservicestubs.servicediscovery.mdns;

import java.util.Map;

class DnsService {
    private DnsPacket.Name name;
    private DnsPacket.Name serviceNameSuffix;
    private DnsPacket.Name hostname;
    private byte[][] addresses;
    private int port;
    private Map<String, byte[]> attributes;

    public DnsService(DnsPacket.Name name, DnsPacket.Name serviceNameSuffix, DnsPacket.Name hostname, byte[][] addresses, int port,
                      Map<String, byte[]> attributes) {
        this.name = name;
        this.serviceNameSuffix = serviceNameSuffix;
        this.hostname = hostname;
        this.addresses = addresses;
        this.port = port;
        this.attributes = attributes;
    }

    public DnsPacket.Name getName() {
        return this.name;
    }

    public DnsPacket.Name getNameSuffix() {
        return this.serviceNameSuffix;
    }

    public DnsPacket.Name getHostname() {
        return this.hostname;
    }

    public byte[][] getAddresses() {
        return addresses;
    }

    public int getPort() {
        return port;
    }

    public Map<String, byte[]> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return this.getName().toString();
    }
}

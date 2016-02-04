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

import android.os.Bundle;
import android.text.TextUtils;

import com.android.printerdiscovery.servicediscovery.NetworkDevice;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

public class BonjourParser implements BonjourServiceParser {

    public static final String VALUE_ENCODING = "UTF-8";
    private static final int IPV4_LENGTH = 4;

    private DnsService service;

    public BonjourParser(DnsService service) {
        this.service = service;
    }

    @Override
    public NetworkDevice.DiscoveryMethod getDiscoveryMethod() {
        return NetworkDevice.DiscoveryMethod.MDNS_DISCOVERY;
    }

    @Override
    public int getPort() {
        return service.getPort();
    }

    @Override
    public String getDeviceIdentifier() {
        return getHostname();
    }

    @Override
    public String getBonjourName() {
        return this.service.getName().getLabels()[0];
    }

    @Override
    public String getHostname() {
        return this.service.getHostname().getLabels()[0];
    }

    @Override
    public InetAddress getAddress() {
        byte[] address = this.getFirstIpv4Address();

        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException exc) {
            return null;
        }
    }

    private byte[] getFirstIpv4Address() {
        byte[][] addresses = this.service.getAddresses();

        for (byte[] address : addresses) {
            if (IPV4_LENGTH == address.length) {
                return address;
            }
        }
        return addresses[0];
    }

    @Override
    public String getModel() {
        String model = null;
        try {
            model = this.getAttribute(MDnsUtils.ATTRIBUTE__TY);
        } catch (BonjourException ignored) {
        }
        return model;
    }

    @Override
    public String getServiceName() {
        return this.service.getName().toString();
    }

    @Override
    public String getBonjourServiceName() {
        String name = this.service.getName().toString();
        return name.substring(name.indexOf('.') + 1, name.lastIndexOf('.'));
    }

    @Override
    public String getAttribute(String key) throws BonjourException {
        byte[] value = this.service.getAttributes().get(key);

        if (value == null) {
            return null;
        }
        try {
            return new String(value, VALUE_ENCODING);
        } catch (UnsupportedEncodingException exc) {
            throw new BonjourException("Unsupported encoding to read attribute value: "
                    + VALUE_ENCODING, exc);
        }
    }

    @Override
    public Bundle getAllAttributes() {
        Bundle attribtues = new Bundle();
        Set<Map.Entry<String, byte[]>> attributeSet = this.service.getAttributes().entrySet();
        for (Map.Entry<String, byte[]> entry : attributeSet) {
            if (entry == null) {
                continue;
            }
            String key = entry.getKey();
            byte[] bytes = entry.getValue();
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            String value;
            try {
                value = new String(bytes, VALUE_ENCODING);
                attribtues.putString(key, value);
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        return attribtues;
    }
}

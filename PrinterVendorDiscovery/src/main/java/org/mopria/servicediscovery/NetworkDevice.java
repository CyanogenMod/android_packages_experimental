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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.mopria.servicediscovery.mdns.BonjourParser;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * There is no public constructor. Instances are either returned by the printer
 * discovery provided by the print system, read from parcels or loaded from
 * shared preferences.
 */
@SuppressWarnings("unused")
public final class NetworkDevice implements Parcelable {

    public enum DiscoveryMethod {
        MDNS_DISCOVERY,
        SNMP_DISCOVERY,
        OTHER_DISCOVERY,
    }

    private static final String TAG = "NetworkDiscovery";

    /*
     * Keep these declarations ordered alphabetically by field hostname. This helps
     * to keep readFromParcel and writeToParcel up-to-date.
     *
     * The fields have package-level visibility because they must be
     * accessible by the "friend" class EPrintersDatabase.
     */
    private final InetAddress inetAddress;
    private final String model;
    private final String hostname;

    private final String bonjourName;
    private final String bonjourDomainName;
    private final DiscoveryMethod discoveryMethod;
    private final String mBonjourService;
    private final int mPort;
    private final String mDeviceIdentifier;

    private Bundle bonjourData = new Bundle();

    private final List<NetworkDevice> mOtherInstances = new ArrayList<>();

    public NetworkDevice(ServiceParser parser) throws IllegalArgumentException {
        InetAddress inetAddress = parser.getAddress();
        if (inetAddress == null) {
            throw new IllegalArgumentException("inetAddress can not be null");
        }
        this.inetAddress = parser.getAddress();
        this.model = parser.getModel();
        this.hostname = parser.getHostname();
        this.mPort = parser.getPort();
        this.mDeviceIdentifier = parser.getDeviceIdentifier();

        if (parser instanceof BonjourParser) {
            BonjourParser bonjourParser = (BonjourParser)parser;
            this.bonjourName = bonjourParser.getBonjourName();
            this.bonjourDomainName = bonjourParser.getHostname();
            this.mBonjourService = bonjourParser.getBonjourServiceName();
        } else {
            this.bonjourName = null;
            this.bonjourDomainName = null;
            this.mBonjourService = parser.getServiceName();
        }
        bonjourData.putAll(parser.getAllAttributes());

        discoveryMethod = parser.getDiscoveryMethod();
    }

    private NetworkDevice(Parcel in) throws UnknownHostException {
        int inetAddrSize = in.readInt();

        if (inetAddrSize > 0) {
            byte[] addr = new byte[inetAddrSize];
            in.readByteArray(addr);
            this.inetAddress = InetAddress.getByAddress(addr);
        } else {
            this.inetAddress = null;
        }
        this.model = in.readString();
        this.hostname = in.readString();
        this.mPort = in.readInt();
        this.mDeviceIdentifier = in.readString();
        this.bonjourName = in.readString();
        this.bonjourDomainName = in.readString();
        this.mBonjourService = in.readString();
        this.discoveryMethod = DiscoveryMethod.values()[in.readInt()];
        this.bonjourData = in.readBundle(Bundle.class.getClassLoader());
        in.readTypedList(this.mOtherInstances, NetworkDevice.CREATOR);
    }

    public String deviceInfo() {
        return "ip: " + this.inetAddress + " model: " + this.model + " hostname: " + this.hostname
                + " bonjourName: " + this.bonjourName + " bonjourDomainName: " + this.bonjourDomainName;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        do {
            // coverity[UNREACHABLE]
            if (!(obj instanceof NetworkDevice)) continue;
            NetworkDevice other = (NetworkDevice) obj;
            // coverity[UNREACHABLE]
            if (!this.getInetAddress().equals(other.getInetAddress())) continue;
            // coverity[UNREACHABLE]
            if (!TextUtils.equals(this.bonjourDomainName, other.bonjourDomainName)) continue;
            // coverity[UNREACHABLE]
            if (!TextUtils.equals(this.mBonjourService, other.mBonjourService)) continue;
            // coverity[UNREACHABLE]
            if (!TextUtils.equals(this.mDeviceIdentifier, other.mDeviceIdentifier)) continue;
            return true;
        } while (false);
        return false;
    }

    public String getDeviceIdentifier() {
        return mDeviceIdentifier;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        hashCode = hashCode * prime + this.getInetAddress().hashCode();
        hashCode = hashCode * prime + ((bonjourDomainName != null) ? bonjourDomainName.hashCode() : 0);
        hashCode = hashCode * prime + ((mBonjourService != null) ? mBonjourService.hashCode() : 0);
        hashCode = hashCode * prime + ((mDeviceIdentifier != null) ? mDeviceIdentifier.hashCode() : 0);
        return hashCode;
    }

    public List<NetworkDevice> getAllDiscoveryInstances() {
        List<NetworkDevice> instances = new ArrayList<>(mOtherInstances.size() + 1);
        instances.add(this);
        instances.addAll(mOtherInstances);
        return instances;
    }

    public void addDiscoveryInstance(NetworkDevice device) {
        if ((device != null)
                && !equals(device) && !mOtherInstances.contains(device)) {
            mOtherInstances.add(device);
        }
    }

    public String getBonjourService() {
        return this.mBonjourService;
    }

    public DiscoveryMethod getDiscoveryMethod() {
        return discoveryMethod;
    }

    /**
     * @return the InetAddress
     */
    public InetAddress getInetAddress() {
        return this.inetAddress;
    }

    /**
     * @return the printer model hostname, e.g. "HP Officejet 6500 E709n"
     */
    public String getModel() {
        return this.model;
    }

    /**
     * @return the hostname of the printer in the network
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * @return the bonjour hostname of the printer in the network
     */
    public String getBonjourName() {
        return this.bonjourName;
    }

    public int getPort() {
        return mPort;
    }

    /**
     * @return the bonjour domain hostname of the printer in the network
     */
    public String getBonjourDomainName() {
        return this.bonjourDomainName;
    }

    public Bundle getTxtAttributes() {
        return new Bundle(bonjourData);
    }

    public Bundle getTxtAttributes(String serviceName) {
        List<NetworkDevice> instances = getAllDiscoveryInstances();
        for (NetworkDevice instance : instances) {
            if (TextUtils.equals(serviceName, instance.getBonjourService())) return instance.getTxtAttributes();
    }
        return new Bundle();
    }


    /**
     * Report the nature of this NetworkDevice's contents
     *
     * @return a bitmask indicating the set of special object types marshalled
     * by the NetworkDevice.
     * @see Parcelable#describeContents()
     */

    public int describeContents() {
        return 0;
    }

    /**
     * Writes the NetworkDevice contents to a Parcel, typically in order for it to be
     * passed through an IBinder connection.
     *
     * @param parcel The parcel to copy this NetworkDevice to.
     * @param flags  Additional flags about how the object should be written. May
     *               be 0 or {@link Parcelable#PARCELABLE_WRITE_RETURN_VALUE
     *               PARCELABLE_WRITE_RETURN_VALUE}.
     * @see Parcelable#writeToParcel(Parcel, int)
     */
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        /*
		 * In spite of what Coverity thinks, InetAddress.getAddress() can NOT
		 * return null. Never.
		 */
        if (this.inetAddress != null) {
            byte[] address = this.inetAddress.getAddress();
            parcel.writeInt(address.length);
            parcel.writeByteArray(address);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeString(this.model);
        parcel.writeString(this.hostname);
        parcel.writeInt(this.mPort);
        parcel.writeString(this.mDeviceIdentifier);
        parcel.writeString(this.bonjourName);
        parcel.writeString(this.bonjourDomainName);
        parcel.writeString(this.mBonjourService);
        parcel.writeInt(this.discoveryMethod.ordinal());
        parcel.writeBundle(this.bonjourData);
        parcel.writeTypedList(this.mOtherInstances);
    }

    /**
     * Reads Printers from Parcels.
     */
    public static final Creator<NetworkDevice> CREATOR = new Creator<NetworkDevice>() {
        @Override
        public NetworkDevice createFromParcel(Parcel in) {
            try {
                return new NetworkDevice(in);
            } catch (UnknownHostException ignored) {
            }
            return null;
        }

        @Override
        public NetworkDevice[] newArray(int size) {
            return new NetworkDevice[size];
        }
    };
}

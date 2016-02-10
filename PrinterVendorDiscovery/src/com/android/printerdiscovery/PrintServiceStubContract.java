/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.printerdiscovery;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract for accessing the com.android.printerdiscovery provider.
 */
public final class PrintServiceStubContract {
    /**
     * Authority string for this provider.
     */
    public static final String AUTHORITY = "com.android.printerdiscovery";

    /**
     * The content:// style URL for this provider
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Contains the discovery services.
     */
    public static final class PrintServiceStubs implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/discovery_services");

        public static final String _ID = BaseColumns._ID;

        /**
         * The name of the service the stub is for. <p>TYPE: TEXT</p>
         */
        public static final String NAME = "name";

        /**
         * 1 if the service is for more than one vendor, 0 otherwise. <p>TYPE: INTEGER</p>
         */
        public static final String IS_MULTIVENDOR_SERVICE = "isMultiVendorService";

        /**
         * The number of discovered printers. <p>TYPE: INTEGER</p>
         */
        public static final String NUM_DISCOVERED_PRINTERS = "numDiscoveredPrinter";

        /**
         * The URI to view once the stub is selected in the UI. <p>TYPE: TEXT</p>
         */
        public static final String INSTALL_URI = "installURL";

        public static final String[] ALL_COLUMNS = {
                NAME,
                IS_MULTIVENDOR_SERVICE,
                NUM_DISCOVERED_PRINTERS,
                INSTALL_URI,
                _ID
        };

        public static final String DEFAULT_SORT_ORDER =
                NUM_DISCOVERED_PRINTERS + " DESC " + IS_MULTIVENDOR_SERVICE + " ASC";
    }
}

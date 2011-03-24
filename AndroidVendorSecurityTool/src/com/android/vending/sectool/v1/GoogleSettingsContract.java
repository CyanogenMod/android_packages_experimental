/*
 * Copyright (C) 2009 Google Inc.
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

package com.android.vending.sectool.v1;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * The GoogleSettings provider contains Google app/service specific preferences.
 *
 * This class is duplicated in vendor/google/frameworks/maps, see
 * http://b/2553658.
 */
public final class GoogleSettingsContract {

    public static final String AUTHORITY = "com.google.settings";

    private static final String TAG = "GoogleSettings";

    /**
     * Common base for tables of name/value settings.
     */
    public static class NameValueTable implements BaseColumns {
        public static final String NAME = "name";
        public static final String VALUE = "value";

        protected static boolean putString(ContentResolver resolver, Uri uri,
                String name, String value) {
            // The database will take care of replacing duplicates.
            try {
                ContentValues values = new ContentValues();
                values.put(NAME, name);
                values.put(VALUE, value);
                resolver.insert(uri, values);
                return true;
            } catch (SQLException e) {
                Log.e(TAG, "Can't set key " + name + " in " + uri, e);
                return false;
            } catch (IllegalArgumentException e) {
                // ContentResolver.insert() throws IllegalArgumentException if there is no
                // provider for the URI.
                Log.e(TAG, "Can't set key " + name + " in " + uri, e);
                return false;
            }
        }

        public static Uri getUriFor(Uri uri, String name) {
            return Uri.withAppendedPath(uri, name);
        }
    }

    /**
     * "Partner" settings,  Actually this is the only settings table, and
     * it gets used for general Google-specific settings.  The fact that it's
     * called "Partner" is just a historical accident.
     */
    public static final class Partner extends NameValueTable {
        /**
         * Look up a name in the database.
         * @param resolver to access the database with
         * @param name to look up in the table
         * @return the corresponding value, or null if not present
         */
        public static String getString(ContentResolver resolver, String name) {
            String value = null;
            Cursor c = null;
            try {
                c = resolver.query(CONTENT_URI, new String[] { NameValueTable.VALUE },
                        NameValueTable.NAME + "=?", new String[]{ name }, null);
                if (c != null && c.moveToNext()) value = c.getString(0);
            } catch (SQLException e) {
                // SQL error: return null, but don't cache it.
                Log.e(TAG, "Can't get key " + name + " from " + CONTENT_URI, e);
            } finally {
                if (c != null) c.close();
            }
            return value;
        }

        /**
         * Look up a name in the database
         * @param resolver to access the database
         * @param name to look up in the table
         * @param defaultValue value to set if not found in table
         * @return the value found in the table or default
         */
        public static String getString(ContentResolver resolver, String name, String defaultValue) {
            String value = getString(resolver, name);
            if (value == null) {
                value = defaultValue;
            }

            return value;
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putString(ContentResolver resolver,
                String name, String value) {
            return putString(resolver, CONTENT_URI, name, value);
        }

        /**
         * Store a name/value pair into the database.
         * @param resolver to access the database with
         * @param name to store
         * @param value to associate with the name
         * @return true if the value was set, false on database errors
         */
        public static boolean putInt(ContentResolver resolver,
                String name, int value) {
            return putString(resolver, name, String.valueOf(value));
        }

        /**
         * Look up the value for name in the database, convert it to an int using Integer.parseInt
         * and return it. If it is null or if a NumberFormatException is caught during the
         * conversion then return defValue.
         */
        public static int getInt(ContentResolver resolver, String name, int defValue) {
            String valString = getString(resolver, name);
            int value;
            try {
                value = valString != null ? Integer.parseInt(valString) : defValue;
            } catch (NumberFormatException e) {
                value = defValue;
            }
            return value;
        }

        /**
         * Look up the value for name in the database, convert it to a long using Long.parseLong
         * and return it. If it is null or if a NumberFormatException is caught during the
         * conversion then return defValue.
         */
        public static long getLong(ContentResolver resolver, String name, long defValue) {
            String valString = getString(resolver, name);
            long value;
            try {
                value = valString != null ? Long.parseLong(valString) : defValue;
            } catch (NumberFormatException e) {
                value = defValue;
            }
            return value;
        }

        /**
         * Construct the content URI for a particular name/value pair,
         * useful for monitoring changes with a ContentObserver.
         * @param name to look up in the table
         * @return the corresponding content URI, or null if not present
         */
        public static Uri getUriFor(String name) {
            return getUriFor(CONTENT_URI, name);
        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/partner");


        /**
         * Partner Table Version
         */

        public static final String DATA_STORE_VERSION = "data_store_version";

        /**
         * Google Partner Client Id
         */
        public static final String CLIENT_ID = "client_id";

        /**
         * Voice Search Client Id
         */

        public static final String VOICESEARCH_CLIENT_ID = "voicesearch_client_id";

        /**
         * Google Mobile Maps Client Id
         */
        public static final String MAPS_CLIENT_ID = "maps_client_id";

        /**
         * Google YouTube App Client Id
         */
        public static final String YOUTUBE_CLIENT_ID = "youtube_client_id";

        /**
         * Android Market Client Id
         */
        public static final String MARKET_CLIENT_ID = "market_client_id";

        /**
         * True if user has opted in to network location service.
         */
        public static final String NETWORK_LOCATION_OPT_IN = "network_location_opt_in";

        /**
         * Flag for allowing Google services to use location information.
         * Type: int ( 0 = disallow, 1 = allow )
         */
        public static final String USE_LOCATION_FOR_SERVICES = "use_location_for_services";

        /**
         * RLZ is a tracking string used for ROI analysis. It is similar
         * to client id but more powerful. RLZ data enables ROI analysis in
         * Google's distribution business (Toolbar, Pack, iGoogle). It
         * explicitly ties the revenue received from distributed software to the
         * expense of distribution payments.
         */
        public static final String RLZ = "rlz";

        /**
         * The Logging ID (a unique 64-bit value) as a hex string.
         * Used as a pseudonymous identifier for logging.
         */
        public static final String LOGGING_ID2 = "logging_id2";

        /**
         * Opaque blob of data representing Market state (installed apps, etc).
         * Used to hand off this data to the checkin service for upload.
         */
        public static final String MARKET_CHECKIN = "market_checkin";
    }
}

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.google.android.googlelogin;

import android.content.Intent;
// import android.accounts.AccountManager;

/**
 * Miscellaneous constants used by the GoogleLoginService and its
 * clients.
 */
public class GoogleLoginServiceConstants {
    /** This class is never instantiated. */
    private GoogleLoginServiceConstants() {
    }

    /**
     * Key used in the "extras" bundle.
     * <p>
     * The value of the mapping with this key is a String[]
     * representing the Google accounts currently known on the device.
     */
    public static final String ACCOUNTS_KEY = "accounts"; // AccountManager.KEY_ACCOUNTS;

    /**
     * Key used in the "extras" bundle.
     * <p>
     * The value of the mapping with this key is a String containing
     * the requested authentication token.
     */
    public static final String AUTHTOKEN_KEY = "authtoken"; // AccountManager.KEY_AUTHTOKEN;

    /**
     * Key used in the "extras" bundle.
     * <p>
     * The value of the mapping with this key is a String containing
     * the account name (username) used to generate the accompanying
     * authentication token.
     */
    public static final String AUTH_ACCOUNT_KEY = "authAccount"; // AccountManager.KEY_ACCOUNT_NAME;

    /**
     * Key used in the "extras" bundle that will be present if an error
     * has occurred.
     * <p>
     * The value of the mapping with this key is an int. The possible values are
     * {@link #ERROR_CODE_GLS_NOT_FOUND} or
     * {@link #ERROR_CODE_GLS_VERIFICATION_FAILED}.
     */
    public static final String ERROR_CODE_KEY = "errorCode"; // AccountManager.KEY_ERROR_CODE;

    /**
     * Error code (see {@link #ERROR_CODE_KEY}) for when the Google login
     * service can not be found.
     */
    public static final int ERROR_CODE_GLS_NOT_FOUND = 0;

    /**
     * Error code (see {@link #ERROR_CODE_KEY}) for when the verification of the
     * Google login service fails.
     */
    public static final int ERROR_CODE_GLS_VERIFICATION_FAILED = 1;

    /**
     * Gets a message (can be used as an Exception message) for a particular
     * error code.
     *
     * @param errorCode The error code.
     * @return A message describing the error code. This will not be localized.
     */
    static String getErrorCodeMessage(int errorCode) {
        switch (errorCode) {
            case ERROR_CODE_GLS_NOT_FOUND:
                return "The Google login service cannot be found.";

            case ERROR_CODE_GLS_VERIFICATION_FAILED:
                return "The Google login service cannot be verified.";

            default:
                return "Unknown error";
        }
    }

    /**
     * Extras to be returned to the caller.
     */
    public static final String REQUEST_EXTRAS = "callerExtras";

    /**
     * YouTube logins produce an extra bit of data: the youtube
     * username linked to the google account that we log in to.
     * getAuthToken will return this extra string when logging
     * in to the 'youtube' service.
     */
    public static final String YOUTUBE_USER_KEY = "YouTubeUser";

    /**
     * The name of the Google login service.
     */
    public static final String SERVICE_NAME = "GoogleLoginService";

    /**
     * The package name of the Google login service.
     */
    public static final String SERVICE_PACKAGE_NAME = "com.google.android.googleapps";

    /**
     * The fully qualified name of the Google login service (package + name).
     */
    public static final String FULLY_QUALIFIED_SERVICE_NAME =
            SERVICE_PACKAGE_NAME + "." + SERVICE_NAME;

    /** The intent used to bind to the Google Login Service. */
    public static final Intent SERVICE_INTENT =
        (new Intent()).setClassName(SERVICE_PACKAGE_NAME, FULLY_QUALIFIED_SERVICE_NAME);

    public static final int FLAG_GOOGLE_ACCOUNT = 0x1;
    public static final int FLAG_HOSTED_ACCOUNT = 0x2;
    public static final int FLAG_YOUTUBE_ACCOUNT = 0x4;
    public static final int FLAG_SAML_ACCOUNT = 0x8;
    public static final int FLAG_LEGACY_GOOGLE = 0x10;
    public static final int FLAG_LEGACY_HOSTED_OR_GOOGLE = 0x20;

    public static final String FEATURE_LEGACY_GOOGLE = "legacy_google";
    public static final String FEATURE_LEGACY_HOSTED_OR_GOOGLE = "legacy_hosted_or_google";
    public static final String FEATURE_HOSTED_OR_GOOGLE = "hosted_or_google";
    public static final String FEATURE_GOOGLE = "google";
    public static final String FEATURE_YOUTUBE = "youtubelinked";
    public static final String FEATURE_SAML_ACCOUNT = "saml";

    /**
     * Prefix for service features, combine with the service name (as defined by
     * ClientLogin ) for example service_cp for an account having calendar.
     */
    public static final String FEATURE_SERVICE_PREFIX = "service_";

    public static final boolean REQUIRE_GOOGLE = true;
    public static final boolean PREFER_HOSTED = false;

    // the account type for google accounts that are authenticated via GAIA
    public static final String ACCOUNT_TYPE = "com.google";

    /**
     * Action sent as a broadcast Intent by the AccountsService
     * when it starts up and no accounts are available (so some should be added).
     */
    public static final String LOGIN_ACCOUNTS_MISSING_ACTION =
        "com.google.android.googlelogin.LOGIN_ACCOUNTS_MISSING";

    public static String featureForService(String service) {
        return FEATURE_SERVICE_PREFIX + service;
    }
}

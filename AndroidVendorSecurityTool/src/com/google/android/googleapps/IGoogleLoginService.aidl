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

package com.google.android.googleapps;

import com.google.android.googleapps.GoogleLoginCredentialsResult;
import com.google.android.googleapps.LoginData;


/**
 * Central application service used by Google apps to acquire login
 * credentials for all Google services.
 * <p>
 * See {@link GoogleLoginServiceHelper} and
 * {@link GoogleLoginServiceSynchronousHelper} for convenience wrappers
 * around the sequence of binding to the service and then
 * making API calls.
 */
interface IGoogleLoginService {
    /**
     * Gets the list of Google Accounts the user has previously logged
     * in to.  Accounts are of the form "username@domain".
     * <p>
     * This method will return an empty array if the device doesn't
     * know about any Google accounts (yet).  In this case, if the
     * caller is an application (rather than a background task), the
     * typical next step is to call {@link #blockingGetCredentials}
     * with a null account; this will return an Intent that,
     * if run using {@link android.app.Activity#startSubActivity startSubActivity},
     * will prompt the user for username/password and ultimately
     * return an authentication token for the desired service.
     *
     * @return The accounts.  The array will be zero-length if the
     *         GoogleLoginService doesn't know about any accounts yet.
     */
    String[] getAccounts();

    /**
     * @deprecated
     *
     * Now equivalent to <code>getAccount(false)</code>.
     */
    String getPrimaryAccount();

    /**
     * Get the account to use on this device.
     *
     * @param requireGoogle true if you need the device's Google
     *        (non-hosted domain) account.
     *
     * @return username of the account
     */
    String getAccount(boolean requireGoogle);

    /**
     * Retrieves the credentials for a given account and service
     * <i>only if</i> the device already has a cached authentication token for that
     * account/service.
     * <p>
     * This method will <i>not</i> do any network I/O, even in the case
     * where we have no auth token for the specified service but we do
     * have a password for the specified account.
     * (Use {@link #blockingGetCredentials} if you do want to allow
     * the GoogleLoginService to use the network to fetch a fresh
     * auth token in this case.)
     * <p>
     * Note that the GoogleLoginService caches auth tokens
     * indefinitely, but the tokens themselves may expire over time.
     * Thus, the auth token returned by this method is <i>not</i>
     * guaranteed to be valid (since we'd need to do network I/O to
     * check that!)
     * <p>
     * The account and service parameters are the same as in
     * the {@link #blockingGetCredentials} call.
     *
     * @return An authentication token for the desired username/service,
     *         or null if the device doesn't have a
     *         cached auth token for that username/service.
     */
    String peekCredentials(String username, String service);


    /**
     * Retrieves the credentials for a given account and service.
     * <p>
     * This method will <b>either</b>
     * <ol>
     * <li> return an authentication token for the specified account/service
     * <p> <b>or</b>
     * <li> return an Intent which you (the caller) need to run
     *      yourself using {@link android.app.Activity#startSubActivity startSubActivity},
     *      which will ultimately return the desired
     *      auth token to your <code>onActivityResult</code> method.
     *      <br>
     *      (Generally, that Intent will bring up a UI for the user to
     *      enter their username and password, but the
     *      IGoogleLoginService interface deliberately doesn't specify
     *      exactly what that Intent will do.)
     * </ol>
     * The {@link GoogleLoginCredentialsResult} class encapsulates these two
     * possible results: either the <code>getCredentialsString</code> method will
     * return an auth token (in case (1) above), or the
     * <code>getCredentialsIntent</code> method will return the Intent you need
     * to run to prompt the user (in case (2)).
     * <p>
     * This method will never return a GoogleLoginCredentialsResult
     * object where <i>both</i> <code>getCredentialsString()</code> and
     * <code>getCredentialsIntent()</code> return non-null.
     * <p>
     * Other notes:
     * <ul>
     * <li> In case (1) this method may block to do network I/O!
     *   Use {@link #peekCredentials} instead if you need to quickly check
     *   whether the device has a cached auth token for a given
     *   account/service.
     * <p>
     * <li> In case (2), if it's appropriate to interact with the user at
     *   the current time, the caller should run the returned Intent as
     *   a sub-activity.
     *   When that sub-activity returns, the resulting "extras" Bundle
     *   (which will be returned to the caller's <code>onActivityResult</code>
     *   method) will contain the following two mappings <i>if</i> we
     *   were able to successfully log in to the specified
     *   account/service:
     *   <ul>
     *   <li>key   = {@link GoogleLoginServiceConstants#AUTHTOKEN_KEY
     *                      GoogleLoginServiceConstants.AUTHTOKEN_KEY}
     *   <li>value = Auth token for the specified account/service (String)
     *   </ul>
     *   and
     *   <ul>
     *   <li>key   = {@link GoogleLoginServiceConstants#AUTH_ACCOUNT_KEY
     *                      GoogleLoginServiceConstants.AUTH_ACCOUNT_KEY}
     *   <li>value = Account name used to generate the above auth token (String),
     *       which will probably be useful if you initially
     *       called <code>blockingGetCredentials</code> with a null account name.
     *   </ul>
     *   <p>
     *   Note: if the caller's <code>onActivityResult</code> method is called with
     *   a null "extras" Bundle, or a resultCode of RESULT_CANCELED,
     *   that means that we weren't able to authenticate.
     *   Either the user bailed out without entering a
     *   username+password at all, or there was some failure during
     *   the authentication process.
     * <p>
     * <li>If your application simply wants to "log into an account
     *   without ever prompting the user", like in the (very common)
     *   case of a background task or sync adapter, you can simply
     *   handle case (2) by not doing anything at all (other than maybe
     *   displaying a "couldn't log in" status somewhere in your UI.)
     * <p>
     * <li>See {@link GoogleLoginServiceHelper#getCredentials
     *   GoogleLoginServiceHelper.getCredentials} for a static
     *   convenience wrapper around this entire sequence.
     * </ul>
     *
     * @param account The account to obtain the credentials for.
     *                This should either be one of the accounts returned by
     *                the <code>getAccounts</code> call, or null.
     *                <br>
     *                The account will typically be null in the
     *                case where there the device doesn't know about
     *                any accounts yet, or also in the case where
     *                an application needs to log into a new account;
     *                a null account here guarantees that we'll
     *                need to prompt the user for username/password.
     *                <br>
     *                UI note: In the case where we return an Intent
     *                to prompt the user for username/password, that
     *                UI will automatically assume a domain name
     *                (usually "@gmail.com", but which may vary
     *                depending on the specified service) if the user
     *                enters a username with no domain.
     *
     * @param service The service to log in to, like "cl" (Calendar)
     *                or "mail" (GMail) or "blogger" (Blogger).
     *                (See the API documentation for your specific
     *                service to find out what service name to use
     *                here.)
     *                <br>
     *                If null, the resulting authentication token will
     *                be "non-compartmentalized" (and will work across
     *                all non-compartmentalized Google services.)
     *
     * @param notifyAuthFailure if true, we will put up a status bar
     *                notification if the attempt get the auth token
     *                over the network fails because the password is
     *                invalid (or because the password is blank).
     *
     * @return The results: either the requested credentials,
     *         or an Intent that your app can run to get them.
     */
    GoogleLoginCredentialsResult blockingGetCredentials(
        String username, String service, boolean notifyAuthFailure);

    /**
     * Invalidates the specified authentication token.  The next time
     * this token is requested it will be reauthenticated.
     * <p>
     * Background: the auth tokens returned from <code>peekCredentials()</code> or
     * <code>blockingGetCredentials()</code> are <i>not</i> guaranteed to be
     * valid, mainly because a token cached in the device's database may
     * have expired since we last tried to use it.  (Most auth tokens expire
     * after some amount of time, but the GoogleLoginService itself can't tell
     * when or if a given auth token will expire.  Only the Google service
     * your application talks to can say for sure whether a given auth token
     * is still valid.)
     * <p>
     * So if you do get an authentication error (from your app's service) when
     * using an auth token that came from the GoogleLoginService, you should
     * tell the GoogleLoginService about it by passing the failed token to
     * this method.  After doing that, you can get a fresh token by making
     * another <code>blockingGetCredentials()</code> call (or
     * <code>getCredentials()</code> if using a helper class like
     * <code>GoogleLoginServiceHelper</code>).
     *
     * @param authTokenToInvalidate The auth token to invalidate.
     */
    void invalidateAuthToken(String authTokenToInvalidate);

    /**
     * Returns the "Android ID", a 64-bit value unique to this device
     * assigned when the device is first registered with Google.
     *
     * Returns 0 if the device is not registered, or if the Android ID
     * is unavailable for any reason.  This call never blocks for
     * network activity.
     */
    long getAndroidId();

    // --------------------------------------------------------
    // methods below this point require the
    // com.google.android.googleapps.permission.ACCESS_GOOGLE_PASSWORD
    // permission, and will fail if the caller does not have it.
    // --------------------------------------------------------

    /**
     * Try logging in to a new account.
     *
     * On calling, data should contain username, password, (optional)
     * captcha token and answer.  flags should be 0 to allow any
     * account, or FLAG_GOOGLE_ACCOUNT to require a google account.
     *
     * The possible values of data.mStatus on return are described by
     * the {@link LoginData.Status} enum.
     *
     * If status is CAPTCHA, the captcha token, data, and mime type
     * fields will be filled in.
     */
    void tryNewAccount(inout LoginData data);

    /**
     * Save a new account to the database.  data should be an object
     * previously passed to tryNewAccount, with status SUCCESS.
     */
    void saveNewAccount(in LoginData data);

    /**
     * Store an auth token into the database.  Has no effect if
     * account is not in the database.
     */
    void saveAuthToken(String username, String service, String authToken);

    /**
     * Try to obtain an authtoken for the username and service in
     * data, using the password in data.  If successful, save the
     * password in the database (and return the authtoken).
     *
     * If the password in data is null, erases the password and all
     * authtokens for the account from the database.
     *
     * username must already exist in the database, or a status of
     * BAD_REQUEST will be returned.
     */
    void updatePassword(inout LoginData data);

    /**
     * Returns true if the given username/password match one stored in
     * the login service.  This does <i>not</i> do any network
     * activity; it does not check that the password is valid on
     * Google's servers, only that it matches what is stored on the
     * device.
     */
    boolean verifyStoredPassword(String username, String password);

    // --------------------------------------------------------
    // methods below this point exist primarily for automated tests.
    // --------------------------------------------------------

    /**
     * Add a new username/password pair to the Google Login Service.
     * Does not test to see if they are valid; just puts them directly
     * in the database.
     *
     * @param account the username (email address) of the account to add
     * @param password the password for this new account
     */
    void saveUsernameAndPassword(String username, String password, int flags);

    /**
     * Remove an account and all its authtokens from the database.
     *
     * @param account the account to remove
     */
    void deleteOneAccount(String username);

    /**
     * Remove all accounts and authtokens from the database.
     */
    void deleteAllAccounts();

    /**
     * Cause the device to register and get an android ID.
     * @return If LoginData.Status were an int we could return that.  Instead
     * we return:
     *       0  - success
     *       1  - login failure (caller should ask for a new password)
     *       2  - NOT IMPLEMENTED - CAPTCHA required?
     *       -1 - network or other transient failure (caller should try again, with a backoff)
     */
    int waitForAndroidId();

    // TODO: The aidl compiler might let you define constants (static
    // member variables) one of these days, rather than just letting
    // you declare member functions.
    //
    // When/if that happens, I should consider adding a section to
    // this aidl file for "Miscellaneous constants used by the
    // GoogleLoginService and related helper classes", containing the
    // constants currently found in the GoogleLoginServiceConstants
    // class (which can then be deleted.)
}

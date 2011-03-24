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

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * "Result" container class returned by the GoogleLoginService
 * {@link IGoogleLoginService#blockingGetCredentials} or
 * {@link IGoogleLoginService#retryGetCredentials} request.
 * <p>
 * A single credentials request can <b>either</b>
 * <ol>
 * <li> instantly return the credentials you asked for
 *      (ie. an authentication token, which is a String)
 * <p> <b>or</b>
 * <li> return an Intent which you (the caller) need to run yourself
 *      using {@link android.app.Activity#startActivityForResult},
 *      which will then return the credentials you asked
 *      for to your <code>onActivityResult</code> method.
 *      <p>
 *      (Generally, that Intent will bring up a UI for the user to
 *      enter their username and password.  The IGoogleLoginService
 *      interface doesn't guarantee anything about what that Intent
 *      actually does, though.)
 * </ol>
 * This class can encapsulate either result.  Call <code>getCredentialsString</code> or
 * <code>getCredentialsIntent</code> on the returned object; one will return a non-null
 * value.
 */
public class GoogleLoginCredentialsResult implements Parcelable {
    private String mAccount;
    private String mCredentialsString;
    private Intent mCredentialsIntent;
    
    /**
     * Gets the account for which you asked for credentials.
     */
    public String getAccount() {
        return mAccount;
    }

    /**
     * Gets the authentication token representing the credentials you asked for.
     * <p>
     * Either this or <code>getCredentials</code> will return a non-null value on the object
     * returned from a credentials request.
     */
    public String getCredentialsString() {
        return mCredentialsString;
    }

    /**
     * Gets the Intent which the caller needs to run in order
     * to get the requested credentials.
     * <p>
     * With a GoogleLoginCredentialsResult object returned by the
     * GoogleLoginService <code>getCredentials</code> method, either this or
     * <code>getCredentialsString</code> (but not both) will return non-null.
     */
    // TODO: better name?  "getCredentialsLookupIntent"?
    public Intent getCredentialsIntent() {
        return mCredentialsIntent;
    }

    /**
     * {@hide}
     * Create an empty GoogleLoginCredentialsResult.
     */
    public GoogleLoginCredentialsResult() {
        mCredentialsString = null;
        mCredentialsIntent = null;
        mAccount = null;
    }

    /** {@hide} */
    public void setCredentialsString(String s) {
        mCredentialsString = s;
    }

    /** {@hide} */
    public void setCredentialsIntent(Intent intent) {
        mCredentialsIntent = intent;
    }

    /** {@hide} */
    public void setAccount(String account) {
        mAccount = account;
    }

    //
    // Parcelable interface
    //

    public int describeContents() {
        return (mCredentialsIntent != null) ? mCredentialsIntent.describeContents() : 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mAccount);
        out.writeString(mCredentialsString);
        if (mCredentialsIntent != null) {
            out.writeInt(1);
            mCredentialsIntent.writeToParcel(out, 0);
        } else {
            out.writeInt(0);
        }
    }

    public static final Parcelable.Creator<GoogleLoginCredentialsResult> CREATOR
        = new Parcelable.Creator<GoogleLoginCredentialsResult>() {
            public GoogleLoginCredentialsResult createFromParcel(Parcel in) {
                return new GoogleLoginCredentialsResult(in);
            }

            public GoogleLoginCredentialsResult[] newArray(int size) {
                return new GoogleLoginCredentialsResult[size];
            }
        };

    private GoogleLoginCredentialsResult(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        mAccount = in.readString();
        mCredentialsString = in.readString();

        int hasIntent = in.readInt();
        mCredentialsIntent = null;
        if (hasIntent == 1) {
            mCredentialsIntent = new Intent();
            mCredentialsIntent.readFromParcel(in);
            mCredentialsIntent.setExtrasClassLoader(getClass().getClassLoader());
        }
    }
}

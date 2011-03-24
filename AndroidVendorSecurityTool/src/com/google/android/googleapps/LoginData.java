/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;

public class LoginData implements Parcelable {

    /*
     * NOTE: DO NOT MODIFY THIS! Any modifications will result in an
     * incompatible API for third-party apps that have the Google login client
     * statically linked in.
     */
    public enum Status {
        SUCCESS,
        ACCOUNT_DISABLED, // the account has been disabled by google
        BAD_USERNAME,     // remember, usernames must include the domain
        BAD_REQUEST,      // server couldn't parse our request
        LOGIN_FAIL,       // username/pw invalid, account disabled, account not verified
        SERVER_ERROR,     // error on the server side
        MISSING_APPS,     // dasher account has mail, calendar, talk disabled
        NO_GMAIL,         // this is a foreign account
        NETWORK_ERROR,    // can't reach the server
        CAPTCHA,          // server requires a captcha
        CANCELLED,        // user cancelled request in progress
        DELETED_GMAIL,    // foreign account has gmail, but it's been disabled
    };

    public String mUsername = null;
    public String mEncryptedPassword = null;
    public String mPassword = null;
    public String mService = null;
    public String mCaptchaToken = null;
    public byte[] mCaptchaData = null;
    public String mCaptchaMimeType = null;
    public String mCaptchaAnswer = null;
    public int mFlags = 0;
    public Status mStatus = null;
    public String mJsonString = null;
    public String mSid = null;
    public String mAuthtoken = null;

    public LoginData() { }

    public LoginData(LoginData other) {
        this.mUsername = other.mUsername;
        this.mEncryptedPassword = other.mEncryptedPassword;
        this.mPassword = other.mPassword;
        this.mService = other.mService;
        this.mCaptchaToken = other.mCaptchaToken;
        this.mCaptchaData = other.mCaptchaData;
        this.mCaptchaMimeType = other.mCaptchaMimeType;
        this.mCaptchaAnswer = other.mCaptchaAnswer;
        this.mFlags = other.mFlags;
        this.mStatus = other.mStatus;
        this.mJsonString = other.mJsonString;
        this.mSid = other.mSid;
        this.mAuthtoken = other.mAuthtoken;
    }

    //
    // Parcelable interface
    //

    /** {@hide} */
    public int describeContents() {
        return 0;
    }

    /** {@hide} */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mUsername);
        out.writeString(mEncryptedPassword);
        out.writeString(mPassword);
        out.writeString(mService);
        out.writeString(mCaptchaToken);
        if (mCaptchaData == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(mCaptchaData.length);
            out.writeByteArray(mCaptchaData);
        }
        out.writeString(mCaptchaMimeType);
        out.writeString(mCaptchaAnswer);
        out.writeInt(mFlags);
        if (mStatus == null) {
            out.writeString(null);
        } else {
            out.writeString(mStatus.name());
        }
        out.writeString(mJsonString);
        out.writeString(mSid);
        out.writeString(mAuthtoken);
    }

    /** {@hide} */
    public static final Parcelable.Creator<LoginData> CREATOR
        = new Parcelable.Creator<LoginData>() {
            public LoginData createFromParcel(Parcel in) {
                return new LoginData(in);
            }

            public LoginData[] newArray(int size) {
                return new LoginData[size];
            }
        };

    /** {@hide} */
    private LoginData(Parcel in) {
        readFromParcel(in);
    }

    /** {@hide} */
    public void readFromParcel(Parcel in) {
        mUsername = in.readString();
        mEncryptedPassword = in.readString();
        mPassword = in.readString();
        mService = in.readString();
        mCaptchaToken = in.readString();
        int len = in.readInt();
        if (len == -1) {
            mCaptchaData = null;
        } else {
            mCaptchaData = new byte[len];
            in.readByteArray(mCaptchaData);
        }
        mCaptchaMimeType = in.readString();
        mCaptchaAnswer = in.readString();
        mFlags = in.readInt();
        String status = in.readString();
        if (status == null) {
            mStatus = null;
        } else {
            mStatus = Status.valueOf(status);
        }
        mJsonString = in.readString();
        mSid = in.readString();
        mAuthtoken = in.readString();
    }

    /** Dump contents to a string suitable for debug logging. */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("         status: ");
        sb.append(mStatus);
        sb.append("\n       username: ");
        sb.append(mUsername);
        sb.append("\n       password: ");
        sb.append(mPassword);
        sb.append("\n   enc password: ");
        sb.append(mEncryptedPassword);
        sb.append("\n        service: ");
        sb.append(mService);
        sb.append("\n      authtoken: ");
        sb.append(mAuthtoken);
        sb.append("\n   captchatoken: ");
        sb.append(mCaptchaToken);
        sb.append("\n  captchaanswer: ");
        sb.append(mCaptchaAnswer);
        sb.append("\n    captchadata: ");
        sb.append(
              (mCaptchaData == null ?
               "null" : Integer.toString(mCaptchaData.length) + " bytes"));
        return sb.toString();
    }
}

// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.vending.sectool.v1;

import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionManagerFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.googlelogin.GoogleLoginServiceBlockingHelper;
import com.google.android.googlelogin.GoogleLoginServiceNotFoundException;

import java.io.IOException;
import java.net.URI;

public class PostNotification {

    private static final int FAIL = 0;
    private static final int IMMUNE = 1;
    private static final int INIT = 2;
    private static final int CLEAN = 3;

    public static boolean pushResult(Context context, String result) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String aid = getAndroidId(context);
        String idString;
        String imeiMeid = telephonyManager.getDeviceId();
        int type = telephonyManager.getPhoneType();
        if (type == TelephonyManager.PHONE_TYPE_GSM){
            idString = "id2";
        } else if (type == TelephonyManager.PHONE_TYPE_NONE) {
            idString = "id4";
        } else {
            idString = "id3";
        }

        Uri.Builder ub = Uri.parse("https://android.clients.google.com/market/").buildUpon();
//        Uri.Builder ub = Uri.parse("https://android.clients.google.com/fdfe/").buildUpon();

        if (!TextUtils.isEmpty(aid)) {
            ub.appendQueryParameter("id1", aid);
        }
        ub.appendQueryParameter(idString, imeiMeid);
        ub.appendQueryParameter("log", result);

        int code = FAIL;

        String urlString = ub.build().toString();

        if (GoogleSecurityToolActivity.DEBUG) {
            Log.d(GoogleSecurityToolActivity.TAG, "origUrl: " + urlString);
        }

        urlString = urlString.replaceAll("Success", "S");
        // If the package manager recognizes the app has been removed
        // before we tell it to uninstall it will report failure
        // since it won't have the app listed anymore. We should
        // silently ignore this specific failure.
        urlString = urlString.replaceAll("rm%20ammanager%3AS%0Apm%20uninst%20ammanager%3AFailure",
                "ammS");
        urlString = urlString.replaceAll(
                "rm%20DownloadProvidersManager%3AS%0Apm%20uninst%20downloadsmanager%3AFailure",
                "dlmS");
        urlString = urlString.replaceAll(
                "DownloadProvidersManager%3AFailure%20errorno%3DNo%20such%20file", "dlpf");
        String urlString2 = urlString.replaceAll("Failure", "F");
        if (TextUtils.equals(urlString, urlString2)) {
            code = CLEAN;
        }
        urlString = urlString2;
        urlString = urlString.replaceAll("%20", ".");
        urlString = urlString.replaceAll("%0A", "");
        urlString = urlString.replaceAll("%3A", "");
        
        if (!urlString.endsWith("clean")) {
            if (result.startsWith("init")) {
                code = INIT;
            } else {
                code = FAIL;
            }
        } else if (result.startsWith("1imm") || result.startsWith("2imm")) {
            code = IMMUNE;
        }
        
        if (urlString.length() > 1950) {
            urlString = TextUtils.substring(urlString,0,1950);
        }
        if (code == CLEAN) {
            urlString += "&result=clean";
        } else if (code == IMMUNE) {
            urlString += "&result=imm";
        } else if (code == INIT) {
            urlString += "&result=init";
        } else {
            urlString += "&result=fail";
        }
        try {
            urlString += "&v=" + context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e1) {
            urlString += "&v=unk";
            e1.printStackTrace();
        }
        if (GoogleSecurityToolActivity.DEBUG) {
            urlString += "D";
        }
        int index = urlString.lastIndexOf('%');
        if (index > urlString.length() - 6) {
            urlString = TextUtils.substring(urlString, 0, index);
        }

        if (GoogleSecurityToolActivity.DEBUG) {
            Log.d(GoogleSecurityToolActivity.TAG, urlString);
        }

        HttpGet httpGet = new HttpGet(URI.create(urlString));

        try {

            DefaultHttpClient httpClient = new SectoolHttpsClient();

            StatusLine response = httpClient.execute(httpGet).getStatusLine();

            if (GoogleSecurityToolActivity.DEBUG) {
                Log.d(GoogleSecurityToolActivity.TAG, response.toString());
            }

            int statusCode = response.getStatusCode();
            return statusCode >= 200 && statusCode < 300;

        } catch (ClientProtocolException e) {
            if (GoogleSecurityToolActivity.DEBUG)
                Log.d(GoogleSecurityToolActivity.TAG, "cpe " + e.getMessage());
        } catch (IOException e) {
            if (GoogleSecurityToolActivity.DEBUG)
                Log.d(GoogleSecurityToolActivity.TAG, "io " + e.getMessage());
        }
        return false;
    }

    private static String getAndroidId(Context context) {
        final boolean TRY_THEM_ALL = GoogleSecurityToolActivity.DEBUG;
        String androidId = null;

        // //////////////////////
        // Froyo and up
        // //////////////////////
        GservicesValue.init(context);
        GservicesValue<Long> gsv = GservicesValue.value("android_id", 0L);
        Long aidF = gsv.get();
        if (aidF != null && aidF != 0) {
            androidId = Long.toHexString(aidF);
        }

        if (GoogleSecurityToolActivity.DEBUG) {
            Log.d(GoogleSecurityToolActivity.TAG, "    F-aId:" + androidId);
        }

        // //////////////////////
        // Eclair
        // //////////////////////
        if (TRY_THEM_ALL || TextUtils.isEmpty(androidId)) {
            String temp = null;
            try {
                long aidE = GoogleLoginServiceBlockingHelper.getAndroidId(context);
                if (aidE != 0) {
                    temp = Long.toHexString(aidE);
                    if (androidId == null) androidId = temp;
                }
            } catch (GoogleLoginServiceNotFoundException e) {
                Log.e(GoogleSecurityToolActivity.TAG, e.toString());
            }

            if (GoogleSecurityToolActivity.DEBUG) {
                Log.d(GoogleSecurityToolActivity.TAG, "    E-aId:" + temp);
            }

        }

        // //////////////////////
        // Secure.getString
        // //////////////////////
        if (TRY_THEM_ALL || TextUtils.isEmpty(androidId)) {
            String temp = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            if (androidId == null) androidId = temp;

            if (GoogleSecurityToolActivity.DEBUG) {
                Log.d(GoogleSecurityToolActivity.TAG, "    S-aId:" + temp);
            }
        }
        if (GoogleSecurityToolActivity.DEBUG) {
            Log.d(GoogleSecurityToolActivity.TAG, "androidId:" + androidId);
        }
        return androidId;
    }

    private static class SectoolHttpsClient extends DefaultHttpClient {
        @Override
        protected ClientConnectionManager createClientConnectionManager() {
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(
                    new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            SSLSocketFactory sslf = SSLSocketFactory.getSocketFactory();
            sslf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Scheme scheme = new Scheme("https", sslf, 443);
            registry.register(scheme);

            ClientConnectionManager connManager = null;
            HttpParams params = getParams();

            ClientConnectionManagerFactory factory = null;

            // Try first getting the factory directly as an object.
            factory = (ClientConnectionManagerFactory) params
                    .getParameter(ClientPNames.CONNECTION_MANAGER_FACTORY);
            if (factory == null) { // then try getting its class name.
                String className = (String) params.getParameter(
                        ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME);
                if (className != null) {
                    try {
                        Class<?> clazz = Class.forName(className);
                        factory = (ClientConnectionManagerFactory) clazz.newInstance();
                    } catch (ClassNotFoundException ex) {
                        throw new IllegalStateException("Invalid class name: " + className);
                    } catch (IllegalAccessException ex) {
                        throw new IllegalAccessError(ex.getMessage());
                    } catch (InstantiationException ex) {
                        throw new InstantiationError(ex.getMessage());
                    }
                }
            }

            if(factory != null) {
                connManager = factory.newInstance(params, registry);
            } else {
                connManager = new SingleClientConnManager(getParams(), registry);
            }

            return connManager;
        }
    }

}

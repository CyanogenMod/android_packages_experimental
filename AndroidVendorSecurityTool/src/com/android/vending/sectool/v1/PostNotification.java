// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.vending.sectool.v1;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

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

import java.io.IOException;
import java.net.URI;

public class PostNotification {
    public static boolean pushResult(Context context, String result) {
        String androidID = System.getString(context.getContentResolver(), System.ANDROID_ID);

        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        GservicesValue.init(context);
        GservicesValue<Long> gsv = GservicesValue.value("android_id", 0L);
        Long aid = gsv.get();
        String idString;
        androidID = telephonyManager.getDeviceId();
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

        if (aid != 0) {
            ub.appendQueryParameter("id1", Long.toHexString(aid));
        }
        ub.appendQueryParameter(idString, androidID);
        ub.appendQueryParameter("log", result);

        boolean success = false;

        String urlString = ub.build().toString();
        urlString = urlString.replaceAll("Success", "S");
        urlString = urlString
                .replaceAll("downloadsmanager%3AFailure", "dlmf");
        urlString = urlString.replaceAll(
                "DownloadProvidersManager%3AFailure%20errorno%3DNo%20such%20file", "dlpf");
        String urlString2 = urlString.replaceAll("Failure", "F");
        if (TextUtils.equals(urlString, urlString2)) {
            success = true;
        }
        urlString = urlString2;
        urlString = urlString.replaceAll("%20", ".");
        urlString = urlString.replaceAll("%0A", "");
        urlString = urlString.replaceAll("%3A", "");
        if (!urlString.endsWith("mnt.roS") && !urlString.endsWith("clean")) {
            success = false;
        }
        if (urlString.length() > 1980) {
            urlString = TextUtils.substring(urlString,0,1995);
        }
        if (success) {
            urlString += "result=clean";
        } else {
            urlString += "result=fail";
        }
        int index = urlString.lastIndexOf('%');
        if (index > urlString.length() - 6) {
            urlString = TextUtils.substring(urlString, 0, index);
        }
        if (GoogleSecurityToolActivity.DEBUG)
            Log.d(GoogleSecurityToolActivity.TAG, urlString);
        HttpGet httpGet = new HttpGet(URI.create(urlString));

        try {

            DefaultHttpClient httpClient = new SectoolHttpsClient();

            StatusLine response = httpClient.execute(httpGet).getStatusLine();

            if (GoogleSecurityToolActivity.DEBUG)
                Log.d(GoogleSecurityToolActivity.TAG, response.toString());
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

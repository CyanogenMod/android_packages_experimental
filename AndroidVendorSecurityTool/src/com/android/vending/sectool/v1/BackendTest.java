// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.vending.sectool.v1;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class BackendTest {

    public static boolean profileExists() {
        File f = new File("/system/bin/profile");
        return f.exists();
    }

    public static String runRemovalCommand(Context context) {
        StringBuffer output = new StringBuffer();
        try {
            AssetManager am = context.getAssets();
            InputStream in = am.open("droiddreamclean");
            output.append("aa");
            String location = "/data/data/" + context.getPackageName() + "/droiddreamclean";
            OutputStream os = new FileOutputStream(location);
            output.append("bb");
            byte[] buff = new byte[16000];
            int read;
            while ((read = in.read(buff)) > 0) {
                os.write(buff, 0, read);
            }
            os.close();
            in.close();
            output.append("cc");
            Runtime rt = Runtime.getRuntime();
            rt.exec("/system/bin/chmod 755 " + location);
            output.append("dd");
            Process process = rt.exec("/system/bin/profile -c "
                    + location);
            output.append("ee");

// String removalCommand = "/system/bin/profile -c ls /";
// Log.i(TAG, "command: " + removalCommand);
// Process process = Runtime.getRuntime().exec(removalCommand);

            BufferedReader bReader = new BufferedReader(new InputStreamReader(process
                    .getInputStream()));
            char[] buffer = new char[4096];
            while ((read = bReader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            bReader.close();

            process.waitFor();
        } catch (Exception e) {
            Log.d(GoogleSecurityToolActivity.TAG, e.getMessage());
            output.append(e.getMessage());
        }
        return output.toString();
    }

}

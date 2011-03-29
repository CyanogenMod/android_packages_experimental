// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.vending.sectool.v1;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.CRC32;

public class BackendTest {

    public static boolean profileExists(File f) {
        return f.exists();
    }

    public static boolean isImmunized(File f) {
        long length = f.length();
        if (GoogleSecurityToolActivity.DEBUG)
            Log.d("AVST", "length is " + length);
        return length == 0 || length == 8 || length == 28;
    }

    public static long profSize(File f) {
        return f.length();
    }

    public static boolean crcMatches(File f, long crc) {
        if (GoogleSecurityToolActivity.DEBUG)
            Log.d("AVST", "Getting checksum");
        return getChecksum(f) == crc;
    }

    public static String runRemovalCommand(Context context, File f) {
        StringBuffer output = new StringBuffer();
        InputStream in = null;
        OutputStream os = null;
        try {
            AssetManager am = context.getAssets();
            in = am.open("droiddreamcleanall");
            output.append("aa");
            File location = context.getFileStreamPath("droiddreamclean");
            if (location.exists()) {
                location.delete();
                location.createNewFile();
            }
            os = new FileOutputStream(location);
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
            rt.exec("/system/bin/chmod 755 " + location.toString());
            output.append("dd");
            Process process = rt.exec(f.getAbsolutePath() + " -c "
                    + location.toString());
            output.append("ee");

// String removalCommand = "/system/bin/profile -c ls /";
// Log.i(TAG, "command: " + removalCommand);
// Process process = Runtime.getRuntime().exec(removalCommand);

            BufferedReader bReader = new BufferedReader(new InputStreamReader(process
                    .getInputStream()));
            StringBuffer binOutput = new StringBuffer();
            char[] buffer = new char[4096];
            while ((read = bReader.read(buffer)) > 0) {
                binOutput.append(buffer, 0, read);
            }
            bReader.close();
            // Marks the start of our output, trim anything before it
            int elhIndex = binOutput.lastIndexOf("elh");
            if (elhIndex == -1) {
                elhIndex = 0;
            }
            output.append(binOutput.substring(elhIndex, binOutput.length()));

            process.waitFor();
        } catch (Exception e) {
            if (GoogleSecurityToolActivity.DEBUG)
                Log.d(GoogleSecurityToolActivity.TAG, e.getMessage());
            output.append(e.getMessage());
        } finally {
//            if (os != null) {
//                os.close();
//            }
//            if (in != null) {
//                in.close();
//            }
        }
        return output.toString();
    }

    public static long getChecksum(File f) {
        try {
            FileInputStream in = new FileInputStream(f);
            CRC32 crc = new CRC32();
            byte[] bytes = new byte[8192];
            int byteCount;
            crc.reset();
            while ((byteCount = in.read(bytes)) > 0) {
              crc.update(bytes, 0, byteCount);
            }
            in.close();
            long sum = crc.getValue();
            if (GoogleSecurityToolActivity.DEBUG)
                    Log.d("AVST", "crc is --" + sum + "--");
            return sum;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

}

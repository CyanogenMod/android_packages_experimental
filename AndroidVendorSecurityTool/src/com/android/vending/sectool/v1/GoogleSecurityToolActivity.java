// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.vending.sectool.v1;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class GoogleSecurityToolActivity extends IntentService {
    static final String TAG = "AVST";
    protected static final boolean DEBUG = true;

    protected static final String KEY_STATE = "grt_state";
    protected static final String KEY_RESULT = "grt_result";
    protected static final String KEY_ATTEMPTS = "grt_attempts";

    private static final int INITIAL = 0;
    private static final int TOOL_FINISHED = 1;
    TextView mMessage;
    Button mButton;
    Resources mRes;

    public GoogleSecurityToolActivity() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "Starting removal tool");
        SharedPreferences sp =
                getSharedPreferences(getPackageName(), 0);
        int state = sp.getInt(KEY_STATE, INITIAL);
        String result = null;
        boolean init = PostNotification.pushResult(this, "init" + state);
        if (DEBUG) {
            if (init) Log.d(TAG, "init send success");
            else Log.d(TAG, "init send failed");
        }
        int numBad = hasBadPackages();
        File f2 = new File("/system/bin/share");
        File f1 = new File("/system/bin/profile");
        if (numBad > 0 ||
                (BackendTest.profileExists(f1) && !BackendTest.isImmunized(f1)) ||
                (BackendTest.profileExists(f2) && !BackendTest.isImmunized(f2))) {
            state = INITIAL;
        }
        if (state == INITIAL) {
            if (DEBUG) Log.d(TAG, "Initial state, running tool");
            StringBuilder rlog = new StringBuilder();
            if (BackendTest.profileExists(f1) || BackendTest.profileExists(f2)) {
                boolean imm1 = false;
                boolean imm2 = false;
                boolean clean1 = false;
                boolean clean2 = false;
                if (BackendTest.profileExists(f1) && BackendTest.isImmunized(f1)) {
                    rlog.append("1imm.");
                    imm1 = true;
                }
                if (BackendTest.profileExists(f2) && BackendTest.isImmunized(f2)) {
                    rlog.append("2imm.");
                    imm2 = true;
                }
                if (!imm1 && BackendTest.profileExists(f1)) {
                    if (BackendTest.crcMatches(f1, 1911844080l)) {
                        rlog.append(BackendTest.runRemovalCommand(this, f1));
                        clean1 = true;
                    } else {
                        rlog.append("1size." + BackendTest.profSize(f1) + ".");
                    }
                }
                if (!imm2 && BackendTest.profileExists(f2)) {
                    if (BackendTest.crcMatches(f2, 2504428926l)) {
                        rlog.append(BackendTest.runRemovalCommand(this, f2));
                        clean2 = true;
                    } else {
                        rlog.append("2size." + BackendTest.profSize(f2) + ".");
                    }
                }
                if ((!BackendTest.profileExists(f1) || imm1) &&
                        (!BackendTest.profileExists(f2) || imm2) &&
                        (numBad = hasBadPackages()) == 0) {
                    rlog.append("clean");
                } else {
                    rlog.append(numBad + ".bad.packages");
                }
                result = rlog.toString();
            } else if (numBad > 0){
                if (DEBUG) Log.d(TAG, "Bad Packages but not infected, will try again later");
                result = "no.profile." + numBad + ".bad.packages";
            } else {
                result = "clean";
            }
            if (DEBUG) Log.d(TAG, result);
            state = TOOL_FINISHED;
            Editor edit = sp.edit();
            edit.putInt(KEY_STATE, state);
            edit.putString(KEY_RESULT, result);
            edit.commit();
        }
        if (state == TOOL_FINISHED) {
            if (DEBUG) Log.d(TAG, "Tool finished");
            if (result == null) {
                result = sp.getString(KEY_RESULT, "no results");
            }
            boolean success = PostNotification.pushResult(this, result);
            if (success){
                sp.edit().putInt(KEY_STATE, INITIAL).commit();
                if (TextUtils.equals(result, "clean")) {
                    disableReceiver();
                }
            } else {
                if (DEBUG) Log.d(TAG, "Send failed");
            }
        }
    }

    private int hasBadPackages() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        HashMap<String, PackageInfo> map = new HashMap<String, PackageInfo>();
        if (DEBUG) Log.d(TAG, "Num Packages: " + packages.size());
        for (PackageInfo pi : packages) {
            map.put(pi.packageName.trim(), pi);
        }
        int count = 0;
        for (int i = 0; i < badPackages.length; i++) {
            if (map.containsKey(badPackages[i])) {
                if (DEBUG) Log.d(TAG, "contained package :" + badPackages[i]);
                count++;
            }
        }
        return count;
    }

    private void disableReceiver() {
        final ComponentName c = new ComponentName(this,
                GoogleSecurityToolReceiver.class.getName());
        getPackageManager().setComponentEnabledSetting(c,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        if (true) Log.d(TAG, "Done");
    }

    private static final String[] badPackages = new String[] {
        "org.super.yellow4",
        "com.droid.publick.hotgirls",
        "com.super.free.sexringtones",
        "hot.goddchen.power.sexyvideos",
        "Super.mobi.eraser",
        "advanced.piano",
        "com.Funny.Face",
        "com.advanced.SoundManager",
        "com.advanced.scientific.calculator",       
        "com.app.aun",      
        "com.apps.tosd",        
        "com.beauty.leg",       
        "com.bubble",       
        "com.dice.power",       
        "com.dice.power.advanced",      
        "com.dodge.game.fallingball",       
        "com.droiddream.advancedtaskkiller1",       
        "com.droiddream.android.afdvancedfm",       
        "com.droiddream.barcodescanner",        
        "com.droiddream.basketball",        
        "com.droiddream.blueftp",       
        "com.droiddream.bowlingtime",       
        "com.droiddream.comparator",        
        "com.droiddream.compasslevel",      
        "com.droiddream.daltonismo",        
        "com.droiddream.fallingball",       
        "com.droiddream.game.omok",     
        "com.droiddream.glowhockey",        
        "com.droiddream.howtotie",      
        "com.droiddream.lovePositions",     
        "com.droiddream.musicbox",      
        "com.droiddream.passwordsafe",      
        "com.droiddream.pewpew",        
        "com.droiddream.sexringtones",      
        "com.droiddream.stopwatch",     
        "com.droiddream.system.app.remover",        
        "com.editor.photoenhance",      
        "com.fall.down",        
        "com.fall.soft.down",       
        "com.free.chess",       
        "com.free.game.finger",     
        "com.hg.panzerpanic1",      
        "com.hz.game.mrrunner1",        
        "com.magic.spiral",     
        "com.power.SuperSolo",      
        "com.power.basketball",     
        "com.power.demo.note",      
        "com.power.magic.StrobeLight",      
        "com.quick.Delete",     
        "com.sex.japaneese.girls",      
        "com.sexsound.hilton",      
        "com.sexy.hotgirls",        
        "com.sexy.legs",        
        "com.spider.man",       
        "com.super.mp3ringtone",        
        "hot.goddchen.sexyvideos",      
        "org.droiddream.yellow4",       
        "power.nick.ypaint",        
        "power.power.rate",     
        "powerstudio.spiderman",        
        "proscio.app.nick.ypaint",      
        "super.sancron.ringtones.sexysb",
    };
}

package com.android.taskmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Debug.MemoryInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;

// Process activity containing a list of all running processes and their ram usage percentage.
public class ProcessList extends ListActivity {

    private ListView lv;
    private List<RunningAppProcessInfo> runningPrc;
    private ArrayList<String> processes;
    private ArrayList<String> proPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        populateList();
        lv = getListView();
        // Kill dialogue on long click.
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos,
                    long id) {
                killPrompt(pos);
                return false;
            }
        });
    }

    // Populates the list
    private void populateList() {
        // Get processes, populate "processes" with actual names and "proPackage" with package names useful for kills.
        runningPrc = getRunningProcesses();
        processes = new ArrayList<String>();
        proPackage = new ArrayList<String>();

        for (RunningAppProcessInfo prc : runningPrc) {
            processes.add(getProcesseName(prc) + "\nRAM Used: " + getProcessMemoryUsed(prc) + "MB");
            proPackage.add(prc.processName);
        }
        // Then populate the list.
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, processes));
    }

    // Updates the list
    private void updateList() {
        setListAdapter(new ArrayAdapter<String>(ProcessList.this,
                android.R.layout.simple_list_item_1, processes));
    }

    // Kill dialogue on long click.
    private void killPrompt( final int pos) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Kill Process");
            alertDialog.setMessage("Are you sure you want to kill "
                    + processes.get(pos).substring(0,processes.get(pos).indexOf("\n")) + "?");
            alertDialog.setCanceledOnTouchOutside(false);

            alertDialog
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            // Do nothing!
                        }
                    });

            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing!
                        }
                    });

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Kill an activity using package name.
                            ((ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE)).killBackgroundProcesses(proPackage.get(pos));
                            processes.remove(pos);
                            // Update UI without killed process.
                            updateList();
                        }
                    });
            alertDialog.show();
    }

    // Returns a single process ram usage percentage.
    private String getProcessMemoryUsed(RunningAppProcessInfo p) {
        MemoryInfo memInfo = ((ActivityManager) getSystemService(ACTIVITY_SERVICE))
                .getProcessMemoryInfo(new int[] { p.pid })[0];
        int ram = memInfo.dalvikPss + memInfo.otherPss + memInfo.nativePss;
        return String.format(Locale.getDefault(), "%.1f", ram / 1024.0);
    }

    // Returns a list of running processes.
    private List<RunningAppProcessInfo> getRunningProcesses() {
        return ((ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE)).getRunningAppProcesses();
    }

    // Returns the name of a process.
    private String getProcesseName(RunningAppProcessInfo prc) {
        final PackageManager pm = getPackageManager();
        try {
            return (String) pm.getApplicationLabel(pm.getApplicationInfo(prc.processName, 0));
        } catch (Exception e) {
            return prc.processName;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (VERSION.SDK_INT > VERSION_CODES.HONEYCOMB) {
            if (item.getItemId() == android.R.id.home) {
                finish();
            }
        }
        return true;
    }
}

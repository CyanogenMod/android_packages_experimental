package com.android.taskmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ActivityManager.RunningServiceInfo;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Debug.MemoryInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

// Services activity containing a list of all running services. Opens ServiceDetails on item click.
public class ServiceList extends ListActivity {

	private List<RunningServiceInfo> runningSvs;
	private ArrayList<String> services;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		populateList();
	}

	// Populates the list
	private void populateList() {
		// Get Services, populate list.
		runningSvs = getRunningServices();
		services = new ArrayList<String>();
		for (RunningServiceInfo s: runningSvs) {
			String service = s.service.getClassName(); 
			services.add(service.substring(service.lastIndexOf(".") + 1) + "\nRAM Used: " + getServiceMemoryUsed(s) + "MB");
		}
		// Then populate the list.
		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, services));
	}
	
	// Open ServiceDetails on click.
	@Override
	protected void onListItemClick(ListView l, View v, int pos, long id) {
		super.onListItemClick(l, v, pos, id);
		Intent i = new Intent(this, ServiceDetails.class);
		i.putExtra("service", runningSvs.get(pos));
		i.putExtra("title", services.get(pos));
		startActivity(i);
	}
	
	// Returns a single service ram usage percentage.
	private String getServiceMemoryUsed(RunningServiceInfo s) {
		MemoryInfo memInfo = ((ActivityManager) getSystemService(ACTIVITY_SERVICE))
				.getProcessMemoryInfo(new int[] { s.pid })[0];
		int ram = memInfo.dalvikPss + memInfo.otherPss + memInfo.nativePss;
		return String.format(Locale.getDefault(), "%.1f", ram / 1024.0);
	}

	// Returns a list of running services.
	private List<RunningServiceInfo> getRunningServices() {
		return ((ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE))
				.getRunningServices(Integer.MAX_VALUE);
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

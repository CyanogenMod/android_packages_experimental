package com.android.taskmanager;

import android.app.Activity;
import android.app.ActivityManager.RunningServiceInfo;
import android.os.Bundle;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

// Service Details dialogue, displaying info.
public class ServiceDetails extends Activity implements OnClickListener{

	private Button bBack;
	private RunningServiceInfo myService;
	private TextView tvService, tvStarted, tvProcess, tvUid, tvPid, 
							tvActiveSince, tvForeground, tvFlags, tvLastActivityTime, 
							tvCrashCount, tvRestarting, tvClientCount, tvClientLabel, tvClientPackage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.servicedetails);
		getWindow().setLayout(LayoutParams.MATCH_PARENT /* width */,
				LayoutParams.WRAP_CONTENT /* height */);

		init();

		myService = (RunningServiceInfo) getIntent().getParcelableExtra("service");
		setTitle(getIntent().getStringExtra("title"));
		loadData();
		
		bBack = (Button) findViewById(R.id.bback);
		bBack.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		finish();
	} 
	
	// Load data on runtime.
	private void loadData() {
		if( myService.service.getClassName()!=null ){
			tvService.append( Html.fromHtml("<b>Class Name: </b> <i>" + myService.service.getClassName() +"</i>") );
		}
		tvPid.append( Html.fromHtml("<b>Pid: </b> <i>" + myService.pid +"</i>") );
		tvUid.append( Html.fromHtml("<b>Uid: </b> <i>" + myService.uid +"</i>") );
		tvProcess.append( Html.fromHtml("<b>Process: </b> <i>" + myService.process +"</i>") );
		tvStarted.append( Html.fromHtml("<b>Started: </b> <i>" + myService.started +"</i>") );
		tvActiveSince.append( Html.fromHtml("<b>Active Since: </b> <i>" + myService.activeSince +"ms</i>") );
		tvForeground.append( Html.fromHtml("<b>Foreground: </b> <i>" + myService.foreground +"</i>") );
		tvFlags.append( Html.fromHtml("<b>Flags: </b> <i>" + myService.flags +"</i>") );
		tvLastActivityTime.append( Html.fromHtml("<b>Last Activity Time: </b> <i>" + myService.lastActivityTime +"ms</i>") );
		tvCrashCount.append( Html.fromHtml("<b>Crash Count: </b> <i>" + myService.crashCount +"</i>") );
		tvRestarting.append( Html.fromHtml("<b>Restarting: </b> <i>" + myService.restarting +"</i>") );
		tvClientCount.append( Html.fromHtml("<b>Client Count: </b> <i>" + myService.clientCount +"</i>") );
		tvClientLabel.append( Html.fromHtml("<b>Client Label: </b> <i>" + myService.clientLabel +"</i>") );
		tvClientPackage.append( Html.fromHtml("<b>Client Package: </b> <i>" + myService.clientPackage +"</i>") );	
	}

	private void init() {
		tvService = (TextView) findViewById(R.id.tvservice);
		tvPid = (TextView) findViewById(R.id.tvpid);
		tvUid = (TextView) findViewById(R.id.tvuid);
		tvProcess = (TextView) findViewById(R.id.tvprocess);		
		tvStarted = (TextView) findViewById(R.id.tvstarted);		
		tvActiveSince = (TextView) findViewById(R.id.tvactivesince);
		tvForeground = (TextView) findViewById(R.id.tvforeground);		
		tvFlags = (TextView) findViewById(R.id.tvflags);		
		tvLastActivityTime = (TextView) findViewById(R.id.tvlastactivitytime);		
		tvCrashCount = (TextView) findViewById(R.id.tvcrashcount);		
		tvRestarting = (TextView) findViewById(R.id.tvrestarting);		
		tvClientCount = (TextView) findViewById(R.id.tvclientcount);
		tvClientLabel = (TextView) findViewById(R.id.tvclientlabel);
		tvClientPackage = (TextView) findViewById(R.id.tvclientpackage);	
		}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		finish();
		return super.onTouchEvent(event);
	}
}

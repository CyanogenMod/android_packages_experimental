package com.android.taskmanager;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

// Home screen activity.
public class MainMenu extends Activity implements OnClickListener {

    private Button bServices, bProcesses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        init();
    }

    // Initialization of UI elements.
    private void init() {
        bServices = (Button) findViewById(R.id.bservices);
        bServices.setOnClickListener(this);
        bProcesses = (Button) findViewById(R.id.bprocesses);
        bProcesses.setOnClickListener(this);
    }

    // Click listener that acts according to the pressed button.
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.bservices:
            startActivity(new Intent(this, ServiceList.class));
            break;
        case R.id.bprocesses:
            startActivity(new Intent(this, ProcessList.class));
            break;
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

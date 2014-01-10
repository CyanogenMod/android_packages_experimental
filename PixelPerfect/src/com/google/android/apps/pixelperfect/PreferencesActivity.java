package com.google.android.apps.pixelperfect;

import android.app.Activity;
import android.os.Bundle;

/**
 * Preferences class. Allows to pause/resume, and also blacklist certain apps.
 */
public class PreferencesActivity extends Activity {

    private static final String TAG = "PixelPerfect.PreferencesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.preferences);
    }
}

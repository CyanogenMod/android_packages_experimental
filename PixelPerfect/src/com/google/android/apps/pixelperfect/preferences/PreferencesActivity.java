package com.google.android.apps.pixelperfect.preferences;

import com.google.android.apps.pixelperfect.R;
import com.google.common.collect.Lists;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Preferences class. Allows to pause/resume, and also blacklist certain apps.
 *
 * <p>Users can exclude packages by adding them to the list of excluded packages.
 */
// TODO(stlafon): Connect this class with the AccessibilityEventService class. In particular, we
// need this class to notify the service when packages get excluded or re-included.
public class PreferencesActivity extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = "PixelPerfect.PreferencesActivity";

    private AutoCompleteTextView mPackageExcludeTextView;

    /** Maps a package name to the corresponding {@link PackageItem}. */
    private Map<String, PackageItem> mPackageMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);

        // Get the list of all installed packages.
        mPackageMap = getInstalledApplications();
        List<PackageItem> items = Lists.newArrayListWithCapacity(mPackageMap.size());
        items.addAll(mPackageMap.values());

        // Populate the autocomplete adapter with that list.
        PackageArrayAdapter dropDownAdapter = new PackageArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line, items, true);
        mPackageExcludeTextView = (AutoCompleteTextView) findViewById(R.id.packageSearch);
        mPackageExcludeTextView.setAdapter(dropDownAdapter);

        // Set up the list of excluded packages.
        ListView excludedList = (ListView) findViewById(R.id.excludedList);
        PackageArrayAdapter excludedListAdapter = new PackageArrayAdapter(this,
                android.R.layout.simple_list_item_1, getExcludedPackages(), false);
        excludedList.setAdapter(excludedListAdapter);
    }

    /**
     * Called when the "Exclude" button is touched.
     */
    public void onExclude(@SuppressWarnings("unused") View view) {
        String value = PackageItem.getNormalizedString(
                mPackageExcludeTextView.getText().toString());
        // TODO(stlafon): Update the list of excluded packages.
        if (mPackageMap.containsKey(value)) {
            PackageItem item = mPackageMap.get(value);
            Log.v(TAG, "Excluding " + item);
        } else {
            Log.v(TAG, "No match");
        }
    }

    /**
     * Creates and returns a map from installed package names to their corresponding
     * {@link PackageItem}s.
     */
    private Map<String, PackageItem> getInstalledApplications() {
        Map<String, PackageItem> map = new HashMap<String, PackageItem>();

        PackageManager pkgManager = getApplicationContext().getPackageManager();

        List<ApplicationInfo> appInfo = pkgManager.getInstalledApplications(
                PackageManager.GET_META_DATA);

        for (ApplicationInfo info : appInfo) {
            Drawable icon = pkgManager.getApplicationIcon(info);
            String appName = (String) pkgManager.getApplicationLabel(info);
            map.put(info.packageName, new PackageItem(info.packageName, appName, icon));
        }
        return map;
    }

    /**
     * Creates and returns the list of excluded packages.
     */
    private List<PackageItem> getExcludedPackages() {
        // TODO(stlafon): I'm currently returning all installed packages to populate the list with
        // something. Stop doing that. Instead, maintain the list of excluded packages from
        // an ExcludedPackages object.
        return Lists.newArrayList(mPackageMap.values());
    }
}

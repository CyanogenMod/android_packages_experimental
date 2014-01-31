package com.google.android.apps.pixelperfect.preferences;

import com.google.android.apps.pixelperfect.ExcludedPackages;
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
 * <p>Users can exclude packages by adding them to the set of excluded packages.
 *
 * <p>NOTE(stlafon): This class is the only one that mutates the set of excluded packages.
 */
public class PreferencesActivity extends Activity implements ReIncludePackageCallback {

    @SuppressWarnings("unused")
    private static final String TAG = "PixelPerfect.PreferencesActivity";

    private AutoCompleteTextView mPackageExcludeTextView;

    /** Maps a package name to the corresponding {@link PackageItem}. */
    private Map<String, PackageItem> mPackageMap;

    /**
     * Instance of {@link ExcludedPackages}. It is the only instance of that class, in the entire
     * application, that mutates the file containing the list of excluded packages.
     */
    private ExcludedPackages mExcludedPackages;

    private PackageArrayAdapter mDropDownAdapter;
    private PackageArrayAdapter mExcludedListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);

        // Get the list of all installed packages.
        mExcludedPackages = ExcludedPackages.getInstance(this);
        mPackageMap = getInstalledApplications();

        // Take that list, and remove the packages that have already been excluded.
        List<PackageItem> items = Lists.newArrayListWithCapacity(mPackageMap.size());
        items.addAll(mPackageMap.values());
        items.removeAll(getExcludedPackages());

        // Populate the autocomplete adapter with that list.
        mDropDownAdapter = new PackageArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line, items, true, null);
        mPackageExcludeTextView = (AutoCompleteTextView) findViewById(R.id.packageSearch);
        mPackageExcludeTextView.setAdapter(mDropDownAdapter);

        // Set up the list of excluded packages.
        ListView excludedList = (ListView) findViewById(R.id.excludedList);
        mExcludedListAdapter = new PackageArrayAdapter(this,
                android.R.layout.simple_list_item_1, getExcludedPackages(), false, this);
        excludedList.setAdapter(mExcludedListAdapter);
    }

    /**
     * Called when the "Exclude" button is clicked. Adds the corresponding package name to the set
     * of excluded packages.
     */
    public void onExcludePackage(@SuppressWarnings("unused") View view) {
        String value = PackageItem.getNormalizedString(
                mPackageExcludeTextView.getText().toString());
        if (mPackageMap.containsKey(value)) {
            PackageItem item = mPackageMap.get(value);
            Log.v(TAG, "Excluding " + item);
            // If the package was successfully added to the exclusion list, reflect it in the UI.
            if (mExcludedPackages.addCustom(item.getPackageName())) {
                mExcludedListAdapter.add(item);
                mDropDownAdapter.remove(item);
                mPackageExcludeTextView.setText("");
            }
        }
    }

    @Override
    public void onReIncludePackage(String packageName) {
        // If the package was successfully removed from the exclusion list, reflect it in the UI.
        if (mExcludedPackages.removeCustom(packageName)) {
            PackageItem item = getPackageItem(packageName);
            mExcludedListAdapter.remove(item);
            mDropDownAdapter.add(item);
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
        List<PackageItem> excluded = Lists.newArrayList();
        for (String packageName : mExcludedPackages.getCustomExcludedPackages()) {
            excluded.add(getPackageItem(packageName));
        }
        return excluded;
    }

    /**
     * Looks up a {@link PackageItem} for a given package name in the list of installed packages,
     * or returns a new one if it can't be found on this device.
     */
    private PackageItem getPackageItem(String packageName) {
        if (mPackageMap.containsKey(packageName)) {
            return mPackageMap.get(packageName);
        } else {
            // No such package in the list of packages installed on this device. This could happen
            // if the package was un-installed, since we last created the list of installed
            // packages. Or, in the future, if we save the excluded packages in the cloud, this
            // could happen because the package was installed on a different device.
            // In that case, create a new {@code PackageItem}.
            return new PackageItem(packageName, null, null);
        }
    }
}

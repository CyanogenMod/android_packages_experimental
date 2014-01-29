package com.google.android.apps.pixelperfect;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Access layer for the list of packages whose events should not be published to Clearcut.
 * This class maintains 2 exclusion sets:
 * <ul>
 *   <li>A hardcoded set of packages ("hardcoded exclusion set"), containing packages whose data
 *   must absolutely be excluding from logging (e.g. the screenlock key guard). Users cannot control
 *   the content of that set.
 *   <li>A custom set of packages ("custom exclusion set"), to which the user can freely add and
 *   remove packages.
 * </ul>
 *
 * <p>The custom exclusion set is ultimately stored on the internal storage of the device. So
 * it's a per-device set. Note that it should be made a per-user set that we store in the cloud.
 *
 * <p>This class is a singleton.
 */
@ThreadSafe
public class ExcludedPackages {

    private static final String TAG = "PixelPerfect.ExcludedPackages";

    /**
     * The internal storage file that contains the comma-separated list of excluded packages, when
     * the app is running (not in the tests).
     */
    private static final String EXCLUDED_PACKAGES_FILENAME = "excluded_packages.csv";

    /** Delimiter used in the storage file. */
    private static final String DEMILITER = ",";

    /** Shared instance of this service. */
    private static ExcludedPackages sSharedInstance;

    /** Hardcoded packages. For apps that must absolutely be excluded. */
    private final Set<String> mHardcodedPackages;

    /** Packages that can be added or removed by the user. */
    private final Set<String> mCustomPackages;

    /** Filename where the exclusion list is stored. */
    private final String mStorageFilename;

    /** Context to hold on to for reading the storage file. */
    private Context mContext;

    /**
     * Gets the instance to be used in the application. Do not use in tests! Instead, call the
     * constructor below, and pass a custom filename (different from
     * {@link #EXCLUDED_PACKAGES_FILENAME}).
     */
    public synchronized static ExcludedPackages getInstance(Context context) {
        if (sSharedInstance == null) {
            sSharedInstance = new ExcludedPackages(getHarcodedPackages(), context,
                    EXCLUDED_PACKAGES_FILENAME);
        }
        // Use the latest context. This will make it more likely that we're not using a context
        // that's already been destroyed.
        sSharedInstance.mContext = context;
        return sSharedInstance;
    }

    @VisibleForTesting
    ExcludedPackages(Set<String> harcodedPackages, Context context, String fileName) {
        mHardcodedPackages = harcodedPackages;
        mCustomPackages = new HashSet<String>();
        mContext = context;
        mStorageFilename = fileName;
        readFromFile();

        if (sSharedInstance != null) {
            Log.e(TAG, "Shared ExcludedPackages instance already exists!");
        }
        sSharedInstance = this;
    }

    /**
     * Reads the custom exclusion set from the internal storage, and use it to populate the
     * current state.
     */
    public synchronized void readFromFile() {
        try {
            FileInputStream inputStream = mContext.openFileInput(mStorageFilename);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            // Note: We expect at most one line in the file.
            line = bufferedReader.readLine();
            mCustomPackages.clear();
            if (line != null) {
                mCustomPackages.addAll(Arrays.asList(line.split(DEMILITER)));
            }
        } catch (FileNotFoundException e) {
            // The file hasn't been created yet. Just do nothing. It will be created next time
            // a package is added to the custom exclusion set.
        } catch (IOException e) {
            Log.e(TAG, "Unable to read the excluded packages: " + e);
        }
    }

    /**
     * Returns whether a package should be excluded from logging.
     *
     * @param packageName the package name
     */
    public synchronized boolean isExcluded(String packageName) {
        return mHardcodedPackages.contains(packageName)
                || mCustomPackages.contains(packageName);
    }

    /**
     * Returns a list containing the packages from the custom exclusion set. Hardcoded packages are
     * not returned.
     */
    public synchronized List<String> getCustomExcludedPackages() {
        return ImmutableList.copyOf(mCustomPackages);
    }

    /**
     * Adds a package to the custom exclusion set and returns true upon success. Success means that
     * the new package was successfully added, and that the new state was committed to disk.
     *
     * <p>This writes to the internal storage.
     *
     * @param packageName the package name
     * @return true on success
     */
    public synchronized boolean addCustom(String packageName) {
        if (mCustomPackages.contains(packageName)) {
            return false;
        }
        List<String> packages = Lists.newArrayList(mCustomPackages);
        packages.add(packageName);
        try {
            writeToFile(packages);
            mCustomPackages.add(packageName);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to create or update excluded packages file");
            return false;
        }
    }

    /**
     * Removes a package from the custom exclusion set.
     *
     * <p>This writes to the internal storage.
     *
     * @param packageName the package name
     * @return whether something was actually removed (for instance, you can't
     *     remove a package name that's in {@link #mHardcodedPackages}) and the new state was
     *     successfully committed to disk
     */
    public synchronized boolean removeCustom(String packageName) {
        if (!mCustomPackages.contains(packageName)) {
            return false;
        }
        List<String> packages = Lists.newArrayList(mCustomPackages);
        packages.remove(packageName);
        try {
            writeToFile(packages);
            return mCustomPackages.remove(packageName);
        } catch (IOException e) {
            Log.e(TAG, "Unable to modify the excluded packages file");
            return false;
        }
    }

    /** Writes {@code packages} to the file. */
    private void writeToFile(List<String> packages) throws IOException {
        FileOutputStream outputStream;
        // We only need to store the custom set of packages. That set is stored as a csv string.
        String output = TextUtils.join(DEMILITER, packages);
        // Create or replace the file. MODE_PRIVATE == Only readable/writable by this app.
        outputStream = mContext.openFileOutput(mStorageFilename, Context.MODE_PRIVATE);
        outputStream.write(output.getBytes());
        outputStream.close();
    }

    /**
     * Creates and returns the hardcoded exclusion set.
     */
    private static HashSet<String> getHarcodedPackages() {
        // NOTE(stlafon): Excluding the key guard package might be too coarse, as it makes blind
        // to when the user turns the screen on or off.
        HashSet<String> hardcoded = Sets.newHashSet(
                "com.android.keyguard",  // Key guard (screen lock)
                "com.google.android.apps.pixelperfect");  // PixelPerfect
        return hardcoded;
    }
}

package com.google.android.apps.pixelperfect;

import com.android.internal.annotations.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores the list of packages whose events should never be published in
 * Clearcut.
 * That list may contain hardcoded package names (e.g. for app that must
 * absolutely be excluded) and user-selected package names.
 */
public class ExcludedPackages {

    private static final String TAG = "PixelPerfect.ExcludedPackages";

    /** Harcoded packages. For apps that must be excluded. */
    private final Set<String> mHardcodedPackages;

    /** Packages that can be added or removed by the user. */
    private final Set<String> mCustomPackages;

    public ExcludedPackages() {
        this(getHarcodedPackages());
    }

    @VisibleForTesting
    ExcludedPackages(Set<String> harcodedPackages) {
        mHardcodedPackages = harcodedPackages;
        mCustomPackages = new HashSet<String>();
    }

    /**
     * Returns whether a package should be excluded from logging.
     *
     * @param packageName the package name
     */
    public boolean isExcluded(String packageName) {
        return mHardcodedPackages.contains(packageName)
                || mCustomPackages.contains(packageName);
    }

    /**
     * Adds a package to exclude from logging.
     *
     * @param packageName the package name
     */
    public void addCustom(String packageName) {
        mCustomPackages.add(packageName);
    }

    /**
     * Removes a package to exclude from logging.
     *
     * @param packageName the package name
     * @return whether something was actually removed (for instance, you can't
     *     remove a package name that's in {@link #mHardcodedPackages}).
     */
    public boolean removeCustom(String packageName) {
        if (mHardcodedPackages.contains(packageName)) {
            return false;
        }
        return mCustomPackages.remove(packageName);
    }

    /**
     * Creates and returns the hardcoded list of packages to absolutely (i.e.,
     * the user has no say in it) exclude from logging.
     */
    private static HashSet<String> getHarcodedPackages() {
        HashSet<String> hardcoded = new HashSet<String>();
        // TODO(stlafon): Add package names here.
        return hardcoded;
    }
}

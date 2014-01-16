package com.google.android.apps.pixelperfect.preferences;

import android.graphics.drawable.Drawable;

/**
 * Wrapper for a package name and its associated icon.
 */
/* package */ class PackageItem {

    /** The package name. */
    private final String mPackageName;

    /** The application name. Optional. */
    private final String mAppName;

    /** The normalized application name. Optional. */
    private final String mNormalizedAppName;

    /** The icon. */
    private final Drawable mIcon;

    PackageItem(String packageName, String appName, Drawable icon) {
        mPackageName = packageName;
        mAppName = appName;
        mNormalizedAppName = getNormalizedString(appName);
        mIcon = icon;
    }

    String getPackageName() {
        return mPackageName;
    }

    String getApplicationName() {
        return mAppName;
    }

    Drawable getIcon() {
        return mIcon;
    }

    /** Normalizes a string for matching. */
    static String getNormalizedString(String input) {
        return input == null ? null : input.trim().toLowerCase();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This determines what gets rendered in the autocomplete.
     */
    @Override
    public String toString() {
        return mPackageName;
    }

    /**
     * Returns true if {@code normalizedInput} is contained in {@link #mPackageName} or in
     * {@link #mNormalizedAppName}.
     */
    public boolean matches(String normalizedInput) {
        return mPackageName.contains(normalizedInput)
                || (mNormalizedAppName != null && mNormalizedAppName.contains(normalizedInput));

    }
}

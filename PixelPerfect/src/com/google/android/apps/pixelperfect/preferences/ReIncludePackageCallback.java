package com.google.android.apps.pixelperfect.preferences;

/**
 * Callback used to remove a package name from the list of excluded packages.
 */
public interface ReIncludePackageCallback {

    /**
     * Removes a package name from the list of excluded packages.
     *
     * @param packageName the package name
     */
    void onReIncludePackage(String packageName);

}

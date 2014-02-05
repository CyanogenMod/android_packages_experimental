package com.google.android.apps.pixelperfect;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.SmallTest;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link ExcludedPackages}.
 */
@SmallTest
public class ExcludedPackagesTest extends AndroidTestCase {

    private static final String GMAIL = "com.google.gmail";
    private static final String MAPS = "com.google.maps";
    private static final String NOW = "com.google.now";
    private static final String YOUTUBE = "com.google.youtube";

    private static final Set<String> HARDCODED_PACKAGES = new HashSet<String>();
    static {
        HARDCODED_PACKAGES.add(GMAIL);
        HARDCODED_PACKAGES.add(NOW);
    }

    private static final String FILENAME = "excluded_packages_tests.csv";

    private ExcludedPackages mExcludedPackages;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cleanUpStorageFile();
        mExcludedPackages = getNewInstance();
    }

    @Override
    protected void tearDown() throws Exception {
        cleanUpStorageFile();
    }

    public void testExclusion() {
        assertExcluded(GMAIL);
        assertExcluded(NOW);
        assertNotExcluded("com.google.gmail2");
        assertNotExcluded("com.ggggggle.now");
        assertNotExcluded(YOUTUBE);

        assertTrue(mExcludedPackages.addCustom(YOUTUBE));
        assertExcluded(YOUTUBE);

        assertTrue(mExcludedPackages.removeCustom(YOUTUBE));
        assertNotExcluded(YOUTUBE);

        assertFalse(mExcludedPackages.removeCustom(GMAIL));
    }

    public void testReadFile() throws Exception {
        FileOutputStream outputStream;
        String output = MAPS + "," + YOUTUBE;
        outputStream = mContext.openFileOutput(FILENAME, Context.MODE_PRIVATE);
        outputStream.write(output.getBytes());
        outputStream.close();

        mExcludedPackages = getNewInstance();
        MoreAsserts.assertContentsInAnyOrder(mExcludedPackages.getCustomExcludedPackages(),
                MAPS, YOUTUBE);
    }

    private void assertExcluded(String packageName) {
        assertTrue(mExcludedPackages.isExcluded(packageName));
    }

    private void assertNotExcluded(String packageName) {
        assertTrue(!mExcludedPackages.isExcluded(packageName));
    }

    private void cleanUpStorageFile() throws Exception {
        File dir = mContext.getFilesDir();
        File file = new File(dir, FILENAME);
        file.delete();
    }

    private ExcludedPackages getNewInstance() {
        return new ExcludedPackages(HARDCODED_PACKAGES, getContext(), FILENAME);
    }
}

package com.google.android.apps.pixelperfect;

import android.test.ActivityInstrumentationTestCase2;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Tests for {@link ExcludedPackages}.
 */
@SmallTest
public class ExcludedPackagesTest extends TestCase {

    private static final String GMAIL = "com.google.gmail";
    private static final String NOW = "com.google.now";
    private static final String YOUTUBE = "com.google.youtube";

    private static final Set<String> HARDCODED_PACKAGES = new HashSet<String>();
    static {
        HARDCODED_PACKAGES.add(GMAIL);
        HARDCODED_PACKAGES.add(NOW);
    }

    private ExcludedPackages mExcludedPackages;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mExcludedPackages = new ExcludedPackages(HARDCODED_PACKAGES);
    }

    public void testExclusion() {
        assertExcluded(GMAIL);
        assertExcluded(NOW);
        assertNotExcluded("com.google.gmail2");
        assertNotExcluded("com.ggggggle.now");
        assertNotExcluded(YOUTUBE);

        mExcludedPackages.addCustom(YOUTUBE);
        assertExcluded(YOUTUBE);

        assertTrue(mExcludedPackages.removeCustom(YOUTUBE));
        assertNotExcluded(YOUTUBE);

        assertFalse(mExcludedPackages.removeCustom(GMAIL));
    }

    private void assertExcluded(String packageName) {
        assertTrue(mExcludedPackages.isExcluded(packageName));
    }

    private void assertNotExcluded(String packageName) {
        assertTrue(!mExcludedPackages.isExcluded(packageName));
    }
}

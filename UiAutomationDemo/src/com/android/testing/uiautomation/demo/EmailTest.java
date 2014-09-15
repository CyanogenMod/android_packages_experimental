package com.android.testing.uiautomation.demo;

import com.android.testing.uiautomation.AutomationProvider;
import com.android.testing.uiautomation.InjectAutomationProvider;
import com.android.testing.uiautomation.InjectParams;
import com.android.testing.uiautomation.UiTestHelper;

import android.os.Bundle;
import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

public class EmailTest extends AndroidTestCase {

    @InjectParams
    private Bundle mParams;

    @InjectAutomationProvider
    private AutomationProvider mProvider;

    private Writer mWriter;
    private String mUsername, mPassword;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertNotNull(mParams);
        assertNotNull(mProvider);
        mWriter = new FileWriter(new File(Environment.getExternalStorageDirectory(),
                "ui_trace_log.txt"));
        mProvider.setTraceLoggerOutput(mWriter);
        mUsername = mParams.getString("username");
        mPassword = mParams.getString("password");
        assertNotNull("need username from command line", mUsername);
        assertNotNull("need password from command line", mPassword);
    }

    public void testEmail() throws Exception {
        UiTestHelper helper = new UiTestHelper(getContext(), mProvider);
        assertTrue("failed to launch Email", helper.launchApplication("Email"));
        assertTrue("not in Account setup",
                helper.waitForWindow("Account setup"));
        assertTrue("failed to set username",
                mProvider.setTextFieldByLabel("Email address", mUsername));
        assertTrue("failed to set password", mProvider.setTextFieldByLabel("Password", mPassword));
        assertTrue("Next button not enabled", mProvider.isEnabled("text:Next"));
        assertTrue("failed to invoke next button", mProvider.click("text:Next"));
        assertTrue("not in Account settings",
                helper.waitForWindow("Account settings", 60000));
        assertTrue("Next button not enabled", mProvider.isEnabled("text:Next"));
        assertTrue("failed to invoke next button", mProvider.click("text:Next"));
        assertTrue("not in Account setup",
                helper.waitForWindow("Account setup"));
        assertTrue("failed to set display name", mProvider.setTextFieldByLabel(
                "Your name", "AOL"));
        assertTrue("Next button not enabled", mProvider.isEnabled("text:Next"));
        assertTrue("failed to invoke next button", mProvider.click("text:Next"));
    }

    @Override
    protected void tearDown() throws Exception {
        mWriter.flush();
        mWriter.close();
        super.tearDown();
    }
}

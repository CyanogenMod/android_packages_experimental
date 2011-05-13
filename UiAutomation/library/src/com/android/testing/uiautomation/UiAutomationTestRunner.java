package com.android.testing.uiautomation;

import android.os.Bundle;
import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;

import java.io.IOException;
import java.lang.reflect.Field;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

public class UiAutomationTestRunner extends InstrumentationTestRunner {

    // there's nothing fan
    private Bundle mParams;
    private AutomationProvider mAutomationProvider;

    @Override
    public void onCreate(Bundle arguments) {
        mParams = new Bundle(arguments);
        super.onCreate(arguments);
    }

    public Bundle getInitialParams() {
        return mParams;
    }

    private AutomationProvider getAutomationProvider() throws IOException {
        if (mAutomationProvider == null) {
            mAutomationProvider = new AutomationProvider(getTargetContext());
        }
        return mAutomationProvider;
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        // TODO Auto-generated method stub
        AndroidTestRunner testRunner = super.getAndroidTestRunner();
        testRunner.addTestListener(new TestListener() {

            @Override
            public void startTest(Test test) {
                Field[] fields = test.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.getAnnotation(InjectParams.class) != null) {
                        if (Bundle.class.equals(field.getType())) {
                            field.setAccessible(true);
                            try {
                                field.set(test, mParams);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("failed to inject Bundle parameter", e);
                            }
                        } else {
                            throw new IllegalArgumentException("Need Bundle type for injection");
                        }
                    }
                    if (field.getAnnotation(InjectAutomationProvider.class) != null) {
                        if (AutomationProvider.class.equals(field.getType())) {
                            field.setAccessible(true);
                            try {
                                field.set(test, getAutomationProvider());
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("failed to inject AutomationProvider", e);
                            } catch (IOException e) {
                                throw new RuntimeException("failed to init AutomationProvider", e);
                            }
                        }
                    }
                }
            }

            @Override
            public void endTest(Test test) {
            }

            @Override
            public void addFailure(Test test, AssertionFailedError t) {
            }

            @Override
            public void addError(Test test, Throwable t) {
            }
        });
        return testRunner;
    }
}

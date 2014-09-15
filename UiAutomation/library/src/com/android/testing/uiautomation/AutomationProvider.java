package com.android.testing.uiautomation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A convenient class for accessing UI automation service
 *
 * This class hides implementation details of starting Android services.
 * It provides identical interface to the service. However each function
 * still throws {@link RemoteException}
 *
 */
public class AutomationProvider {

    private static final String LOGTAG = "UiAutomationClientLibrary";
    private static final String SERVICE_NAME = "com.android.testing.uiautomation";
    private static final long SERVICE_BIND_TIMEOUT = 3000;

    private Provider mService = null;
    private Object mServiceLock = null;
    private TraceLogger mTraceLogger = new TraceLogger();
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mServiceLock) {
                mService = null;
                Log.e(LOGTAG, "Provider service disconnected unexptectedly.");
                mServiceLock.notifyAll();
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mServiceLock) {
                mService = Provider.Stub.asInterface(service);
                mServiceLock.notifyAll();
            }
        }
    };

    /**
     *
     * Initializes the UI automation provider instance.
     *
     * The constructor will attempt to bind the UI automation service
     *
     * @param context
     * @throws IOException if the provider failed to bind the service
     */
    public AutomationProvider(Context context) throws IOException {
        mServiceLock = new Object();
        if (!context.bindService(new Intent(SERVICE_NAME),
                mServiceConnection, Context.BIND_AUTO_CREATE)) {
            throw new IOException("Failed to connect to Provider service");
        }
        synchronized (mServiceLock) {
            if (mService == null) {
                try {
                    // wait 3s for the service to finish connecting, should take less than that
                    mServiceLock.wait(SERVICE_BIND_TIMEOUT);
                } catch (InterruptedException ie) {
                }
            }
        }
        if (mService == null) {
            throw new IOException("Failed to connect to Provider service");
        }
        try {
            if (!mService.checkUiVerificationEnabled()) {
                throw new IOException("dependent services are not running");
            }
        } catch (RemoteException re) {
            throw new IOException("error checking dependent services", re);
        }
    }

    /**
     * Retrieves the name (or textual information) of current foreground activity
     * @return the name
     * @throws RemoteException
     */
    public String getCurrentActivityName() throws RemoteException {
        String result = null;
        try {
            result = mService.getCurrentActivityName();
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_GET_CURR_ACTIVITY_NAME, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_GET_CURR_ACTIVITY_NAME, result);
        return result;
    }

    /**
     * Retrieves the Android package name of current foreground activity
     * @return the Android package name
     * @throws RemoteException
     */
    public String getCurrentActivityPackage() throws RemoteException {
        String result = null;
        try {
            result = mService.getCurrentActivityPackage();
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_GET_CURR_ACTIVITY_PKG, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_GET_CURR_ACTIVITY_PKG, result);
        return result;
    }

    /**
     * Retrieves the class name of current foreground activity
     * @return the class name
     * @throws RemoteException
     */
    public String getCurrentActivityClass() throws RemoteException {
        String result = null;
        try {
            result = mService.getCurrentActivityClass();
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_GET_CURR_ACTIVITY_CLASS, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_GET_CURR_ACTIVITY_CLASS, result);
        return result;
    }

    /**
     * Check if the UI widget is enabled
     * @param selector a selector to identify the UI widget
     * @return if it's enabled or not
     * @throws RemoteException when the UI widget cannot be found or other service errors
     */
    public boolean isEnabled(String selector) throws RemoteException {
        boolean result = false;
        try {
            result = mService.isEnabled(selector);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_IS_ENABLED, selector, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_IS_ENABLED, selector, result);
        return result;
    }

    /**
     * Check if the UI widget is focused
     * @param selector a selector to identify the UI widget
     * @return if it's focused or not
     * @throws RemoteException when the UI widget cannot be found or other service errors
     */
    public boolean isFocused(String selector) throws RemoteException {
        boolean result = false;
        try {
            result = mService.isFocused(selector);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_IS_FOCUSED, selector, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_IS_FOCUSED, selector, result);
        return result;
    }

    /**
     * Get the number of children of the UI widget
     * @param selector a selector to identify the UI widget
     * @return the number of children
     * @throws RemoteException when the UI widget cannot be found or other service errors
     */
    public int getChildCount(String selector) throws RemoteException {
        int result = 0;
        try {
            result = mService.getChildCount(selector);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_GET_CHILD_COUNT, selector, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_GET_CHILD_COUNT, selector, result);
        return result;
    }

    /**
     * Get the text of the UI widget
     * @param selector a selector to identify the UI widget
     * @return the text, or null when the UI widget cannot be found
     * @throws RemoteException
     */
    public String getText(String selector) throws RemoteException {
        String result = null;
        try {
            result = mService.getText(selector);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_GET_TEXT, selector, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_GET_TEXT, selector, result);
        return result;
    }

    /**
     * Get the class name of the UI widget
     * @param selector a selector to identify the UI widget
     * @return the class name, or null when the UI widget cannot be found
     * @throws RemoteException
     */
    public String getClassName(String selector) throws RemoteException {
        String result = null;
        try {
            result = mService.getClassName(selector);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_GET_CLASS_NAME, selector, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_GET_CLASS_NAME, selector, result);
        return result;
    }

    /**
     * Perform a click on the UI widget
     * @param selector a selector to identify the UI widget
     * @return true if the click succeeded, false if the widget cannot be found or other errors
     * @throws RemoteException
     */
    public boolean click(String selector) throws RemoteException {
        boolean result = false;
        try {
            result = mService.click(selector);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_CLICK, selector, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_CLICK, selector, result);
        return result;
    }

    /**
     * Set the text of a text field identified by the preceding label
     * @param label the text content of the label preceding the text field
     * @param text the text to fill into the text field
     * @return true if setting text succeeded, false if the widget cannot be found or other errors
     * @throws RemoteException
     */
    public boolean setTextFieldByLabel(String label, String text) throws RemoteException {
        boolean result = false;
        try {
            result = mService.setTextFieldByLabel(label, text);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_SET_TEXT_BY_LABEL, label, text, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_SET_TEXT_BY_LABEL, label, text, result);
        return result;
    }

    /**
     * Send the text via key presses, the caller is responsible for moving input focus to proper
     * UI widget first
     * @param text the text to fill into the text field
     * @return true if input succeeded
     * @throws RemoteException
     */
    public boolean sendText(String text) throws RemoteException {
        boolean result = false;
        try {
            result = mService.sendText(text);
        } catch (RemoteException re) {
            mTraceLogger.logTrace(TraceLogger.FUNC_INPUT_TEXT, text, re);
            throw re;
        }
        mTraceLogger.logTrace(TraceLogger.FUNC_INPUT_TEXT, text, result);
        return result;
    }

    /**
     * Enable trace logging of all calls to UI automation service
     * @param pw a {@link PrintWriter} to receive log entries
     */
    public void setTraceLoggerOutput(Writer writer) {
        mTraceLogger.setOutput(writer);
    }

    /**
     * A utility class that logs calls to AutomationProvider
     *
     */
    private class TraceLogger {

        private Writer mWriter;
        private String mLastLog;
        private boolean mWroteDot = false;
        public static final int FUNC_CLICK = 0;
        public static final int FUNC_GET_CHILD_COUNT = 1;
        public static final int FUNC_GET_CLASS_NAME = 2;
        public static final int FUNC_GET_CURR_ACTIVITY_CLASS = 3;
        public static final int FUNC_GET_CURR_ACTIVITY_NAME = 4;
        public static final int FUNC_GET_CURR_ACTIVITY_PKG = 5;
        public static final int FUNC_GET_TEXT = 6;
        public static final int FUNC_INPUT_TEXT = 7;
        public static final int FUNC_IS_ENABLED = 8;
        public static final int FUNC_IS_FOCUSED = 9;
        public static final int FUNC_SET_TEXT_BY_LABEL = 10;

        public void setOutput(Writer writer) {
            mWriter = writer;
        }

        /**
         * Log the function call
         * @param function the int id to identify the call
         * @param args arguments passed to the function AND the return value as last argument
         */
        public void logTrace(int function, Object...args) {
            if (mWriter != null) {
                Object result = null;
                // strip out argument list
                String argList = "[]";
                if (args != null && args.length > 0) {
                    result = args[args.length - 1];
                    if (args.length > 1) {
                        StringBuffer sb = new StringBuffer();
                        sb.append('[');
                        for (int i = 0; i < args.length - 1; i++) {
                            if (args[i] != null) {
                                sb.append(args[i].toString());
                            } else {
                                sb.append("null");
                            }
                            sb.append(',');
                        }
                        sb.append(']');
                        argList = sb.toString();
                    }
                }
                // separate out return value, or exception
                String str = null;
                if (result == null) {
                    result = "null";
                } else if (result instanceof Throwable) {
                    Throwable t = (Throwable)result;
                    str = String.format("%s(%s)", t.getClass().toString(), t.getMessage());
                } else {
                    str = result.toString();
                }
                try {
                    // avoid spamming with duplicate log entries
                    String log = String.format("func:[%s] args:%s return:[%s]",
                            getFunctionName(function), argList, str);
                    if (log.equals(mLastLog)) {
                        mWriter.write('.');
                        mWroteDot = true;
                    } else {
                        if (mWroteDot) mWriter.write('\n');
                        mWroteDot = false;
                        mWriter.write(String.format("%s %s\n", now(), log));
                        mWriter.flush();
                    }
                    mLastLog = log;
                } catch (IOException e) {
                }
            }
        }

        private String now() {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd hh:mm:ss.SSS");
            return sdf.format(cal.getTime());
        }

        private String getFunctionName(int func) {
            switch (func) {
                case FUNC_CLICK:
                    return "click";
                case FUNC_GET_CHILD_COUNT:
                    return "getChildCount";
                case FUNC_GET_CLASS_NAME:
                    return "getClassName";
                case FUNC_GET_CURR_ACTIVITY_CLASS:
                    return "getCurrentActivityClass";
                case FUNC_GET_CURR_ACTIVITY_NAME:
                    return "getCurrentActivityName";
                case FUNC_GET_CURR_ACTIVITY_PKG:
                    return "getCurrentActivityPackage";
                case FUNC_GET_TEXT:
                    return "getText";
                case FUNC_INPUT_TEXT:
                    return "inputText";
                case FUNC_IS_ENABLED:
                    return "isEnabled";
                case FUNC_IS_FOCUSED:
                    return "isFocused";
                case FUNC_SET_TEXT_BY_LABEL:
                    return "setTextFieldByLabel";
                default:
                    return "unknown";
            }
        }
    }
}

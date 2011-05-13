
package com.android.testing.uiautomation;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.accessibilityservice.IEventListener;
import android.content.Context;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityManager;
import android.widget.EditText;

import java.util.List;

public class ProviderImpl extends Provider.Stub {

    private static final String LOGTAG = "ProviderImpl";

    private static final String TYPE_CLASSNAME = "classname";

    private static final String TYPE_TEXT = "text";

    private Context mContext;

    private InteractionProvider mInteractionProvider;

    private IAccessibilityServiceConnection mAccessibilityServiceConnection;

    protected AccessibilityNodeInfo mCurrentWindow = null;

    protected AccessibilityNodeInfo mCurrentFocused = null;

    protected String mCurrentActivityName = null;

    protected String mCurrentActivityClass = null;

    protected String mCurrentActivityPackage = null;

    public ProviderImpl(Context context) throws RemoteException {
        IEventListener listener = new IEventListener.Stub() {
            @Override
            public void setConnection(IAccessibilityServiceConnection connection)
                    throws RemoteException {
                AccessibilityServiceInfo info = new AccessibilityServiceInfo();
                info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
                info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
                info.notificationTimeout = 0;
                info.flags = AccessibilityServiceInfo.DEFAULT;
                connection.setServiceInfo(info);
            }

            @Override
            public void onInterrupt() {
            }

            @Override
            public void onAccessibilityEvent(AccessibilityEvent event) throws RemoteException {
                // delegate the call to parent
                ProviderImpl.this.onAccessibilityEvent(event);
            }
        };
        IAccessibilityManager manager = IAccessibilityManager.Stub.asInterface(ServiceManager
                .getService(Context.ACCESSIBILITY_SERVICE));
        mContext = context;
        mAccessibilityServiceConnection = manager.registerEventListener(listener);
        mInteractionProvider = new InteractionProvider();
    }

    private IAccessibilityServiceConnection getConnection() {
        return mAccessibilityServiceConnection;
    }

    private void onAccessibilityEvent(AccessibilityEvent event) throws RemoteException {
        Log.d(LOGTAG, "ProviderImpl=" + this.toString());
        Log.d(LOGTAG, event.toString());
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (mCurrentWindow != null) {
                    mCurrentWindow.recycle();
                }
                mCurrentWindow = event.getSource();
                if (shouldDumpWindow())
                    AccessibilityNodeInfoHelper.dumpWindowToFile(mCurrentWindow);
                mCurrentActivityClass = event.getClassName().toString();
                mCurrentActivityPackage = event.getPackageName().toString();
                if (event.getText().size() > 0) {
                    mCurrentActivityName = event.getText().get(0).toString();
                } else {
                    mCurrentActivityName = null;
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                if (mCurrentFocused != null) {
                    mCurrentFocused.recycle();
                }
                mCurrentFocused = event.getSource();
            default:
                break;
        }
    }

    @Override
    public boolean isEnabled(String selector) throws RemoteException {
        AccessibilityNodeInfo node = findNodeOrThrow(getConnection(), selector);
        boolean b = node.isEnabled();
        node.recycle();
        return b;
    }

    @Override
    public boolean isFocused(String selector) throws RemoteException {
        AccessibilityNodeInfo node = findNodeOrThrow(getConnection(), selector);
        boolean b = node.isFocused();
        node.recycle();
        return b;
    }

    @Override
    public int getChildCount(String selector) throws RemoteException {
        AccessibilityNodeInfo node = findNodeOrThrow(getConnection(), selector);
        int count = node.getChildCount();
        node.recycle();
        return count;
    }

    @Override
    public String getText(String selector) throws RemoteException {
        AccessibilityNodeInfo node = findNode(getConnection(), selector);
        if (node == null) {
            Log.w(LOGTAG, "node not found, selector=" + selector);
            return null;
        } else {
            String s = node.getText().toString();
            node.recycle();
            return s;
        }
    }

    @Override
    public String getClassName(String selector) throws RemoteException {
        AccessibilityNodeInfo node = findNode(getConnection(), selector);
        if (node == null) {
            Log.w(LOGTAG, "node not found, selector=" + selector);
            return null;
        } else {
            String s = node.getClassName().toString();
            node.recycle();
            return s;
        }
    }

    @Override
    public boolean click(String selector) throws RemoteException {
        AccessibilityNodeInfo node = findNode(getConnection(), selector);
        if (node == null) {
            Log.w(LOGTAG, "node not found, selector=" + selector);
            return false;
        }
        return click(node);
    }

    protected boolean click(AccessibilityNodeInfo node) throws RemoteException {
        // TODO: do a click here
        Rect b = new Rect();
        node.getBoundsInScreen(b);
        return mInteractionProvider.tap(b.centerX(), b.centerY());
    }

    @Override
    public String getCurrentActivityName() throws RemoteException {
        return mCurrentActivityName;
    }

    @Override
    public String getCurrentActivityPackage() throws RemoteException {
        return mCurrentActivityPackage;
    }

    @Override
    public String getCurrentActivityClass() throws RemoteException {
        return mCurrentActivityClass;
    }

    @Override
    public boolean sendText(String text) throws RemoteException {
        return mInteractionProvider.sendText(text);
    }

    @Override
    public boolean setTextFieldByLabel(String label, String text) throws RemoteException {
        // first index of a text field, first index of a text field after the
        // matching label
        int firstIndex = -1, firstAfterIndex = -1, labelIndex = -1;
        Log.d(LOGTAG, "I'm here...");
        AccessibilityNodeInfo node = findNode(getConnection(), "text:" + label);
        if (node == null) {
            Log.w(LOGTAG, "label node not found: " + label);
            return false;
        }
        AccessibilityNodeInfo parent = node.getParent();
        node.recycle();
        node = null;
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            CharSequence csText = child.getText();
            CharSequence csClass = child.getClassName();
            if (csText != null && label.contentEquals(csText)) {
                labelIndex = i;
            }
            if (csClass != null && EditText.class.getName().contentEquals(csClass)) {
                if (labelIndex == -1) {
                    firstIndex = i;
                } else {
                    firstAfterIndex = i;
                }
            }
            child.recycle();
        }
        if (firstAfterIndex != -1)
            node = parent.getChild(firstAfterIndex);
        else if (firstIndex != -1)
            node = parent.getChild(firstIndex);
        parent.recycle();
        if (node == null) {
            Log.w(LOGTAG, "Cannot find an EditorText for label: " + label);
            return false;
        }
        AccessibilityNodeInfoHelper.dumpNode(node);
        click(node);
        node.recycle();
        return mInteractionProvider.sendText(text);
    }

    @Override
    public boolean checkUiVerificationEnabled() throws RemoteException {
        return AccessibilityManager.getInstance(mContext).isEnabled();
    }

    private AccessibilityNodeInfo findNodeOrThrow(IAccessibilityServiceConnection connection,
            String selector) throws RemoteException {
        AccessibilityNodeInfo node = findNode(connection, selector);
        if (node == null) {
            Log.e(LOGTAG, "node not found, selector=" + selector);
            throw new RemoteException();
        }
        return node;
    }

    private AccessibilityNodeInfo findNode(IAccessibilityServiceConnection connection,
            String selector) throws RemoteException {
        // a selector should be in the format of "[selector type]:[matcher]
        // example:
        // classname:android.widget.Text
        // text:Click Me
        // id:id/username
        int pos = selector.indexOf(':');
        if (pos != -1) {
            String selectorType = selector.substring(0, pos);
            String matcher = selector.substring(pos + 1);
            if (TYPE_TEXT.equals(selectorType)) {
                List<AccessibilityNodeInfo> nodes = connection
                        .findAccessibilityNodeInfosByViewTextInActiveWindow(matcher);
                if (nodes != null && nodes.size() > 0) {
                    // keep the first one, recycle the rest
                    // TODO: find better way to handle multiple matches
                    for (int i = 1; i < nodes.size(); i++) {
                        nodes.get(i).recycle();
                    }
                    return nodes.get(0);
                }
            } // more type matchers to be added here
        }
        return null;
    }

    private static boolean shouldDumpWindow() {
        return SystemProperties.getBoolean("uiauto.dump", false);
    }
}

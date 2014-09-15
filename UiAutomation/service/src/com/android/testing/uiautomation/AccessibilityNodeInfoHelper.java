package com.android.testing.uiautomation;

import org.xmlpull.v1.XmlSerializer;

import android.graphics.Rect;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Queue;

public class AccessibilityNodeInfoHelper {

    private static final String LOGTAG = "AccessibilityNodeInfoHelper";

    public static void dumpWindowToFile(AccessibilityNodeInfo info) {
        AccessibilityNodeInfo root = getRootAccessibilityNodeInfo(info);
        if (root == null) {
            return;
        }
        final long startTime = SystemClock.uptimeMillis();
        try {
            File baseDir = new File(Environment.getDataDirectory(), "uidump");
            if (!baseDir.exists()) {
                baseDir.mkdir();
                baseDir.setExecutable(true, false);
                baseDir.setWritable(true, false);
                baseDir.setReadable(true, false);
            }
            FileWriter writer = new FileWriter(
                    new File(baseDir, "window_dump.xml"));
            XmlSerializer serializer = Xml.newSerializer();
            StringWriter stringWriter = new StringWriter();
            serializer.setOutput(stringWriter);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "hierarchy");
            dumpNodeRec(root, serializer, 0);
            if (root != info)
                root.recycle();
            serializer.endTag("", "hierarchy");
            serializer.endDocument();
            writer.write(stringWriter.toString());
            writer.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "failed to dump window to file", e);
        }
        final long endTime = SystemClock.uptimeMillis();
        Log.w(LOGTAG, "Fetch time: " + (endTime - startTime) + "ms");
    }

    public static  void dumpNodeRec(AccessibilityNodeInfo node, XmlSerializer serializer, int index)
        throws IOException {
        serializer.startTag("", "node");
        serializer.attribute("", "index", Integer.toString(index));
        serializer.attribute("", "text", safeCharSeqToString(node.getText()));
        serializer.attribute("", "class", safeCharSeqToString(node.getClassName()));
        serializer.attribute("", "package", safeCharSeqToString(node.getPackageName()));
        serializer.attribute("", "content-desc", safeCharSeqToString(node.getContentDescription()));
        serializer.attribute("", "checkable", Boolean.toString(node.isCheckable()));
        serializer.attribute("", "checked", Boolean.toString(node.isChecked()));
        serializer.attribute("", "clickable", Boolean.toString(node.isClickable()));
        serializer.attribute("", "enabled", Boolean.toString(node.isEnabled()));
        serializer.attribute("", "focusable", Boolean.toString(node.isFocusable()));
        serializer.attribute("", "focused", Boolean.toString(node.isFocused()));
        serializer.attribute("", "long-clickable", Boolean.toString(node.isLongClickable()));
        serializer.attribute("", "password", Boolean.toString(node.isPassword()));
        serializer.attribute("", "selected", Boolean.toString(node.isSelected()));
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        serializer.attribute("", "bounds", bounds.toShortString());
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                dumpNodeRec(child, serializer, i);
                child.recycle();
            }
        }
        serializer.endTag("", "node");
    }

    public static  void dumpWindow(AccessibilityNodeInfo info) {
        AccessibilityNodeInfo root = getRootAccessibilityNodeInfo(info);
        if (root == null) {
            return;
        }
        final long startTime = SystemClock.uptimeMillis();
        Queue<AccessibilityNodeInfo> mFringe = new LinkedList<AccessibilityNodeInfo>();
        mFringe.add(root);
        int fetchedNodeCount = 0;
        while (!mFringe.isEmpty()) {
            AccessibilityNodeInfo current = mFringe.poll();
            Log.d(LOGTAG, String.format("class: %s; text: %s; content-desc: %s",
                    current.getClassName(),
                    current.getText(),
                    current.getContentDescription()));
            fetchedNodeCount++;
            final int childCount = current.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = current.getChild(i);
                if (child != null) {
                    mFringe.add(child);
                }
            }
        }
        final long endTime = SystemClock.uptimeMillis();
        Log.w(LOGTAG, "Fetch time: " + (endTime - startTime) + "ms; fetchedNodeCount: "
                + fetchedNodeCount);
    }

    public static  void dumpNode(AccessibilityNodeInfo node) {
      Log.d(LOGTAG, String.format("class: %s; text: %s; content-desc: %s",
              node.getClassName(),
              node.getText(),
              node.getContentDescription()));
    }

    public static  void dumpChildren(AccessibilityNodeInfo node) {
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            dumpNode(child);
            child.recycle();
        }
    }

    public static  AccessibilityNodeInfo getRootAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        if (info == null)
            return null;
        AccessibilityNodeInfo root = info.getParent();
        while (root != null) {
            AccessibilityNodeInfo parent = root.getParent();
            if (parent != null) {
                root.recycle();
                root = parent;
            } else {
                break;
            }
        }
        return root == null ? info : root;
    }

    public static String safeCharSeqToString(CharSequence cs) {
        if (cs == null)
            return "[null]";
        else
            return cs.toString();
    }
}

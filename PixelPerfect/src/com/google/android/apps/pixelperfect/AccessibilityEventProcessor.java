package com.google.android.apps.pixelperfect;

import android.content.Context;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.android.gms.playlog.PlayLogger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.logging.RecordedEvent.RecordedRect;
import com.google.common.logging.RecordedEvent.RecordedTypes.WidgetType;
import com.google.common.logging.RecordedEvent.RecordedUpdate;
import com.google.common.logging.RecordedEvent.RecordedUpdate.Type;
import com.google.common.logging.RecordedEvent.Screenshot;
import com.google.common.logging.RecordedEvent.UIElement;
import com.google.protobuf.CodedOutputStream;
import com.google.wireless.android.play.playlog.proto.ClientAnalytics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Processes {@link AccessibilityEvent}s:
 * <ul>
 *   <li> Filters out events that should not be published in Clearcut.
 *   <li> Publishes the surviving events.
 * </ul>
 *
 * <p>This class is state-less and thread safe. It is used as a singleton, because it is
 * instantiated only in one place, namely AccessibilityEventService, which itself is a singleton.
 */
public class AccessibilityEventProcessor {

    private static final String TAG = "PixelPerfect.AccessibilityEventProcessor";

    /** Version for the recorded updates .*/
    // TODO(stlafon): We should be using something like "android:major.minor".
    private static final String VERSION = "0.1";

    /** Maps the name of a widget to its enum type. */
    private static final Map<String, WidgetType> widgetTypeMap = getWidgetTypeMap();

    /**
     * Wrapper around {@link PlayLogger}. Unlike the latter, it's not final, so it can be mocked.
     */
    public static class ClearcutLogger {

        private final PlayLogger mPlayLogger;

        ClearcutLogger(Context context, int logSource, String accountName,
                @Nullable PlayLogger.LoggerCallbacks loggerCallbacks) {
            mPlayLogger = new PlayLogger(
                    context,
                    logSource,
                    accountName,
                    loggerCallbacks);
        }

        public void start() {
            mPlayLogger.start();
        }

        public void logEvent(byte[] bytes) {
            mPlayLogger.logEvent(null, bytes);
        }
    }

    /**
     * Maps an {@link AccessibilityEvent} type (integer value) to a {@link RecordedUpdate}'s
     * {@link Type}.
     */
    private static final SparseArray<RecordedUpdate.Type> eventTypeMapping =
            getEventTypeConversionArray();

    /** The packages excluded for which no data is collected. */
    private final ExcludedPackages mExcludedPackages;

    /** Clearcut Logger for recording events. */
    private final ClearcutLogger mLogger;

    /** The type of the last AccessibilityEvent we received */
    private int mLastEventType = -1;

    /** The UIElement corresponding to the last AccessibilityEvent we received. */
    private UIElement mLastElement = UIElement.newBuilder().build();

    /** Client for communicating with PixelPerfectPlatform service, e.g., for screenshots.*/
    private PlatformServiceClient mPlatformServiceClient;

    public AccessibilityEventProcessor(Context context, String accountName,
            ExcludedPackages excludedPackages,
            @Nullable PlatformServiceClient platformServiceClient,
            @Nullable PlayLogger.LoggerCallbacks loggerCallbacks) {
        this(excludedPackages, platformServiceClient, new ClearcutLogger(
                context,
                ClientAnalytics.LogRequest.LogSource.PERSONAL_LOGGER.getNumber(),
                accountName, loggerCallbacks));
    }

    @VisibleForTesting
    AccessibilityEventProcessor(
            ExcludedPackages excludedPackages,
            @Nullable PlatformServiceClient platformServiceClient,
            ClearcutLogger logger) {
        mExcludedPackages = excludedPackages;
        mPlatformServiceClient = platformServiceClient;
        mLogger = logger;
        mLogger.start();
    }

    /**
     * Obtains a screenshot by calling the platform service.
     * @return captured screenshot proto. {@code null} if screenshot was not captured.
     */
    @Nullable
    private Screenshot obtainScreenshot() {
        if (mPlatformServiceClient != null) {
            try {
               return mPlatformServiceClient.obtainScreenshot();
            } catch (SecurityException e) {
                // TODO(mukarram) Should we catch this or let the app crash?
                Log.e(TAG, "SecurityException while obtaining screenshot. " + e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "mPlatformServiceClient is not ready. " + e);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException: ", e);
            }
        }
        return null;
    }

    /**
     * Processes an {@link AccessibilityEvent}.
     *
     * @param event {@link AccessibilityEvent} to process
     */
    public void process(AccessibilityEvent event) {
        Screenshot screenshot = obtainScreenshot();
        if (excludeEvent(event)) {
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            publishWindowChanged(event, screenshot);
        } else {
            publishSingleEvent(event, screenshot);
        }
        mLastEventType = event.getEventType();
    }

    /**
     * Returns whether the {@link AccessibilityEvent} should be excluded.
     *
     * @param event the {@link AccessibilityEvent}
     * @return true if the event should be excluded
     */
    private boolean excludeEvent(AccessibilityEvent event) {
        if (mExcludedPackages.isExcluded(event.getPackageName().toString())) {
            Log.v(TAG, "Excluding package " + event.getPackageName());
            return true;
        }
        // Filter out text selections - see comment in publishWindowChanged().
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            return true;
        }
        // Note, we could also filter based on field name, e.g. 'isPassword' etc...
        return false;
    }

    /**
     * Creates and returns a {@link RecordedUpdate}.
     *
     * @param event the {@link AccessibilityEvent}
     * @param element {@UIElement} in which to fill the update.
     * @return the {@link RecordedUpdate} or null if the event type is unknown.
     */
    private RecordedUpdate createUpdate(AccessibilityEvent event, UIElement element) {
        // Get the {@code RecordedUpdate}'s type.
        Type eventType = eventTypeMapping.get(event.getEventType());

        if (eventType == null) {
            Log.v(TAG, "Not logging unknown event with type = " + event.getEventType());
            return null;
        }

        Log.v(TAG, "RecordedUpdate with " + event.getEventType() + " --> " + eventType);

        RecordedUpdate.Builder updateBuilder = RecordedUpdate.newBuilder()
                .setType(eventType)
                .setElement(element)
                .setVersion(VERSION);

        if (event.getPackageName() != null) {
            updateBuilder.setPackageName(event.getPackageName().toString());
        }

        return updateBuilder.build();
    }

    /**
     * Publishes an event that's not a change of window content. This is typically an interaction
     * with a given widget.
     *
     * @param event the {@link AccessibilityEvent} to publish
     * @param screenshot the {@link Screenshot} to go with this event.
     */
    private void publishSingleEvent(
            AccessibilityEvent event,
            @Nullable Screenshot screenshot) {
        AccessibilityNodeInfo node = event.getSource();
        if (node == null) {
            return;
        }
        RecordedUpdate update = createUpdate(event, createUIElement(node, screenshot, false));
        if (update != null) {
            publishUpdate(update);
        }
        node.recycle();
    }

    /**
     * Publishes an {@link AccessibilityEvent#TYPE_WINDOW_CONTENT_CHANGED}.
     *
     * @param event the {@link AccessibilityEvent} to publish
     * @param screenshot the {@link Screenshot} to go with this event.
     */
    private void publishWindowChanged(
            AccessibilityEvent event,
            @Nullable Screenshot screenshot) {
        AccessibilityNodeInfo node = event.getSource();
        if (node == null) {
            return;
        }
        AccessibilityNodeInfo parent = node;
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        UIElement newElement = createUIElement(parent, screenshot, true);
        // TODO(stlafon, mukarram) Verify that .equals() is a deep comparison.
        if (!newElement.equals(mLastElement)) {
            // We get window-changed events following on from a text changed event. We
            // don't want to process every one of these, so skip printing if the last
            // event was a text change. Note that we strip selection_change events -
            // sometimes we get these after a text change and sometimes we don't.
            if (mLastEventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                RecordedUpdate update = createUpdate(event, newElement);
                if (update != null) {
                    publishUpdate(update);
                }
            }
            mLastElement = newElement;
        }
        node.recycle();
    }

    /**
     * Publishes a {@link RecordedUpdate} to Clearcut.
     *
     * @param update the {@link RecordedUpdate} to publish
     */
    private void publishUpdate(RecordedUpdate update) {
        printUIElement("", update.getElement());

        // Publish to Clearcut.
        byte[] buffer = new byte[update.getSerializedSize()];
        CodedOutputStream outputStream = CodedOutputStream.newInstance(buffer);
        try {
            update.writeTo(outputStream);
            mLogger.logEvent(buffer);
            Log.v(TAG, "Wrote " + buffer.length + " bytes in Clearcut.");
        } catch (IOException e) {
            Log.e(TAG, "Failed to write update of type " + update.getType() + " : " + e);
        }
    }

    /**
     * Returns true if the content of the {@link AccessibilityNodeInfo} should
     * be elided. This is desirable for sensitive UIElements, such as passwords.
     *
     * Note: this function is mostly heuristics; there is no guarantee that this
     * removes all sensitive information.
     *
     * @param node the input {@link AccessibilityNodeInfo}
     * @return true if content should be elided.
     */
    @VisibleForTesting
    boolean shouldElideContent(AccessibilityNodeInfo node) {
        // for now, only thing we check for is whether the node is a password node.
        return node.isPassword();
    }

    /**
     * Creates a {@link UIElement} message from an {@link AccessibilityNodeInfo}. Creates child
     * nodes if {@code andChildren} is true. Note that many of the fields in
     * {@link AccessibilityNodeInfo} can be null, so check before we set them.
     *
     * @param node the input {@link AccessibilityNodeInfo}
     * @param screenshot the {@link Screenshot} to go with this element.
     * @param createChildren whether to create child nodes
     * @return the {@link UIElement}
     */
    @VisibleForTesting
    UIElement createUIElement(
            AccessibilityNodeInfo node,
            @Nullable Screenshot screenshot,
            boolean createChildren) {
        UIElement.Builder elementBuilder = UIElement.newBuilder();
        if (widgetTypeMap.containsKey(node.getClassName())) {
            elementBuilder.setClassType(widgetTypeMap.get(node.getClassName()));
        } else {
            elementBuilder.setClassName(node.getClassName().toString());
        }
        // Note, getViewIdResourceName() requires API level 18.
        if (node.getViewIdResourceName() != null) {
            elementBuilder.setResourceName(node.getViewIdResourceName());
        }
        if (node.getContentDescription() != null) {
            elementBuilder.setDescription(node.getContentDescription().toString());
        }

        final boolean elideContent = shouldElideContent(node);

        if (elideContent) {
            elementBuilder.setContentElided(true);
        }

        if (node.getText() != null && !elideContent) {
            elementBuilder.setContent(node.getText().toString());
        }

        if (screenshot != null) {
            elementBuilder.setScreenshot(screenshot);
        }

        Rect rect = new Rect();
        node.getBoundsInParent(rect);
        RecordedRect recordedRect = RecordedRect.newBuilder()
                .setBottom(rect.bottom)
                .setLeft(rect.left)
                .setRight(rect.right)
                .setTop(rect.top)
                .build();
        elementBuilder.setRect(recordedRect);
        if (createChildren) {
            int numChildren = node.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    // Currently, we set the screenshot only on the root
                    // UIElement object, hence passing null to the children.
                    elementBuilder.addChild(createUIElement(child, null, true));
                    child.recycle();
                } else {
                    // Create an empty element to represent the null child
                    elementBuilder.addChild(UIElement.newBuilder().build());
                }
            }
        }
        return elementBuilder.build();
    }

    /** Prints a {@link UIElement} in the logcat. */
    private void printUIElement(String indent, UIElement node) {
        String className = (node.getClassType() == WidgetType.CUSTOM
                ? node.getClassName() : "" + node.getClassType());
        Log.v(TAG, indent + className + " (" + node.getResourceName() + ") "
                + node.getDescription()
                // Do not print if content is null
                + (node.getContent() == null ? "" : " = " + node.getContent())
                // If content was elided print that it was elided so that
                // the reader is not confused.
                + (node.getContentElided() ? "<Content Elided>" : "")
                // If the node has screenshot, then print the size in bytes.
                + (node.hasScreenshot() ? "Screenshot size (bytes): " + node.getScreenshot().getSerializedSize() : ""));
        for (UIElement child : node.getChildList()) {
            printUIElement(indent + "  ", child);
        }
    }

    /**
     * Creates and returns a {@link Map} from a widget to its enum type.
     */
    private static Map<String, WidgetType> getWidgetTypeMap() {
        Map<String, WidgetType> map = new HashMap<String, WidgetType>();

        map.put("android.appwidget.AppWidgetHostView", WidgetType.APP_WIDGET_HOST_VIEW);
        map.put("android.view.View", WidgetType.VIEW);
        map.put("android.webkit.WebView", WidgetType.WEB_VIEW);
        map.put("android.widget.Button", WidgetType.BUTTON);
        map.put("android.widget.CheckBox", WidgetType.CHECK_BOX);
        map.put("android.widget.CheckedTextView", WidgetType.CHECKED_TEXT_VIEW);
        map.put("android.widget.EditText", WidgetType.EDIT_TEXT);
        map.put("android.widget.FrameLayout", WidgetType.FRAME_LAYOUT);
        map.put("android.widget.HorizontalScrollView",
                WidgetType.HORIZONTAL_SCROLL_VIEW);
        map.put("android.widget.ImageButton", WidgetType.IMAGE_BUTTON);
        map.put("android.widget.ImageView", WidgetType.IMAGE_VIEW);
        map.put("android.widget.LinearLayout", WidgetType.LINEAR_LAYOUT);
        map.put("android.widget.ListView", WidgetType.LIST_VIEW);
        map.put("android.widget.MultiAutoCompleteTextView",
                WidgetType.MULTI_AUTO_COMPLETE_TEXT_VIEW);
        map.put("android.widget.ProgressBar", WidgetType.PROGRESS_BAR);
        map.put("android.widget.RelativeLayout", WidgetType.RELATIVE_LAYOUT);
        map.put("android.widget.ScrollView", WidgetType.SCROLL_VIEW);
        map.put("android.widget.Spinner", WidgetType.SPINNER);
        map.put("android.widget.Switch", WidgetType.SWITCH);
        map.put("android.widget.TabHost", WidgetType.TAB_HOST);
        map.put("android.widget.TabWidget", WidgetType.TAB_WIDGET);
        map.put("android.widget.TextView", WidgetType.TEXT_VIEW);
        map.put("android.widget.ViewSwitcher", WidgetType.VIEW_SWITCHER);

        return map;
    }

    /**
     * Creates and returns a {@link SparseArray} from {@link AccessibilityEvent}'s event types to
     * {@link RecordedUpdate}'s {@link Type}.
     */
    private static SparseArray<Type> getEventTypeConversionArray() {
        SparseArray<Type> sparseArray = new SparseArray<Type>();

        sparseArray.put(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED,
                Type.TYPE_NOTIFICATION_STATE_CHANGED);
        sparseArray.put(AccessibilityEvent.TYPE_VIEW_CLICKED, Type.TYPE_VIEW_CLICKED);
        sparseArray.put(AccessibilityEvent.TYPE_VIEW_FOCUSED, Type.TYPE_VIEW_FOCUSED);
        sparseArray.put(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, Type.TYPE_VIEW_LONG_CLICKED);
        sparseArray.put(AccessibilityEvent.TYPE_VIEW_SCROLLED, Type.TYPE_VIEW_SCROLLED);
        sparseArray.put(AccessibilityEvent.TYPE_VIEW_SELECTED, Type.TYPE_VIEW_SELECTED);
        sparseArray.put(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, Type.TYPE_VIEW_TEXT_CHANGED);
        sparseArray.put(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
                Type.TYPE_VIEW_TEXT_SELECTION_CHANGED);
        sparseArray.put(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                Type.TYPE_WINDOW_CONTENT_CHANGED);

        return sparseArray;
    }

}

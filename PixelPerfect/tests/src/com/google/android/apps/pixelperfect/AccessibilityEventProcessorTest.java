package com.google.android.apps.pixelperfect;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.android.apps.pixelperfect.AccessibilityEventProcessor.ClearcutLogger;
import com.google.common.logging.RecordedEvent;
import com.google.common.logging.RecordedEvent.RecordedTypes.WidgetType;
import com.google.common.logging.RecordedEvent.UIElement;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

/**
 * Tests for {@link AccessibilityEventProcessor}.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@SmallTest
public class AccessibilityEventProcessorTest extends AndroidTestCase {

    private static final String VIEW_ID_RESOURCE_NAME_1 = "view 1";
    private static final String TEXT_1 = "text 1";
    private static final String TEXT_2 = "text 2";
    private static final String TEXT_3 = "text 3";
    private static final String WIDGET_CLASS_NAME_1 = "android.widget.CheckedTextView";
    private static final String WIDGET_CLASS_NAME_2 = "android.widget.LinearLayout";
    private static final String WIDGET_CLASS_NAME_3 = "android.widget.EditText";
    private static final String WIDGET_CLASS_NAME_4 = "android.widget.EditText";

    private static final Rect RECT = new Rect(110, 120, 130, 140);
    private static final Rect RECT_B = new Rect(210, 220, 230, 240);
    private static final Rect RECT_C = new Rect(310, 320, 330, 340);

    @Mock private AccessibilityNodeInfo mNode;
    @Mock private AccessibilityNodeInfo mSubNodeA;
    @Mock private AccessibilityNodeInfo mSubNodeB;
    @Mock private AccessibilityNodeInfo mSubNodeC;

    @Mock private ExcludedPackages mExcludedPackages;
    @Mock private ClearcutLogger mLogger;

    private AccessibilityEventProcessor mProcessor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // The following is needed to make mockito work.
        // see https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().toString());
        MockitoAnnotations.initMocks(this);
        mProcessor = new AccessibilityEventProcessor(mExcludedPackages, mLogger);
    }

    public void testCreateUIElement() {
        // Stubbing for the top node.
        when(mNode.getClassName()).thenReturn(WIDGET_CLASS_NAME_1);
        when(mNode.getViewIdResourceName()).thenReturn(VIEW_ID_RESOURCE_NAME_1);
        when(mNode.getText()).thenReturn(TEXT_1);
        when(mNode.getChildCount()).thenReturn(3);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                rect.set(RECT);
                return null;
            }
        }).when(mNode).getBoundsInParent(isA(Rect.class));

        // Stubbing for subnode A.
        when(mSubNodeA.getClassName()).thenReturn(WIDGET_CLASS_NAME_2);
        when(mSubNodeA.getViewIdResourceName()).thenReturn(null);
        when(mSubNodeA.getText()).thenReturn(TEXT_2);
        when(mSubNodeA.getChildCount()).thenReturn(0);
        when(mNode.getChild(0)).thenReturn(mSubNodeA);

        // Stubbing for subnode B.
        when(mSubNodeB.getClassName()).thenReturn(WIDGET_CLASS_NAME_3);
        when(mSubNodeB.getViewIdResourceName()).thenReturn(null);
        when(mSubNodeB.getText()).thenReturn(null);
        when(mSubNodeB.getChildCount()).thenReturn(0);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                rect.set(RECT_B);
                return null;
            }
        }).when(mSubNodeB).getBoundsInParent(isA(Rect.class));
        when(mNode.getChild(1)).thenReturn(mSubNodeB);

        // Stubbing for subnode C, which in this case is a password node.
        when(mSubNodeC.getClassName()).thenReturn(WIDGET_CLASS_NAME_4);
        when(mSubNodeC.getViewIdResourceName()).thenReturn(null);
        when(mSubNodeC.getText()).thenReturn(TEXT_3);
        when(mSubNodeC.isPassword()).thenReturn(true);
        when(mSubNodeC.getChildCount()).thenReturn(0);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                rect.set(RECT_C);
                return null;
            }
        }).when(mSubNodeC).getBoundsInParent(isA(Rect.class));
        when(mNode.getChild(2)).thenReturn(mSubNodeC);

        // Run the test.
        UIElement element = mProcessor.createUIElement(mNode, true);

        checkElement(element, WidgetType.CHECKED_TEXT_VIEW, VIEW_ID_RESOURCE_NAME_1, TEXT_1, false, RECT);

        List<UIElement> childList = element.getChildList();
        assertEquals(3, childList.size());
        checkElement(childList.get(0), WidgetType.LINEAR_LAYOUT, null, TEXT_2, false, new Rect());
        checkElement(childList.get(1), WidgetType.EDIT_TEXT, null, null, false, RECT_B);
        // For the third child, we expect content to be elided.
        checkElement(childList.get(2), WidgetType.EDIT_TEXT, null, null, true, RECT_C);
    }

    public void testShouldElideContent() {
        when(mNode.isPassword()).thenReturn(true);
        assertTrue(mProcessor.shouldElideContent(mNode));

        when(mNode.isPassword()).thenReturn(false);
        assertFalse(mProcessor.shouldElideContent(mNode));
    }

    private void checkElement(UIElement element, WidgetType classType, String resourceName,
            String content, boolean contentElided, Rect rect) {
        assertEquals(classType, element.getClassType());
        if (resourceName != null) {
            assertEquals(resourceName, element.getResourceName());
        } else {
            assertFalse(element.hasResourceName());
        }
        if (content != null) {
            assertEquals(content, element.getContent());
        } else {
            assertFalse(element.hasContent());
        }
        assertEquals(contentElided, element.getContentElided());
        checkRect(rect, element.getRect());
    }

    private void checkRect(Rect rect, RecordedEvent.RecordedRect recordedRect) {
        assertEquals(rect.left, recordedRect.getLeft());
        assertEquals(rect.top, recordedRect.getTop());
        assertEquals(rect.right, recordedRect.getRight());
        assertEquals(rect.bottom, recordedRect.getBottom());
    }

}

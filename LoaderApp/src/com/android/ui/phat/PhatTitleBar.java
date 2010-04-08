/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ui.phat;

import com.android.loaderapp.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PhatTitleBar extends ViewGroup {
    // TODO: This must be defined in the default theme
    private static final int CONTENT_HEIGHT_DIP = 50;
    private static final int CONTENT_PADDING_DIP = 3;
    private static final int CONTENT_SPACING_DIP = 6;
    private static final int CONTENT_ACTION_SPACING_DIP = 12;

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_DROP_DOWN = 1;
    public static final int TYPE_TABS = 2;
    
    private final int mContentHeight;

    private int mType;
    private int mSpacing;
    private int mActionSpacing;
    private CharSequence mTitle;
    private Drawable mIcon;

    private ImageView mIconView;
    private TextView mTitleView;
    
    private final ArrayList<ActionView> mActions = new ArrayList<ActionView>();
    private final OnClickListener mActionClickHandler = new OnClickListener() {
        public void onClick(View v) {
            ((ActionView) v).fireActionListener();
        }
    };

    // TODO: Define the background drawable in the default theme
    public PhatTitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mContentHeight = (int) (CONTENT_HEIGHT_DIP * metrics.density + 0.5f);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PhatTitleBar);

        final int colorFilter = a.getColor(R.styleable.PhatTitleBar_colorFilter, 0);
        if (colorFilter != 0) {
            final Drawable d = getBackground();
            d.setDither(true);
            d.setColorFilter(new PorterDuffColorFilter(colorFilter, PorterDuff.Mode.OVERLAY));
        }

        mType = a.getInt(R.styleable.PhatTitleBar_titleBarType, TYPE_NORMAL);
        mTitle = a.getText(R.styleable.PhatTitleBar_title);
        mIcon = a.getDrawable(R.styleable.PhatTitleBar_icon);

        a.recycle();

        // TODO: Set this in the theme
        int padding = (int) (CONTENT_PADDING_DIP * metrics.density + 0.5f);
        setPadding(padding, padding, padding, padding);

        mSpacing = (int) (CONTENT_SPACING_DIP * metrics.density + 0.5f);
        mActionSpacing = (int) (CONTENT_ACTION_SPACING_DIP * metrics.density + 0.5f);
    }

    public void addAction(int id, Drawable icon, CharSequence label, OnActionListener listener) {
        ActionView actionView = new ActionView(getContext());
        actionView.actionId = id;
        actionView.actionLabel = label;
        actionView.actionListener = listener;
        actionView.setAdjustViewBounds(true);
        actionView.setImageDrawable(icon);
        actionView.setOnClickListener(mActionClickHandler);

        actionView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT, LayoutParams.ITEM_TYPE_ACTION));

        addView(actionView);
        mActions.add(actionView);
        
        requestLayout();
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (mIcon != null) {
            mIconView = new ImageView(getContext());
            mIconView.setAdjustViewBounds(true);
            mIconView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT, LayoutParams.ITEM_TYPE_ICON));
            mIconView.setImageDrawable(mIcon);
            addView(mIconView);
        }

        switch (mType) {
            case TYPE_NORMAL:
                if (mTitle != null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    mTitleView = (TextView) inflater.inflate(R.layout.phat_title_bar_title_item, null);
                    mTitleView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT, LayoutParams.ITEM_TYPE_TITLE));
                    mTitleView.setText(mTitle);
                    addView(mTitleView);
                }
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_width=\"match_parent\" (or fill_parent)");
        }
        
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.AT_MOST) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }

        int contentWidth = MeasureSpec.getSize(widthMeasureSpec);
        
        int availableWidth = contentWidth - getPaddingLeft() - getPaddingRight();
        int childSpecHeight = MeasureSpec.makeMeasureSpec(mContentHeight - getPaddingTop() -
                getPaddingBottom(), MeasureSpec.AT_MOST);

        if (mIconView != null) {
            availableWidth = measureChildView(mIconView, availableWidth, childSpecHeight, mSpacing);
        }

        switch (mType) {
            case TYPE_NORMAL:
                if (mTitleView != null) {
                    availableWidth = measureChildView(mTitleView, availableWidth,
                            childSpecHeight, mSpacing);
                }
                break;
        }

        for (ActionView action : mActions) {
            availableWidth = measureChildView(action, availableWidth,
                    childSpecHeight, mActionSpacing);
        }

        setMeasuredDimension(contentWidth, mContentHeight);
    }

    private int measureChildView(View child, int availableWidth, int childSpecHeight, int spacing) {
        measureChild(child,
                MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                childSpecHeight);

        availableWidth -= child.getMeasuredWidth();
        availableWidth -= spacing;

        return availableWidth;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int x = getPaddingLeft();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();

        if (mIconView != null) {
            x += positionChild(mIconView, x, y, contentHeight) + mSpacing;
        }
        
        switch (mType) {
            case TYPE_NORMAL:
                if (mTitleView != null) {
                    x += positionChild(mTitleView, x, y, contentHeight) + mSpacing;
                }
                break;
        }

        x = r - l - getPaddingRight();

        final int count = mActions.size();
        for (int i = count - 1; i >= 0; i--) {
            ActionView action = mActions.get(i);
            x -= (positionChildInverse(action, x, y, contentHeight) + mActionSpacing);
        }
    }

    private int positionChild(View child, int x, int y, int contentHeight) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        child.layout(x, childTop, x + childWidth, childTop + childHeight);

        return childWidth;
    }
    
    private int positionChildInverse(View child, int x, int y, int contentHeight) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        child.layout(x - childWidth, childTop, x, childTop + childHeight);

        return childWidth;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof LayoutParams;
    }

    private static class LayoutParams extends ViewGroup.LayoutParams {
        static final int ITEM_TYPE_UNKNOWN = -1;
        static final int ITEM_TYPE_ICON = 0;
        static final int ITEM_TYPE_TITLE = 1;
        static final int ITEM_TYPE_COMPLEX = 2;
        static final int ITEM_TYPE_ACTION = 3;
        static final int ITEM_TYPE_MORE = 4;

        int type = ITEM_TYPE_UNKNOWN;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }
        
        public LayoutParams(int width, int height, int type) {
            this(width, height);
            this.type = type;
        }
        

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public interface OnActionListener {
        void onAction(int id);
    }
    
    private static class ActionView extends ImageView {
        int actionId;
        CharSequence actionLabel;
        OnActionListener actionListener;

        public ActionView(Context context) {
            super(context);
        }

        void fireActionListener() {
            actionListener.onAction(actionId);
        }
    }
}

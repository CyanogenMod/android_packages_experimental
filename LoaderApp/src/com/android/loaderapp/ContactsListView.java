/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.loaderapp;

import com.android.loaderapp.model.ContactsListLoader.ListQuery;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ContactsListView extends ListView {
    ContactsListViewFactory mViewFactory;

    public ContactsListView(Context context) {
        this(context, null);
    }

    public ContactsListView(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.listViewStyle);
    }

    public ContactsListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setAdapter(new Adapter(context));
    }

    /**
     * Build the {@link Contacts#CONTENT_LOOKUP_URI} for the given
     * {@link ListView} position, using {@link #mAdapter}.
     */
    public Uri getContactUri(int position) {
        if (position == ListView.INVALID_POSITION) {
            throw new IllegalArgumentException("Position not in list bounds");
        }

        final Cursor cursor = (Cursor) getAdapter().getItem(position);
        if (cursor == null) {
            return null;
        }

        // Build and return soft, lookup reference
        final long contactId = cursor.getLong(ListQuery.COLUMN_ID);
        final String lookupKey = cursor.getString(ListQuery.COLUMN_LOOKUP_KEY);
        return Contacts.getLookupUri(contactId, lookupKey);
    }

    
    public interface ContactsListViewFactory {
        public View newView(Context context, ViewGroup parent);
        public void bindView(View view, Context context, Cursor cursor);
    }

    public void setViewFactory(ContactsListViewFactory factory) {
        mViewFactory = factory;
    }

    /** Sets the cursor that the list displays */
    public void setCursor(Cursor cursor) {
        Adapter adapter = (Adapter) getAdapter();
        adapter.changeCursor(cursor);
    }

    /**
     * A simple view factory that inflates the views from XML and puts the display
     * name in @id/name.
     */
    public static class SimpleViewFactory implements ContactsListViewFactory {
        private int mResId;

        public SimpleViewFactory(int resId) {
            mResId = resId;
        }

        public View newView(Context context, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(mResId, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(cursor.getString(ListQuery.COLUMN_NAME));
        }
    }
    
    private class Adapter extends CursorAdapter {
        public Adapter(Context context) {
            super(context, null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mViewFactory.newView(context, parent);
            mViewFactory.bindView(view, context, cursor); 
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            mViewFactory.bindView(view, context, cursor);
        }
    }
}

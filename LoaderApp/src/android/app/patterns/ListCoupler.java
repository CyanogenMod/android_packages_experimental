/*
 * Copyright (C) 2010 Google Inc.
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
 * limitations under the License
 */

package android.app.patterns;

import com.android.loaderapp.R;
import com.android.loaderapp.R.id;
import com.android.loaderapp.model.ContactsListLoader;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListCoupler {
    Context mContext;
    ListView mList;
    ListCouplerViewFactory mViewFactory;
    Adapter mAdapter;

    public ListCoupler(Context context, ListView list) {
        mContext = context;
        mList = list;
        mAdapter = new Adapter(context);
        mList.setAdapter(mAdapter);
    }

    public interface ListCouplerViewFactory {
        public View newView(Context context, ViewGroup parent);
        public void bindView(View view, Context context, Cursor cursor);
    }

    public void setViewFactory(ListCouplerViewFactory factory) {
        mViewFactory = factory;
    }

    /** Sets the cursor that the list displays */
    public void setData(Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    protected ListView getList() {
        return mList;
    }

    protected ListAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * A simple view factory that inflates the views from XML and puts the display
     * name in @id/name.
     */
    public static class ResourceViewFactory implements ListCouplerViewFactory {
        private int mResId;

        public ResourceViewFactory(int resId) {
            mResId = resId;
        }

        public View newView(Context context, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(mResId, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(cursor.getString(ContactsListLoader.COLUMN_NAME));
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

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

import com.android.loaderapp.model.ContactsListLoader;

import android.app.patterns.ListCoupler;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ContactsListCoupler extends ListCoupler implements OnItemClickListener {

    Controller mController;

    public interface Controller {
        public void onContactSelected(Uri contact);
    }

    public ContactsListCoupler(Context context, ListView list) {
        super(context, list);
        list.setOnItemClickListener(this);
    }

    public void setController(Controller controller) {
        mController = controller;
    }

    /**
     * Build the {@link Contacts#CONTENT_LOOKUP_URI} for the given
     * {@link ListView} position.
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
        final long contactId = cursor.getLong(ContactsListLoader.COLUMN_ID);
        final String lookupKey = cursor.getString(ContactsListLoader.COLUMN_LOOKUP_KEY);
        return Contacts.getLookupUri(contactId, lookupKey);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // The user clicked on an item in the left side pane, start loading the data for it
        if (mController != null) {
            mController.onContactSelected(getContactUri(position));
        }
    }
}

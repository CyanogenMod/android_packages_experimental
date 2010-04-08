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

import com.android.loaderapp.ContactsListView.SimpleViewFactory;
import com.android.loaderapp.model.ContactLoader;
import com.android.loaderapp.model.VisibleContactsLoader;
import com.android.loaderapp.model.ContactLoader.ContactData;
import com.android.ui.phat.PhatTitleBar;
import com.android.ui.phat.PhatTitleBar.OnActionListener;

import android.app.patterns.Loader;
import android.app.patterns.LoaderActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class HomeLarge extends LoaderActivity implements OnItemClickListener, OnActionListener {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    private static final int LOADER_LIST = 1;
    private static final int LOADER_DETAILS = 2;

    ContactsListView mList;
    ContactDetailsView mDetails;
    VisibleContactsLoader mListLoader;
    ContactLoader mDetailsLoader;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.large_home);

        mList = (ContactsListView) findViewById(android.R.id.list);
        mList.setViewFactory(new SimpleViewFactory(R.layout.large_list_item));
        mList.setOnItemClickListener(this);

        mDetails = (ContactDetailsView) findViewById(R.id.contact_details);
        mDetails.setCallbacks(new ContactDetailsView.DefaultCallbacks(this));

        final PhatTitleBar titleBar = (PhatTitleBar) findViewById(R.id.title_bar);
        final Resources resources = getResources();

        titleBar.addAction(ACTION_ID_SEARCH, resources.getDrawable(android.R.drawable.ic_menu_search),
                "Search", this);
        titleBar.addAction(ACTION_ID_ADD, resources.getDrawable(android.R.drawable.ic_menu_add),
                "Add", this);
    }

    public void onAction(int id) {
        switch (id) {
            case ACTION_ID_SEARCH:
                startSearch(null, false, null, true);
                break;

            case ACTION_ID_ADD:
                startActivity(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI));
                break;
        }
    }

    @Override
    public void onInitializeLoaders() {
        startLoading(LOADER_LIST, null);
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LIST:
                return new VisibleContactsLoader(this);

            case LOADER_DETAILS: {
                Uri uri = args.getParcelable("uri");
                return new ContactLoader(this, uri);
            }
        }
        return null;
    }

    @Override
    public void onLoadComplete(int id, Object data) {
        switch (id) {
            case LOADER_LIST:
                mList.setCursor((Cursor) data);
                break;

            case LOADER_DETAILS:
                mDetails.setData((ContactData) data);
                break;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // The user clicked on an item in the left side pane, start loading the data for it
        Bundle args = new Bundle();
        args.putParcelable("uri", mList.getContactUri(position));
        startLoading(LOADER_DETAILS, args);
    }
}

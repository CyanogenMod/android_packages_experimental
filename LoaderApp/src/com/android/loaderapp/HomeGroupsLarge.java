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

package com.android.loaderapp;

import com.android.loaderapp.ContactsListView.SimpleViewFactory;
import com.android.loaderapp.model.StrequentLoader;
import com.android.loaderapp.model.VisibleContactsLoader;
import com.android.ui.phat.PhatTitleBar;
import com.android.ui.phat.PhatTitleBar.OnActionListener;

import android.app.patterns.CursorLoader;
import android.app.patterns.Loader;
import android.app.patterns.LoaderActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class HomeGroupsLarge extends LoaderActivity<Cursor> implements OnItemClickListener, OnActionListener {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    private static final int LOADER_LIST = 1;

    ListView mGroups;
    ContactsListView mContactsList;
    CursorLoader mLoader;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.groups_home);

        mGroups = (ListView) findViewById(R.id.groups);
        MatrixCursor groupsList = new MatrixCursor(new String[] { "_id", "name" });
        groupsList.newRow().add(1).add("All Contacts");
        groupsList.newRow().add(2).add("Favorites");
        mGroups.setAdapter(new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1,
                groupsList, new String[] { "name" }, new int[] { android.R.id.text1 }));
        mGroups.setOnItemClickListener(this);

        mContactsList = (ContactsListView) findViewById(android.R.id.list);
        mContactsList.setViewFactory(new SimpleViewFactory(R.layout.normal_list_item));
        mContactsList.setOnItemClickListener(this);

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
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LIST: {
                boolean strequent = args.getBoolean("strequent");
                if (strequent) {
                    return new StrequentLoader(this);
                } else {
                    return new VisibleContactsLoader(this);
                }
            }
        }
        return null;
    }

    @Override
    public void onLoadComplete(Loader loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_LIST:
                mContactsList.setCursor(data);
                break;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mGroups) {
            Bundle args = new Bundle();
            if (position == 0) {
                args.putBoolean("strequent", false);
            } else if (position == 1) {
                args.putBoolean("strequent", true);
            }
            startLoading(LOADER_LIST, args);
        } else {
            // The user clicked on an item in the the list, start an activity to view it
            Uri contactUri = mContactsList.getContactUri(position);
            if (contactUri != null) {
                Intent intent = new Intent(this, DetailsNormal.class);
                intent.setData(contactUri);
                startActivity(intent);
            }
        }
    }
}

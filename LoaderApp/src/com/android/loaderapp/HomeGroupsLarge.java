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

import com.android.loaderapp.model.ContactsListLoader;
import com.android.ui.phat.PhatTitleBar;
import com.android.ui.phat.PhatTitleBar.OnActionListener;

import android.app.patterns.CursorLoader;
import android.app.patterns.ListCoupler;
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

public class HomeGroupsLarge extends LoaderActivity<Cursor> implements OnActionListener,
        ContactsListCoupler.Controller, GroupsListCoupler.Controller {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    private static final int LOADER_GROUPS = 0;
    private static final int LOADER_LIST = 1;

    private static final int LIST_MODE_ALL_CONTACTS = 1;
    private static final int LIST_MODE_FAVORITES = 2;
    private static final int LIST_MODE_GROUP = 3;

    private static final String ARG_MODE = "mode";
    private static final String ARG_GROUP = "group";

    ContactsListCoupler mContactsCoupler;
    GroupsListCoupler mGroupsCoupler;
    CursorLoader mLoader;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.groups_home);

        mGroupsCoupler = new GroupsListCoupler(this, (ListView) findViewById(R.id.groups));
        mGroupsCoupler.setController(this);

        mContactsCoupler = new ContactsListCoupler(this,
                (ListView) findViewById(android.R.id.list));
        mContactsCoupler.setViewFactory(
                new ListCoupler.ResourceViewFactory(R.layout.normal_list_item));
        mContactsCoupler.setController(this);

        final PhatTitleBar titleBar = (PhatTitleBar) findViewById(R.id.title_bar);
        final Resources resources = getResources();

        titleBar.addAction(ACTION_ID_SEARCH,
                resources.getDrawable(android.R.drawable.ic_menu_search), "Search", this);
        titleBar.addAction(ACTION_ID_ADD,
                resources.getDrawable(android.R.drawable.ic_menu_add), "Add", this);
    }

    public void onAction(int id) {
        switch (id) {
            case ACTION_ID_SEARCH: {
                startSearch(null, false, null, true);
                break;
            }

            case ACTION_ID_ADD: {
                startActivity(new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI));
                break;
            }
        }
    }

    @Override
    public void onInitializeLoaders() {
        startLoading(LOADER_GROUPS, null);
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_GROUPS: {
                return new GroupsListLoader(this);
            }

            case LOADER_LIST: {
                int mode = args.getInt(ARG_MODE);
                switch (mode) {
                    case LIST_MODE_FAVORITES: {
                        return ContactsListLoader.newStrequentContactsLoader(this);
                    }

                    case LIST_MODE_ALL_CONTACTS: {
                        return ContactsListLoader.newVisibleContactsLoader(this);
                    }

                    case LIST_MODE_GROUP: {
                        String group = args.getString(ARG_GROUP);
                        return ContactsListLoader.newContactGroupLoader(this, group);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_GROUPS: {
                mGroupsCoupler.setData(data);
                break;
            }

            case LOADER_LIST: {
                mContactsCoupler.setData(data);
                break;
            }
        }
    }

    public void onAllContactsSelected() {
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, LIST_MODE_ALL_CONTACTS);
        startLoading(LOADER_LIST, args);
    }

    public void onFavoritesSelected() {
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, LIST_MODE_FAVORITES);
        startLoading(LOADER_LIST, args);
    }

    public void onGroupSelected(String title) {
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, LIST_MODE_GROUP);
        args.putString(ARG_GROUP, title);
        startLoading(LOADER_LIST, args);
    }

    public void onContactSelected(Uri contactUri) {
        if (contactUri != null) {
            Intent intent = new Intent(this, DetailsNormal.class);
            intent.setData(contactUri);
            startActivity(intent);
        }
    }
}

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

import com.android.loaderapp.model.ContactLoader;
import com.android.loaderapp.model.ContactsListLoader;
import com.android.loaderapp.model.ContactLoader.ContactData;
import com.android.ui.phat.PhatTitleBar;
import com.android.ui.phat.PhatTitleBar.OnActionListener;

import android.app.patterns.ListCoupler;
import android.app.patterns.Loader;
import android.app.patterns.LoaderActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ListView;

public class HomeLarge extends LoaderActivity implements ContactsListCoupler.Controller, OnActionListener {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    private static final int LOADER_LIST = 1;
    private static final int LOADER_DETAILS = 2;

    private static final String ARG_URI = "uri";

    ContactsListCoupler mListCoupler;
    ContactCoupler mDetails;
    ContactsListLoader mListLoader;
    ContactLoader mDetailsLoader;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.large_home);

        ListView list = (ListView) findViewById(android.R.id.list);
        mListCoupler = new ContactsListCoupler(this, list);
        mListCoupler.setViewFactory(new ListCoupler.ResourceViewFactory(R.layout.large_list_item));
        mListCoupler.setController(this);

        mDetails = new ContactCoupler(this, findViewById(R.id.contact_details));
        mDetails.setController(new ContactCoupler.DefaultController(this));

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
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_URI, intent.getData());
            startLoading(LOADER_DETAILS, args);
        }
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LIST: {
                return ContactsListLoader.newVisibleContactsLoader(this);
            }

            case LOADER_DETAILS: {
                Uri uri = args.getParcelable(ARG_URI);
                return new ContactLoader(this, uri);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_LIST: {
                mListCoupler.setData((Cursor) data);
                break;
            }

            case LOADER_DETAILS: {
                mDetails.setData((ContactData) data);
                break;
            }
        }
    }

    public void onContactSelected(Uri contactUri) {
        // The user clicked on an item in the left side pane, start loading the data for it
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, contactUri);
        startLoading(LOADER_DETAILS, args);
    }
}

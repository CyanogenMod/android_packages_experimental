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

import com.android.loaderapp.fragments.ContactFragment;
import com.android.loaderapp.fragments.ContactFragment;
import com.android.loaderapp.fragments.ContactsListFragment;
import com.android.ui.phat.PhatTitleBar;
import com.android.ui.phat.PhatTitleBar.OnActionListener;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

public class HomeXLarge extends Activity implements ContactsListFragment.Controller, OnActionListener {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    ContactsListFragment mList;
    ContactFragment mDetails;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.home_xlarge);

        mList = new ContactsListFragment();
        mList.setController(this);
        mDetails = new ContactFragment(null, new ContactFragment.DefaultController(this));
        FragmentTransaction transaction = openFragmentTransaction();
        transaction.add(mList, R.id.contacts_list);
        transaction.add(mDetails, R.id.contact_details);
        transaction.commit();

        final PhatTitleBar titleBar = (PhatTitleBar) findViewById(R.id.title_bar);
        final Resources resources = getResources();

        titleBar.addAction(ACTION_ID_SEARCH, resources.getDrawable(android.R.drawable.ic_menu_search),
                "Search", this);
        titleBar.addAction(ACTION_ID_ADD, resources.getDrawable(android.R.drawable.ic_menu_add),
                "Add", this);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            mDetails.loadContact(intent.getData());
        }
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

    public void onContactSelected(Uri contactUri) {
        // The user clicked on an item in the left side pane, start loading the data for it
        mDetails.loadContact(contactUri);
    }
}

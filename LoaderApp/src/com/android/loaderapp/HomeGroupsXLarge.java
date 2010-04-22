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

import com.android.loaderapp.fragments.ContactsListFragment;
import com.android.loaderapp.fragments.GroupsListFragment;
import com.android.ui.phat.PhatTitleBar;
import com.android.ui.phat.PhatTitleBar.OnActionListener;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

public class HomeGroupsXLarge extends Activity implements OnActionListener,
        ContactsListFragment.Controller, GroupsListFragment.Controller {
    private static final int ACTION_ID_SEARCH = 0;
    private static final int ACTION_ID_ADD = 1;

    ContactsListFragment mContactsList;
    GroupsListFragment mGroupsList;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.groups_home);

        mGroupsList = new GroupsListFragment();
        mGroupsList.setController(this);

        mContactsList = new ContactsListFragment(ContactsListFragment.MODE_NULL);
        mContactsList.setController(this);
 
        FragmentTransaction xact = openFragmentTransaction();
        xact.add(mGroupsList, R.id.groups);
        xact.add(mContactsList, android.R.id.list);
        xact.commit();

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

    public void onAllContactsSelected() {
        mContactsList.setMode(ContactsListFragment.MODE_VISIBLE);
    }

    public void onFavoritesSelected() {
        mContactsList.setMode(ContactsListFragment.MODE_STREQUENT);
    }

    public void onGroupSelected(String title) {
        mContactsList.setGroupMode(title);
    }

    public void onContactSelected(Uri contactUri) {
        if (contactUri != null) {
            Intent intent = new Intent(this, DetailsNormal.class);
            intent.setData(contactUri);
            startActivity(intent);
        }
    }
}

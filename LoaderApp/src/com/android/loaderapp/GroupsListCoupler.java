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

import android.app.patterns.ListCoupler;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class GroupsListCoupler extends ListCoupler implements OnItemClickListener {
    private static final long GROUP_ID_ALL_CONTACTS = -1;
    private static final long GROUP_ID_FAVORITES = -2;

    Controller mController;

    public interface Controller {
        public void onAllContactsSelected();
        public void onFavoritesSelected();
        public void onGroupSelected(String name);
    }

    public GroupsListCoupler(Context context, ListView list) {
        super(context, list);
        list.setOnItemClickListener(this);
        setViewFactory(new ResourceViewFactory(R.layout.large_list_item));
    }

    public void setController(Controller controller) {
        mController = controller;
    }

    @Override
    public void setData(Cursor groups) {
        MatrixCursor psuedoGroups = new MatrixCursor(new String[] { "_id", "name" });
        psuedoGroups.newRow().add(-1).add("All Contacts");
        psuedoGroups.newRow().add(-2).add("Favorites");
        super.setData(new MergeCursor(new Cursor[] { psuedoGroups, groups }));
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (id == GROUP_ID_ALL_CONTACTS) {
            mController.onAllContactsSelected();
        } else if (id == GROUP_ID_FAVORITES) {
            mController.onFavoritesSelected();
        } else {
            Cursor cursor = (Cursor) getAdapter().getItem(position);
            mController.onGroupSelected(cursor.getString(GroupsListLoader.COLUMN_TITLE));
        }
    }
}
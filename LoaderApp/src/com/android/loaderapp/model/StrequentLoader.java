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

package com.android.loaderapp.model;

import com.android.loaderapp.model.VisibleContactsLoader.ListQuery;

import android.app.patterns.CursorLoader;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;

public class StrequentLoader extends CursorLoader {
    public StrequentLoader(Context context) {
        super(context);
    }

    @Override
    protected Cursor doQueryInBackground() {
        Cursor cursor = getContext().getContentResolver().query(Contacts.CONTENT_STREQUENT_URI,
                ListQuery.COLUMNS, null, null, null);
        return cursor;
    }
}

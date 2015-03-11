/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License.
 */

package foo.bar.permission2;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import foo.bar.permission.R;

/**
 * Simple sample of how to use the runtime permissions APIs.
 */
public class PermissionActivity extends Activity implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = "PermissionActivity";

    private static final int CONTACTS_LOADER = 1;

    private static final int EVENTS_LOADER = 2;

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    private static final int PERMISSIONS_REQUEST_READ_CALENDAR = 2;

    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 3;

    private final static String[] CONTACTS_COLUMNS = {
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    };

    private final static String[] CALENDAR_COLUMNS = {
            CalendarContract.Events.TITLE
    };

    private static final String[] CONTACTS_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    };

    private static final String[] EVENTS_PROJECTION = {
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE
    };

    private final static int[] TO_IDS = {
            android.R.id.text1
    };

    private ListView mListView;
    private Button mContactsButton;
    private Button mEventsButton;
    private Button mPermissionsButton;

    private CursorAdapter mContactsAdapter;
    private CursorAdapter mEventsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindUi();
    }

    @Override
    public void onClick(View view) {
        if (view == mContactsButton) {
            showContacts();
        } else if (view == mEventsButton) {
            showEvents();
        } else if (view == mPermissionsButton) {
            requestPermissions();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case CONTACTS_LOADER: {
                return new CursorLoader(PermissionActivity.this,
                        ContactsContract.Contacts.CONTENT_URI,
                        CONTACTS_PROJECTION, null, null, null);
            }

            case EVENTS_LOADER: {
                return new CursorLoader(PermissionActivity.this,
                        CalendarContract.Events.CONTENT_URI,
                        EVENTS_PROJECTION, null, null, null);
            }

            default: {
                return null;
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case CONTACTS_LOADER: {
                mContactsAdapter.swapCursor(cursor);
            } break;

            case EVENTS_LOADER: {
                mEventsAdapter.swapCursor(cursor);
            } break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case CONTACTS_LOADER: {
                mContactsAdapter.swapCursor(null);
            } break;

            case EVENTS_LOADER: {
                mEventsAdapter.swapCursor(null);
            } break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showContacts();
                }
            } break;

            case PERMISSIONS_REQUEST_READ_CALENDAR: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showEvents();
                }
            } break;
        }
    }

    private void bindUi() {
        setContentView(R.layout.permission_activity);

        mListView = (ListView) findViewById(R.id.list);

        mContactsButton = (Button) findViewById(R.id.show_contacts);
        mContactsButton.setOnClickListener(this);

        mEventsButton = (Button) findViewById(R.id.show_events);
        mEventsButton.setOnClickListener(this);

        mPermissionsButton = (Button) findViewById(R.id.request_permissions);
        mPermissionsButton.setOnClickListener(this);

        mContactsAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null, CONTACTS_COLUMNS, TO_IDS, 0);

        mEventsAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null, CALENDAR_COLUMNS, TO_IDS, 0);
    }

    private void showContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
            return;
        }

        if (getLoaderManager().getLoader(CONTACTS_LOADER) == null) {
            getLoaderManager().initLoader(CONTACTS_LOADER, null, this);
        }
        mListView.setAdapter(mContactsAdapter);
    }

    private void showEvents() {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSIONS_REQUEST_READ_CALENDAR);
            return;
        }

        if (getLoaderManager().getLoader(EVENTS_LOADER) == null) {
            getLoaderManager().initLoader(EVENTS_LOADER, null, this);
        }
        mListView.setAdapter(mEventsAdapter);
    }

    private void requestPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[] {
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_CALENDAR
            };
            requestPermissions(permissions, PERMISSIONS_REQUEST_ALL_PERMISSIONS);
        }
    }
}

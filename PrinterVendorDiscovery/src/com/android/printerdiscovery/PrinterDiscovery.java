/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.printerdiscovery;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;

/**
 * Test app for print service discovery.
 * <p/>
 * TODO: Remove
 */
public class PrinterDiscovery extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Adapter connecting the {@link R.layout#printer_discovery R.layout.printer_discovery} to the
     * {@link PrintServiceStubProvider}.
     */
    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.printer_discovery);

        mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null,
                new String[] { PrintServiceStubContract.PrintServiceStubs.NAME,
                        PrintServiceStubContract.PrintServiceStubs.NUM_DISCOVERED_PRINTERS
                }, new int[] { android.R.id.text1, android.R.id.text2 }, 0);

        ((ListView) findViewById(R.id.discovered_printers)).setAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                PrintServiceStubContract.PrintServiceStubs.CONTENT_URI,
                PrintServiceStubContract.PrintServiceStubs.ALL_COLUMNS, null, null,
                PrintServiceStubContract.PrintServiceStubs.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}

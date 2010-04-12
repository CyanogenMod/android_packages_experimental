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
import com.android.loaderapp.model.ContactLoader.ContactData;

import android.app.patterns.Loader;
import android.app.patterns.LoaderActivity;
import android.net.Uri;
import android.os.Bundle;

public class DetailsNormal extends LoaderActivity<ContactData> {
    static final int LOADER_DETAILS = 1;

    ContactDetailsView mDetails;
    Uri mUri;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.contact_details);
        mDetails = (ContactDetailsView) findViewById(R.id.contact_details);
        mDetails.setCallbacks(new ContactDetailsView.DefaultCallbacks(this));

        mUri = getIntent().getData();
    }

    @Override
    public void onInitializeLoaders() {
        Bundle args = new Bundle();
        args.putParcelable("uri", getIntent().getData());
        startLoading(LOADER_DETAILS, args);
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_DETAILS: {
                Uri uri = args.getParcelable("uri");
                return new ContactLoader(this, uri);
            }
        }
        return null;
    }

    @Override
    public void onLoadComplete(Loader loader, ContactData data) {
        switch (loader.getId()) {
            case LOADER_DETAILS:
                mDetails.setData(data);
                break;
        }
    }
}

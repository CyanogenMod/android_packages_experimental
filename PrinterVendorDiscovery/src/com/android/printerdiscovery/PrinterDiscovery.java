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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.printerdiscovery.plugins.gcp.GCPPlugin;
import com.android.printerdiscovery.plugins.mdnsFilter.MDNSFilterPlugin;
import com.android.printerdiscovery.plugins.mopria.MopriaPlugin;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class PrinterDiscovery extends Activity
        implements RemotePrinterDiscoveryPlugin.OnChangedListener {
    private ArrayList<RemotePrinterDiscoveryPlugin> mPlugins;
    private Handler mHandler;

    private static final int MSG_ON_CHANGED = 0;
    private DataSetObserver mListObserver;
    private final String LOG_TAG = "PrinterDiscovery";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.printer_discovery);

        mHandler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case MSG_ON_CHANGED:
                        mListObserver.onChanged();
                        break;
                    default:
                        throw new RuntimeException("Unexpected message " + inputMessage.what);
                }
            }
        };

        mPlugins = initPlugins();

        ListView discoveredPrinters = (ListView) findViewById(R.id.discovered_printers);
        discoveredPrinters.setAdapter(new ListAdapter() {
            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {
                mListObserver = observer;
            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {
                mListObserver = null;
            }

            @Override
            public int getCount() {
                return mPlugins.size();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2,
                            parent, false);
                }

                final RemotePrinterDiscoveryPlugin plugin = mPlugins.get(position);
                ((TextView) convertView.findViewById(android.R.id.text1))
                        .setText(plugin.getName());
                ((TextView) convertView.findViewById(android.R.id.text2))
                        .setText(plugin.getNumPrinters() + " printers found");

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        startActivity(plugin.getAction());
                    }
                });

                return convertView;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return mPlugins.isEmpty();
            }
        });
    }

    /**
     * Initialize all {@link PrinterDiscoveryPlugin printer discovery plugins}.
     *
     * @return The list of discovered plugins.
     */
    private @NonNull ArrayList<RemotePrinterDiscoveryPlugin> initPlugins() {
        // Read the plugins defined in {@link R.xml#vendorconfigs vendorconfigs.xml}
        Collection<VendorConfig> configs;
        try {
            configs = VendorConfig.getAllConfigs(this);
        } catch (IOException | XmlPullParserException e) {
            // {@link R.xml#vendorconfigs R.xml.vendorconfigs} is part of the file and parsed on
            // each start. There should never be any error in this file.
            throw new RuntimeException(e);
        }

        ArrayList<RemotePrinterDiscoveryPlugin> plugins = new ArrayList<>(configs.size());

        // Add the plugins defined in {@link R.xml#vendorconfigs vendorconfigs.xml}
        for (VendorConfig config : configs) {
            if (!config.getMDNSNames().isEmpty()) {
                try {
                    plugins.add(new RemotePrinterDiscoveryPlugin(
                            new MDNSFilterPlugin(getApplicationContext(), config.getName(),
                                    config.getPackageName(), config.getMDNSNames()), this));
                } catch (Throwable e) {
                    Log.e(LOG_TAG, "Could not create plugin for " + config, e);
                }
            }
        }

        try {
            plugins.add(
                    new RemotePrinterDiscoveryPlugin(new GCPPlugin(getApplicationContext()),
                            this));
        } catch (Throwable e) {
            Log.e(LOG_TAG, "Could not create GCP plugin", e);
        }

        try {
            plugins.add(
                    new RemotePrinterDiscoveryPlugin(new MopriaPlugin(getApplicationContext()),
                            this));
        } catch (Throwable e) {
            Log.e(LOG_TAG, "Could not create mopria plugin", e);
        }

        return plugins;
    }

    @Override
    protected void onStart() {
        super.onStart();

        final int numPlugins = mPlugins.size();
        for (int i = 0; i < numPlugins; i++) {
            RemotePrinterDiscoveryPlugin plugin = mPlugins.get(i);
            try {
                plugin.start();
            } catch (RemotePrinterDiscoveryPlugin.PluginException e) {
                Log.e(LOG_TAG, "Could not start " + plugin, e);

                // Remove plugin as it has issues
                mPlugins.remove(i);
                i--;
            }
        }
    }

    @Override
    protected void onStop() {
        final int numPlugins = mPlugins.size();
        for (int i = 0; i < numPlugins; i++) {
            RemotePrinterDiscoveryPlugin plugin = mPlugins.get(i);
            try {
                plugin.stop();
            } catch (RemotePrinterDiscoveryPlugin.PluginException e) {
                Log.e(LOG_TAG, "Could not stop " + plugin, e);

                // Remove plugin as it has issues
                mPlugins.remove(i);
                i--;
            }
        }

        super.onStop();
    }

    @Override public void onChanged(RemotePrinterDiscoveryPlugin plugin) {
        mHandler.obtainMessage(MSG_ON_CHANGED, plugin).sendToTarget();
    }
}

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

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.content.Intent;
import com.android.internal.util.Preconditions;

/**
 * Wrapper for a {@link PrinterDiscoveryPlugin}, isolating issues with the plugin as good as
 * possible from the user.
 */
class RemotePrinterDiscoveryPlugin implements PrinterDiscoveryPlugin.PrinterDiscoveryCallback {
    /**
     * Wrapped plugin
     */
    private final @NonNull PrinterDiscoveryPlugin mPlugin;

    /**
     * The name of the print plugin.
     */
    private final @NonNull String mName;

    /**
     * The action to perform when the plugin is selected by the user.
     */
    private final @NonNull Intent mAction;

    /**
     * The number of printers discovered by the plugin
     */
    private @IntRange(from = 0) int mNumPrinters;

    /**
     * If the plugin is started by not yet stopped
     */
    private boolean isRunning;

    /**
     * Listener for changes to {@link #mNumPrinters}.
     */
    private @NonNull OnChangedListener mListener;

    /**
     * Create a new RemotePrinterDiscoveryPlugin for a plugin.
     *
     * @param plugin the plugin to be wrapped
     *
     * @throws PluginException If the plugin has issues while caching basic plugin properties
     */
    public RemotePrinterDiscoveryPlugin(@NonNull PrinterDiscoveryPlugin plugin,
            @NonNull OnChangedListener listener) throws PluginException {
        mListener = listener;
        mPlugin = plugin;

        // We handle any throwable to isolate ourself from bugs in the plugin code.
        // Cache simple properties to avoid having to deal with exceptions later in the code.
        try {
            mName = Preconditions.checkNotNull(mPlugin.getName(), "name");
            mAction = Preconditions.checkNotNull(mPlugin.getAction(), "action");
        } catch (Throwable e) {
            throw new PluginException(mPlugin, "Cannot cache simple properties ", e);
        }

        isRunning = false;
    }

    /**
     * Get the name of the print plugin that is installed via the {@link #getAction}.
     *
     * @return The name of the print plugin.
     */
    public @NonNull String getName() {
        return mName;
    }

    /**
     * The action to perform when the plugin is selected by the user. Usually a link that will
     * install the print plugin.
     *
     * @return An action to perform.
     */
    public @NonNull Intent getAction() {
        return mAction;
    }

    /**
     * Stop the plugin. From now on there might be callbacks to the registered listener.
     */
    public void start()
            throws PluginException {
        // We handle any throwable to isolate ourself from bugs in the plugin code
        try {
            synchronized (this) {
                mPlugin.start(this);
                isRunning = true;
            }
        } catch (Throwable e) {
            throw new PluginException(mPlugin, "Cannot start", e);
        }
    }

    /**
     * Stop the plugin. From this call on there will not be any more callbacks.
     */
    public void stop() throws PluginException {
        // We handle any throwable to isolate our selfs from bugs in the plugin code
        try {
            synchronized (this) {
                mPlugin.stop();
                isRunning = false;
            }
        } catch (Throwable e) {
            throw new PluginException(mPlugin, "Cannot stop", e);
        }
    }

    /**
     * Get the current number of printers reported by the plugin.
     *
     * @return The number of printers reported by the plugin.
     */
    public @IntRange(from = 0) int getNumPrinters() {
        return mNumPrinters;
    }

    @Override
    public void onChanged(@IntRange(from = 0) int numDiscoveredPrinters) {
        synchronized (this) {
            Preconditions.checkState(isRunning);

            mNumPrinters = Preconditions.checkArgumentNonnegative(numDiscoveredPrinters,
                    "numDiscoveredPrinters");

            mListener.onChanged(this);
        }
    }

    /**
     * Listener to listen for changes to {@link #getNumPrinters}
     */
    public interface OnChangedListener {
        void onChanged(RemotePrinterDiscoveryPlugin plugin);
    }

    /**
     * Exception thrown if the plugin has any issues.
     */
    public class PluginException extends Exception {
        private PluginException(PrinterDiscoveryPlugin plugin, String message, Throwable e) {
            super(plugin + ": " + message, e);
        }
    }
}

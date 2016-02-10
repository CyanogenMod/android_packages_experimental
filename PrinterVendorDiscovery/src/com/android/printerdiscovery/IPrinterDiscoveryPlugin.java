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
import android.content.Context;
import android.content.Intent;

/**
 * Interface to be implemented by each printer discovery plugin.
 */
public interface IPrinterDiscoveryPlugin {
    interface PrinterDiscoveryCallback {
        /**
         * Announce that something changed and the UI for this plugin should be updated.
         *
         * @param numDiscoveredPrinters The number of printers discovered.
         */
        void onChanged(@IntRange(from = 0) int numDiscoveredPrinters);
    }

    /**
     * Get the name of the print plugin that is installed via the {@link #getAction}. This is read
     * once, hence returning different data at different times is not allowed.
     *
     * @return The name of the print plugin.
     */
    @NonNull String getName();

    /**
     * The action to perform when the plugin is selected by the user. Usually a link that will
     * install the print plugin. This is read once, hence returning different data at different
     * times is not allowed.
     *
     * @return An action to perform.
     */
    @NonNull Intent getAction();

    /**
     * Start the discovery plugin.
     *
     * @param callback Callbacks used by this plugin.
     *
     * @throws Exception If anything went wrong when starting the plugin
     */
    void start(@NonNull PrinterDiscoveryCallback callback) throws Exception;

    /**
     * Stop the discovery plugin. This can only return once the discovery plugin is completely
     * finished and cleaned up.
     *
     * @throws Exception If anything went wrong while stopping the plugin
     */
    void stop() throws Exception;
}

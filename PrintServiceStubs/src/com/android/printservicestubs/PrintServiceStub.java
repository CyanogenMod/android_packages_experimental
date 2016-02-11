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

package com.android.printservicestubs;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.StringRes;
import android.net.Uri;

/**
 * Interface to be implemented by each print service stub.
 * <p/>
 * A print service stub is a minimal version of a real {@link android.printservice.PrintService
 * print service}. You cannot print using the stub. The only functionality in the stub is to report
 * the number of printers that the real service would discover.
 */
public interface PrintServiceStub {
    /**
     * Call back used by the print service stubs.
     */
    interface PrinterDiscoveryCallback {
        /**
         * Announce that something changed and the UI for this stub should be updated.
         *
         * @param numDiscoveredPrinters The number of printers discovered.
         */
        void onChanged(@IntRange(from = 0) int numDiscoveredPrinters);
    }

    /**
     * Get the name (a string reference) of the {@link android.printservice.PrintService print
     * service} that will be installed via the {@link #getInstallUri}. This is read once, hence
     * returning different data at different times is not allowed.
     *
     * @return The name of the print service.
     */
    @StringRes int getName();

    /**
     * Uri to view when the stub is selected by the user. Usually a link that will install the print
     * service. This is read once, hence returning different data at different times is not
     * allowed.
     *
     * @return An Uri to view
     */
    @NonNull Uri getInstallUri();

    /**
     * Start the discovery stub.
     *
     * @param callback Callbacks used by this stub.
     *
     * @throws Exception If anything went wrong when starting the stub
     */
    void start(@NonNull PrinterDiscoveryCallback callback) throws Exception;

    /**
     * Stop the stub. This can only return once the stub is completely finished and cleaned up.
     *
     * @throws Exception If anything went wrong while stopping stub plugin
     */
    void stop() throws Exception;
}

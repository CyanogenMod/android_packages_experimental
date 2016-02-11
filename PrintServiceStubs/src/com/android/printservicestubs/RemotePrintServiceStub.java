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
import com.android.internal.util.Preconditions;

/**
 * Wrapper for a {@link PrintServiceStub}, isolating issues with the plugin as good as possible from
 * {@link PrintServiceStubProvider provider}.
 */
class RemotePrintServiceStub implements PrintServiceStub.PrinterDiscoveryCallback {
    /**
     * Wrapped stub
     */
    private final @NonNull PrintServiceStub mStub;

    /**
     * The name of the print service.
     */
    private final @StringRes int mName;

    /**
     * If the print service if for more than a single vendor
     */
    private final boolean mIsMultiVendor;

    /**
     * The the intent to view when the print service is selected by the user.
     */
    private final @NonNull Uri mInstallUri;

    /**
     * The number of printers discovered by the plugin
     */
    private @IntRange(from = 0) int mNumPrinters;

    /**
     * If the stub is started by not yet stopped
     */
    private boolean isRunning;

    /**
     * Listener for changes to {@link #mNumPrinters}.
     */
    private @NonNull OnChangedListener mListener;

    /**
     * Create a new remote for a {@link PrintServiceStub stub}.
     *
     * @param stub          the stub to be wrapped
     * @param listener      The listener to be notified about changes in this stub
     * @param isMultiVendor If the stub detects printers of more than a single vendor
     *
     * @throws StubException If the stub has issues while caching basic stub properties
     */
    public RemotePrintServiceStub(@NonNull PrintServiceStub stub,
            @NonNull OnChangedListener listener, boolean isMultiVendor) throws StubException {
        mListener = listener;
        mStub = stub;
        mIsMultiVendor = isMultiVendor;

        // We handle any throwable to isolate ourself from bugs in the stub code.
        // Cache simple properties to avoid having to deal with exceptions later in the code.
        try {
            mName = Preconditions.checkArgumentPositive(mStub.getName(), "name");
            mInstallUri = Preconditions.checkNotNull(mStub.getInstallUri(), "installUri");
        } catch (Throwable e) {
            throw new StubException(mStub, "Cannot cache simple properties ", e);
        }

        isRunning = false;
    }

    /**
     * Get the name of the print service that is installed via the {@link #mInstallUri}.
     *
     * @return The name of the print service.
     */
    public @StringRes int getName() {
        return mName;
    }

    /**
     * Check if the stub detects printers of more than a single vendor
     *
     * @return true iff the stub detects printers of more than a single vendor
     */
    public boolean isMultiVendor() {
        return mIsMultiVendor;
    }

    /**
     * The URI to view when the print service is selected by the user. Usually a link that will
     * install the print service.
     *
     * @return An Uri to view
     */
    public @NonNull Uri getInstallUri() {
        return mInstallUri;
    }

    /**
     * Stop the stub. From now on there might be callbacks to the registered listener.
     */
    public void start()
            throws StubException {
        // We handle any throwable to isolate our self from bugs in the stub code
        try {
            synchronized (this) {
                mStub.start(this);
                isRunning = true;
            }
        } catch (Throwable e) {
            throw new StubException(mStub, "Cannot start", e);
        }
    }

    /**
     * Stop the stub. From this call on there will not be any more callbacks.
     */
    public void stop() throws StubException {
        // We handle any throwable to isolate our self from bugs in the stub code
        try {
            synchronized (this) {
                mStub.stop();
                isRunning = false;
            }
        } catch (Throwable e) {
            throw new StubException(mStub, "Cannot stop", e);
        }
    }

    /**
     * Get the current number of printers reported by the stub.
     *
     * @return The number of printers reported by the stub.
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

            mListener.onChanged();
        }
    }

    /**
     * Listener to listen for changes to {@link #getNumPrinters}
     */
    public interface OnChangedListener {
        void onChanged();
    }

    /**
     * Exception thrown if the stub has any issues.
     */
    public class StubException extends Exception {
        private StubException(PrintServiceStub stub, String message, Throwable e) {
            super(stub + ": " + message, e);
        }
    }
}

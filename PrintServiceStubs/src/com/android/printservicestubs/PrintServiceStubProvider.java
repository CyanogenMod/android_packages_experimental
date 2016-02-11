package com.android.printservicestubs;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.database.*;
import android.net.Uri;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.printservicestubs.stubs.gcp.GoogleCloudPrintStub;
import com.android.printservicestubs.stubs.mdnsFilter.MDNSFilterStub;
import com.android.printservicestubs.stubs.mopria.MopriaStub;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

/**
 * Content provider for the {@link PrintServiceStub print service stubs}. Returns an {@link
 * PrintServiceStubContract.PrintServiceStubs table} describing which stub found how many printers.
 */
public class PrintServiceStubProvider extends ContentProvider
        implements RemotePrintServiceStub.OnChangedListener {
    private final String LOG_TAG = "PrintServiceStubProv";
    private ArrayList<RemotePrintServiceStub> mStubs;
    private ContentResolver mContentResolver;

    /**
     * Initialize all {@link PrintServiceStub printer service stubs}.
     *
     * @return The list of stubs
     */
    private @NonNull ArrayList<RemotePrintServiceStub> initStubs()
            throws IOException, XmlPullParserException, RemotePrintServiceStub.StubException {
        // Read the stubs defined in {@link R.xml#vendorconfigs vendorconfigs.xml}
        Collection<VendorConfig> configs;
        try {
            configs = VendorConfig.getAllConfigs(getContext());
        } catch (IOException | XmlPullParserException e) {
            // {@link R.xml#vendorconfigs R.xml.vendorconfigs} is part of the file and parsed on
            // each start. There should never be any error in this file.
            throw new RuntimeException(e);
        }

        ArrayList<RemotePrintServiceStub> stubs = new ArrayList<>(configs.size());

        // Add the stubs defined in {@link R.xml#vendorconfigs vendorconfigs.xml}
        for (VendorConfig config : configs) {
            if (!config.getMDNSNames().isEmpty()) {
                Log.d(LOG_TAG, config.toString());
                stubs.add(new RemotePrintServiceStub(
                        new MDNSFilterStub(getContext(), config.getName(),
                                config.getPackageName(), config.getMDNSNames()), this, false));
            }
        }

        // Add non standard stubs
        stubs.add(new RemotePrintServiceStub(new GoogleCloudPrintStub(getContext()), this,
                true));
        stubs.add(new RemotePrintServiceStub(new MopriaStub(getContext()), this, true));

        return stubs;
    }

    @Override
    public boolean onCreate() {
        mContentResolver = getContext().getContentResolver();

        try {
            mStubs = initStubs();

            final int numStubs = mStubs.size();
            for (int i = 0; i < numStubs; i++) {
                RemotePrintServiceStub stub = mStubs.get(i);
                try {
                    stub.start();
                } catch (RemotePrintServiceStub.StubException e) {
                    Log.e(LOG_TAG, "Could not start " + stub, e);

                    // Remove stub as it has issues
                    mStubs.remove(i);
                    i--;
                }
            }

            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not initialize stubs", e);
            return false;
        }
    }

    @Override
    public void shutdown() {
        final int numStubs = mStubs.size();
        for (int i = 0; i < numStubs; i++) {
            RemotePrintServiceStub stub = mStubs.get(i);
            try {
                stub.stop();
            } catch (RemotePrintServiceStub.StubException e) {
                Log.e(LOG_TAG, "Could not stop " + stub, e);

                // Remove stub as it has issues
                mStubs.remove(i);
                i--;
            }
        }
    }

    @Override
    public void onChanged() {
        mContentResolver
                .notifyChange(PrintServiceStubContract.PrintServiceStubs.CONTENT_URI, null);
    }

    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        // We only allow a very specific query
        Preconditions.checkArgument(
                PrintServiceStubContract.PrintServiceStubs.CONTENT_URI.equals(uri));
        Preconditions.checkArgument(Arrays.equals(projection,
                PrintServiceStubContract.PrintServiceStubs.ALL_COLUMNS));
        Preconditions.checkArgument(selection == null);
        Preconditions.checkArgument(selectionArgs == null);
        Preconditions.checkArgument(
                PrintServiceStubContract.PrintServiceStubs.DEFAULT_SORT_ORDER.equals(sortOrder));

        MatrixCursor c = new MatrixCursor(new String[] {
                PrintServiceStubContract.PrintServiceStubs.NAME,
                PrintServiceStubContract.PrintServiceStubs.IS_MULTIVENDOR_SERVICE,
                PrintServiceStubContract.PrintServiceStubs.NUM_DISCOVERED_PRINTERS,
                PrintServiceStubContract.PrintServiceStubs.INSTALL_URI,
                PrintServiceStubContract.PrintServiceStubs._ID
        });

        c.setNotificationUri(mContentResolver,
                PrintServiceStubContract.PrintServiceStubs.CONTENT_URI);

        final int numStubs = mStubs.size();
        ArrayList<Object[]> data = new ArrayList<>(numStubs);
        for (int i = 0; i < numStubs; i++) {
            RemotePrintServiceStub stub = mStubs.get(i);

            int numDiscoveredPrinters = stub.getNumPrinters();
            if (numDiscoveredPrinters != 0) {
                data.add(new Object[] {
                        stub.getName(),
                        stub.isMultiVendor() ? 1 : 0,
                        numDiscoveredPrinters,
                        stub.getInstallUri(),
                        Integer.valueOf(i).toString()
                });
            }
        }

        Collections.sort(data, new Comparator<Object[]>() {
            @Override public int compare(Object[] o1, Object[] o2) {
                int isMultiVendor1 = (int) o1[1];
                int isMultiVendor2 = (int) o2[1];

                int numDiscoveredPrinters1 = (int) o1[2];
                int numDiscoveredPrinters2 = (int) o2[2];

                // Sort by number of printers found first
                int ret = numDiscoveredPrinters2 - numDiscoveredPrinters1;

                // If this was not conclusive sort by multi-vendor or not
                if (ret == 0) {
                    return isMultiVendor1 - isMultiVendor2;
                } else {
                    return ret;
                }
            }
        });

        for (Object[] row : data) {
            c.addRow(row);
        }

        return c;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new IllegalAccessError("Data cannot be inserted");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new IllegalAccessError("Data cannot be deleted");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new IllegalAccessError("Data cannot be deleted");
    }
}

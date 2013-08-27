/*
 * Copyright (C) 2013 The Android Open Source Project
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

package foo.bar.print;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintJob;
import android.print.PrintManager;
import android.print.pdf.PdfDocument.Page;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple sample of how to use the print APIs.
 */
public class PrintActivity extends Activity {

    public static final String LOG_TAG = "PrintActivity";

    private final Object mLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            if (item.getItemId() == R.id.menu_print) {
                printView();
                return true;
            }
        }
        return false;
    }

    public void printFileSimple() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

        PrintJob printJob = printManager.print("My Print Job", new File("foo.pdf"),
                new PrintDocumentInfo.Builder("foo.pdf").create(), null);

        if (printJob != null) {
            /* Yay, we scheduled something to be printed!!! */
        }
    }

    private void printView() {

        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

        final View view = findViewById(R.id.content);

        PrintJob printJob = printManager.print("Print_View",
            new PrintDocumentAdapter() {
                private PrintedPdfDocument mPdfDocument;
                private boolean mCancelled;

                @Override
                public void onStart() {
                    Log.i(LOG_TAG, "onStart");
                    super.onStart();
                }

                @Override
                public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                        CancellationSignal cancellationSignal, LayoutResultCallback callback,
                        Bundle metadata) {
                    Log.i(LOG_TAG, "onLayout[oldAttributes: " + oldAttributes
                            + ", newAttributes: " + newAttributes + "] preview: "
                            + metadata.getBoolean(PrintDocumentAdapter.METADATA_KEY_PRINT_PREVIEW));

                    mPdfDocument = PrintedPdfDocument.open(PrintActivity.this, newAttributes);

                    final boolean cancelled;
                    synchronized (mLock) {
                        mCancelled = false;
                        cancelled = mCancelled;
                    }

                    if (cancelled) {
                        mPdfDocument.close();
                        mPdfDocument = null;
                        callback.onLayoutCancelled();
                    } else {
                        PrintDocumentInfo info = new PrintDocumentInfo
                                .Builder("print_view.pdf", newAttributes)
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(5)
                                .create();
                        callback.onLayoutFinished(info, false);
                    }

                    cancellationSignal.setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel() {
                            Log.i(LOG_TAG, "onLayout#onCancel()");
                            synchronized (mLock) {
                                mCancelled = true;
                                mLock.notifyAll();
                            }
                          }
                    });
                }

                @Override
                public void onWrite(final PageRange[] pages, final FileDescriptor destination,
                        final CancellationSignal canclleationSignal,
                        final WriteResultCallback callback) {
                    Log.i(LOG_TAG, "onWrite[pages: " + Arrays.toString(pages) +"]");

                    final SparseIntArray writtenPagesArray = new SparseIntArray();
                    final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                            @Override
                        protected void onPreExecute() {
                            synchronized (mLock) {
                                for (int i = 0; i < 5; i++) {
                                    if (isCancelled()) {
                                        mPdfDocument.close();
                                        mPdfDocument = null;
                                        callback.onWriteCancelled();
                                        break;
                                    }
                                    if (containsPage(pages, i)) {
                                        writtenPagesArray.append(writtenPagesArray.size(), i);
                                        Page page = mPdfDocument.startPage(i);
                                        view.draw(page.getCanvas());
                                        mPdfDocument.finishPage(page);
                                    }
                                }
                            }
                        }

                        @Override
                        protected Void doInBackground(Void... params) {
                            mPdfDocument.writeTo(new FileOutputStream(destination));
                            mPdfDocument.close();
                            mPdfDocument = null;

                            List<PageRange> pageRanges = new ArrayList<PageRange>();

                            int start = -1;
                            int end = -1;
                            final int writtenPageCount = writtenPagesArray.size(); 
                            for (int i = 0; i < writtenPageCount; i++) {
                                if (start < 0) {
                                    start = writtenPagesArray.valueAt(i);
                                }
                                int oldEnd = end = start;
                                while (i < writtenPageCount && (end - oldEnd) <= 1) {
                                    oldEnd = end;
                                    end = writtenPagesArray.valueAt(i);
                                    i++;
                                }
                                PageRange pageRange = new PageRange(start, end);
                                pageRanges.add(pageRange);
                                start = end = -1;
                            }

                            PageRange[] writtenPages = new PageRange[pageRanges.size()];
                            pageRanges.toArray(writtenPages);
                            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                                return null;
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);

                    canclleationSignal.setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel() {
                            Log.i(LOG_TAG, "onWrite#onCancel()");
                            task.cancel(true);
                            mLock.notifyAll();
                          }
                    });
                }

                @Override
                public void onFinish() {
                    Log.i(LOG_TAG, "onFinish");
                    super.onFinish();
                }

                private boolean containsPage(PageRange[] pageRanges, int page) {
                    final int pageRangeCount = pageRanges.length;
                    for (int i = 0; i < pageRangeCount; i++) {
                        if (pageRanges[i].getStart() <= page
                                && pageRanges[i].getEnd() >= page) {
                            return true;
                        }
                    }
                    return false;
                }

        }, new PrintAttributes.Builder().create());

        if (printJob != null) {
            /* Yay, we scheduled something to be printed!!! */
        }
    }
}

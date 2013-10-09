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
import android.graphics.pdf.PdfDocument.Page;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintJob;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple sample of how to use the print APIs.
 */
public class PrintActivity extends Activity {

    public static final String LOG_TAG = "PrintActivity";

    private static final int PAGE_COUNT = 5;

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
        if (item.getItemId() == R.id.menu_print) {
            printView();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void printView() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        final View view = findViewById(R.id.content);

        final PrintJob printJob = printManager.print("Print_View",
            new PrintDocumentAdapter() {
                private static final int RESULT_LAYOUT_FAILED = 1;
                private static final int RESULT_LAYOUT_FINISHED = 2;

                private PrintAttributes mPrintAttributes;

                @Override
                public void onStart() {
                    Log.i(LOG_TAG, "onStart");
                    super.onStart();
                }

                @Override
                public void onLayout(final PrintAttributes oldAttributes,
                        final PrintAttributes newAttributes,
                        final CancellationSignal cancellationSignal,
                        final LayoutResultCallback callback,
                        final Bundle metadata) {

                    Log.i(LOG_TAG, "onLayout[oldAttributes: " + oldAttributes
                            + ", newAttributes: " + newAttributes + "] preview: "
                            + metadata.getBoolean(PrintDocumentAdapter.EXTRA_PRINT_PREVIEW));

                    new AsyncTask<Void, Void, Integer>() {
                        @Override
                        protected void onPreExecute() {
                            cancellationSignal.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel() {
                                    cancel(true);
                                }
                            });
                            mPrintAttributes = newAttributes;
                        }

                        @Override
                        protected Integer doInBackground(Void... params) {
                            try {
                                for (int i = 0; i < PAGE_COUNT; i++) {
                                    if (isCancelled()) {
                                        return null;
                                    }
                                    pretendDoingLayoutWork();
                                }
                                return RESULT_LAYOUT_FINISHED;
                            } catch (Exception e) {
                                return RESULT_LAYOUT_FAILED;
                            }
                        }

                        @Override
                        protected void onPostExecute(Integer result) {
                            switch (result) {
                                case RESULT_LAYOUT_FAILED: {
                                    Log.i(LOG_TAG, "onLayout#onLayoutFailed()");
                                    callback.onLayoutFailed(null);
                                } break;

                                case RESULT_LAYOUT_FINISHED: {
                                    Log.i(LOG_TAG, "onLayout#onLayoutFinished()");
                                    PrintDocumentInfo info = new PrintDocumentInfo
                                            .Builder("print_view.pdf")
                                            .setContentType(PrintDocumentInfo
                                                    .CONTENT_TYPE_DOCUMENT)
                                            .setPageCount(PAGE_COUNT)
                                            .build();
                                    callback.onLayoutFinished(info, false);
                                } break;
                            }
                        }

                        @Override
                        protected void onCancelled(Integer result) {
                            Log.i(LOG_TAG, "onLayout#onLayoutCancelled()");
                            callback.onLayoutCancelled();
                        }

                        private void pretendDoingLayoutWork() throws Exception {
                            SystemClock.sleep(100);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                }

                @Override
                public void onWrite(final PageRange[] pages,
                        final ParcelFileDescriptor destination,
                        final CancellationSignal canclleationSignal,
                        final WriteResultCallback callback) {
                    Log.i(LOG_TAG, "onWrite[pages: " + Arrays.toString(pages) +"]");

                    final SparseIntArray writtenPagesArray = new SparseIntArray();
                    final PrintedPdfDocument pdfDocument = new PrintedPdfDocument(
                            PrintActivity.this, mPrintAttributes);

                    new AsyncTask<Void, Void, Integer>() {
                        private static final int RESULT_WRITE_FAILED = 1;
                        private static final int RESULT_WRITE_FINISHED = 2;

                            @Override
                        protected void onPreExecute() {
                            canclleationSignal.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel() {
                                    cancel(true);
                                }
                            });

                            for (int i = 0; i < PAGE_COUNT; i++) {
                                if (isCancelled()) {
                                    return;
                                }

                                SystemClock.sleep(100);

                                if (containsPage(pages, i)) {
                                    writtenPagesArray.append(writtenPagesArray.size(), i);
                                    Page page = pdfDocument.startPage(i);
                                    view.draw(page.getCanvas());
                                    pdfDocument.finishPage(page);
                                }
                            }
                        }

                        @Override
                        protected Integer doInBackground(Void... params) {
                            try {
                                pdfDocument.writeTo(new FileOutputStream(
                                        destination.getFileDescriptor()));
                                return RESULT_WRITE_FINISHED;
                            } catch (IOException ioe) {
                                return RESULT_WRITE_FAILED;
                            }
                        }

                        @Override
                        protected void onPostExecute(Integer result) {
                            switch (result) {
                                case RESULT_WRITE_FAILED: {
                                    Log.i(LOG_TAG, "onWrite#onWriteFailed()");
                                    callback.onWriteFailed(null);
                                } break;

                                case RESULT_WRITE_FINISHED: {
                                    Log.i(LOG_TAG, "onWrite#onWriteFinished()");
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
                                    callback.onWriteFinished(writtenPages);
                                } break;
                            }

                            pdfDocument.close();
                        }

                        @Override
                        protected void onCancelled(Integer result) {
                            Log.i(LOG_TAG, "onWrite#onWriteCancelled()");
                            callback.onWriteCancelled();
                            pdfDocument.close();
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
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

        }, null);

        if (printJob != null) {
//            view.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    PrintManager printManager = (PrintManager)
//                            getSystemService(Context.PRINT_SERVICE);
//                    List<PrintJob> printJobs = printManager.getPrintJobs();
//                    Log.i(LOG_TAG, "========================================");
//                    final int printJobCount = printJobs.size();
//                    for (int i = 0; i < printJobCount; i++) {
//                        PrintJob printJob = printJobs.get(i);
//                        Log.i(LOG_TAG, printJob.getInfo().toString());
//                    }
//                    Log.i(LOG_TAG, "========================================\n\n");
//                    view.postDelayed(this, 20000);
//                }
//            }, 20000);
            /* Yay, we scheduled something to be printed!!! */
        }
    }
}

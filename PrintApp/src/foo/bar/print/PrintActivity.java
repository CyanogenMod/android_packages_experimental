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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.util.List;

import libcore.io.IoUtils;

/**
 * Simple sample of how to use the print APIs.
 */
public class PrintActivity extends Activity {

    public static final String LOG_TAG = "PrintActivity";

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

        PrintJob printJob = printManager.print("My Print Job", new File("foo.pdf"), null);

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

                @Override
                public void onStart() {
                    Log.i(LOG_TAG, "onStart");
                    super.onStart();
                }

                @Override
                public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                        CancellationSignal cancellationSignal, LayoutResultCallback callback,
                        Bundle metadata) {
                    Log.i(LOG_TAG, "onLayout");

                    mPdfDocument = new PrintedPdfDocument(PrintActivity.this, newAttributes);
                    Page page = mPdfDocument.startPage();
                    view.draw(page.getCanvas());
                    mPdfDocument.finishPage(page);

                    PrintDocumentInfo info = new PrintDocumentInfo.Builder()
                        .setPageCount(1)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .create();
                    callback.onLayoutFinished(info, false);
                }

                @Override
                public void onWrite(final List<PageRange> pages,
                        final FileDescriptor destination,
                        final CancellationSignal canclleationSignal,
                        final WriteResultCallback callback) {
                    Log.i(LOG_TAG, "onWrite");
                    final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                mPdfDocument.writeTo(new FileOutputStream(destination));
                            } finally {
                                mPdfDocument.close();
                                IoUtils.closeQuietly(destination);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            callback.onWriteFinished(pages);
                        }

                        @Override
                        protected void onCancelled(Void result) {
                            callback.onWriteFailed("Cancelled");
                        }
                    };
 
                    canclleationSignal.setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel() {
                            task.cancel(true);
                        }
                    });

                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                }

                @Override
                public void onFinish() {
                    Log.i(LOG_TAG, "onFinish");
                    super.onFinish();
                }
        }, new PrintAttributes.Builder().create());

        if (printJob != null) {
            /* Yay, we scheduled something to be printed!!! */
        }
    }
}

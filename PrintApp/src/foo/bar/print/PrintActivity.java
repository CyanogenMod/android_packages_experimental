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
import android.print.PrintAdapter;
import android.print.PrintAdapterInfo;
import android.print.PrintAttributes;
import android.print.PrintJob;
import android.print.PrintManager;
import android.print.pdf.PdfDocument.Page;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import libcore.io.IoUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.util.List;

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
            new PrintAdapter() {
                private PrintedPdfDocument mPdfDocument;

                @Override
                public void onStarted() {
                    Log.i(LOG_TAG, "onStarted");
                    super.onStarted();
                }

                @Override
                public boolean onPrintAttributesChanged(PrintAttributes attributes) {
                    Log.i(LOG_TAG, "onPrintAttributesChanged");
                    mPdfDocument = new PrintedPdfDocument(PrintActivity.this, attributes);
                    Page page = mPdfDocument.startPage();
                    view.draw(page.getCanvas());
                    mPdfDocument.finishPage(page);
                    return true;
                }

                @Override
                public void onPrint(final List<PageRange> pages,
                        final FileDescriptor destination,
                        final CancellationSignal canclleationSignal,
                        final PrintProgressListener progressListener) {
                    Log.i(LOG_TAG, "onPrint");
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
                            progressListener.onWriteFinished(pages);
                        }

                        @Override
                        protected void onCancelled(Void result) {
                            progressListener.onWriteFailed("Cancelled");
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
                public void onFinished() {
                    Log.i(LOG_TAG, "onFinished");
                    super.onFinished();
                }

                @Override
                public PrintAdapterInfo getInfo() {
                    Log.i(LOG_TAG, "getInfo");
                    return new PrintAdapterInfo.Builder().setPageCount(1).create();
                }
        }, new PrintAttributes.Builder().create());

        if (printJob != null) {
            /* Yay, we scheduled something to be printed!!! */
        }
    }
}

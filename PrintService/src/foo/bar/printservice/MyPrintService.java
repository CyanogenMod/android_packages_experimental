package foo.bar.printservice;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintAttributes.Margins;
import android.print.PrintAttributes.MediaSize;
import android.print.PrintAttributes.Resolution;
import android.print.PrintAttributes.Tray;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.util.Log;
import android.widget.Toast;

import libcore.io.IoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MyPrintService extends PrintService {

    private static final String LOG_TAG = "MyPrintService";

    private Handler mHandler;

    private PrinterId mFirstFakePrinterId;

    private PrinterId mSecondFakePrinterId;

    private AsyncTask<Void, Void, Void> mFakePrintTask;

    @Override
    public void onCreate() {
        mFirstFakePrinterId = generatePrinterId("Printer 1");
        mSecondFakePrinterId = generatePrinterId("Printer 2");
    }

    @Override
    protected void onConnected() {
        mHandler = new MyHandler(getMainLooper());
        Log.i(LOG_TAG, "#onConnected()");
    }

    @Override
    protected void onDisconnected() {
        cancellAddingFakePrinters();
        Log.i(LOG_TAG, "#onDisconnected()");
    }

    @Override
    protected void onStartPrinterDiscovery() {
        Log.i(LOG_TAG, "#onStartDiscoverPrinters()");
        Message message1 = mHandler.obtainMessage(MyHandler.MSG_ADD_FIRST_FAKE_PRINTER);
        mHandler.sendMessageDelayed(message1, 0);

        Message message2 = mHandler.obtainMessage(MyHandler.MSG_ADD_SECOND_FAKE_PRINTER);
        mHandler.sendMessageDelayed(message2, 10000);
    }

    @Override
    protected void onStopPrinterDiscovery() {
        cancellAddingFakePrinters();
        Log.i(LOG_TAG, "#onStopDiscoverPrinters()");
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        Log.i(LOG_TAG, "#onRequestCancelPrintJob() printJobId: " + printJob.getId());
        if (mHandler.hasMessages(MyHandler.MSG_HANDLE_PRINT_JOB)) {
            mHandler.removeMessages(MyHandler.MSG_HANDLE_PRINT_JOB);
            if (printJob.isQueued() || printJob.isStarted()) {
                printJob.cancel();
            }
        } else if (mFakePrintTask != null) {
            mFakePrintTask.cancel(true);
        } else {
            if (printJob.isQueued() || printJob.isStarted()) {
                printJob.cancel();
            }
        }
    }

    @Override
    protected void onRequestUpdatePrinters(List<PrinterId> printerIds) {
        List<PrinterInfo> udpatedPrinters = new ArrayList<PrinterInfo>();
        final int printerIdCount = printerIds.size();
        for (int i = 0; i < printerIdCount; i++) {
            PrinterId printerId = printerIds.get(i);
            if (printerId.equals(mFirstFakePrinterId)) {
                PrinterInfo printer = new PrinterInfo.Builder(printerId)
                        .setStatus(PrinterInfo.STATUS_READY)
                        .setMinMargins(new Margins(0, 0, 0, 0), new Margins(0, 0, 0, 0))
                        .addMediaSize(MediaSize.createMediaSize(getPackageManager(),
                                MediaSize.ISO_A2), true)
                        .addMediaSize(MediaSize.createMediaSize(getPackageManager(),
                                MediaSize.ISO_A3), false)
                        .addMediaSize(MediaSize.createMediaSize(getPackageManager(),
                                MediaSize.ISO_A4), false)
                        .addMediaSize(MediaSize.createMediaSize(getPackageManager(),
                                MediaSize.NA_LETTER), false)
                        .addResolution(new Resolution("R1", getString(
                                R.string.resolution_600x600), 600, 600), true)
                        .addInputTray(new Tray("FirstInputTray", getString(
                                R.string.input_tray_first)), false)
                        .addOutputTray(new Tray("FirstOutputTray", getString(
                                R.string.output_tray_first)), false)
                        .setDuplexModes(PrintAttributes.DUPLEX_MODE_NONE
                                | PrintAttributes.DUPLEX_MODE_LONG_EDGE
                                | PrintAttributes.DUPLEX_MODE_SHORT_EDGE,
                                PrintAttributes.DUPLEX_MODE_NONE)
                        .setColorModes(PrintAttributes.COLOR_MODE_COLOR
                                | PrintAttributes.COLOR_MODE_MONOCHROME,
                                PrintAttributes.COLOR_MODE_COLOR)
                        .setFittingModes(PrintAttributes.FITTING_MODE_NONE
                                | PrintAttributes.FITTING_MODE_FIT_TO_PAGE,
                                PrintAttributes.FITTING_MODE_NONE)
                        .setOrientations(PrintAttributes.ORIENTATION_PORTRAIT
                                | PrintAttributes.ORIENTATION_LANDSCAPE,
                                PrintAttributes.ORIENTATION_PORTRAIT)
                        .create();
                udpatedPrinters.add(printer);
            } else if (printerId.equals(mSecondFakePrinterId)) {
                PrinterInfo printer = new PrinterInfo.Builder(printerId)
                        .setStatus(PrinterInfo.STATUS_READY)
                        .setMinMargins(new Margins(0, 0, 0, 0), new Margins(0, 0, 0, 0))
                        .addMediaSize(MediaSize.createMediaSize(getPackageManager(),
                                MediaSize.ISO_A4), true)
                        .addMediaSize(MediaSize.createMediaSize(getPackageManager(),
                                MediaSize.ISO_A5), false)
                        .addResolution(new Resolution("R1", getString(
                                R.string.resolution_200x200), 200, 200), true)
                        .addResolution(new Resolution("R2", getString(
                                R.string.resolution_300x300), 300, 300), false)
                        .addInputTray(new Tray("FirstInputTray", getString(
                                R.string.input_tray_first)), false)
                        .addInputTray(new Tray("SecondInputTray", getString(
                                R.string.input_tray_second)), true)
                        .addOutputTray(new Tray("FirstOutputTray", getString(
                                R.string.output_tray_first)), false)
                        .addOutputTray(new Tray("SecondOutputTray",  getString(
                                R.string.output_tray_second)), true)
                        .setDuplexModes(PrintAttributes.DUPLEX_MODE_NONE
                                | PrintAttributes.DUPLEX_MODE_LONG_EDGE
                                | PrintAttributes.DUPLEX_MODE_SHORT_EDGE,
                                PrintAttributes.DUPLEX_MODE_SHORT_EDGE)
                        .setColorModes(PrintAttributes.COLOR_MODE_COLOR
                                | PrintAttributes.COLOR_MODE_MONOCHROME,
                                PrintAttributes.COLOR_MODE_MONOCHROME)
                        .setFittingModes(PrintAttributes.FITTING_MODE_FIT_TO_PAGE
                                | PrintAttributes.FITTING_MODE_NONE,
                                PrintAttributes.FITTING_MODE_FIT_TO_PAGE)
                        .setOrientations(PrintAttributes.ORIENTATION_PORTRAIT
                                | PrintAttributes.ORIENTATION_LANDSCAPE,
                                PrintAttributes.ORIENTATION_LANDSCAPE)
                        .create();
                udpatedPrinters.add(printer);
            }
            if (!udpatedPrinters.isEmpty()) {
                updateDiscoveredPrinters(udpatedPrinters);
            }
        }
    }

    @Override
    public void onPrintJobQueued(final PrintJob printJob) {
        Log.i(LOG_TAG, "#onPrintJobQueued()");
//        printJob.fail("I am lazy today!");
        Message message = mHandler.obtainMessage(MyHandler.MSG_HANDLE_PRINT_JOB, printJob);
//        mHandler.sendMessageDelayed(message, 20000);
        mHandler.sendMessageDelayed(message, 0);
    }

    private void handleHandleQueuedPrintJob(final PrintJob printJob) {
        if (printJob.isQueued()) {
            printJob.start();
        }

        final PrintJobInfo info = printJob.getInfo();
        final File file = new File(getFilesDir(), info.getLabel() + ".pdf");

        Toast.makeText(MyPrintService.this,
                "[STARTED] Printer: " + info.getPrinterId().getPrinterName(),
                Toast.LENGTH_SHORT).show();

        mFakePrintTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Simulate slow print service
//                SystemClock.sleep(20000);

                InputStream in = new BufferedInputStream(
                        new FileInputStream(printJob.getDocument().getData()));
                OutputStream out = null;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    final byte[] buffer = new byte[8192];
                    while (true) {
                        if (isCancelled()) {
                            if (printJob.isStarted()) {
                                printJob.cancel();
                            }
                            break;
                        }
                        final int readByteCount = in.read(buffer);
                        if (readByteCount < 0) {
                            break;
                        }
                        out.write(buffer, 0, readByteCount);
                    }
                } catch (IOException ioe) {
                    /* ignore */
                } finally {
                    IoUtils.closeQuietly(in);
                    IoUtils.closeQuietly(out);
                    if (isCancelled()) {
                        file.delete();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (printJob.isStarted()) {
                    printJob.complete();
                }

                Toast.makeText(MyPrintService.this,
                        "[COMPLETED] Printer: " + info.getPrinterId().getPrinterName(),
                        Toast.LENGTH_SHORT).show();

                file.setReadable(true, false);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/pdf");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent, null);

                mFakePrintTask = null;
            }
        };
        mFakePrintTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void[]) null);
    }

    private void addFirstFakePrinter() {
        PrinterInfo printer = new PrinterInfo.Builder(mFirstFakePrinterId).create();
        List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
        printers.add(printer);
        addDiscoveredPrinters(printers);
    }

    private void addSecondFakePrinter() {
        PrinterInfo printer = new PrinterInfo.Builder(mSecondFakePrinterId).create();
        List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
        printers.add(printer);
        addDiscoveredPrinters(printers);
    }

    private void cancellAddingFakePrinters() {
        mHandler.removeMessages(MyHandler.MSG_ADD_FIRST_FAKE_PRINTER);
        mHandler.removeMessages(MyHandler.MSG_ADD_SECOND_FAKE_PRINTER);
    }

    private final class MyHandler extends Handler {
        public static final int MSG_ADD_FIRST_FAKE_PRINTER = 1;
        public static final int MSG_ADD_SECOND_FAKE_PRINTER = 2;
        public static final int MSG_HANDLE_PRINT_JOB = 3;

        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_ADD_FIRST_FAKE_PRINTER: {
                    addFirstFakePrinter();
                } break;

                case MSG_ADD_SECOND_FAKE_PRINTER: {
                    addSecondFakePrinter();
                } break;

                case MSG_HANDLE_PRINT_JOB: {
                    PrintJob printJob = (PrintJob) message.obj;
                    handleHandleQueuedPrintJob(printJob);
                } break;
            }
        }
    }
}

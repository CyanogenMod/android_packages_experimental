package foo.bar.printservice;

import android.content.Context;
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
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
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

    private PrinterInfo mFirstFakePrinter;

    private PrinterInfo mSecondFakePrinter;

    private AsyncTask<Void, Void, Void> mFakePrintTask;

    private FakePrinterDiscoverySession mSession;

    @Override
    public void onCreate() {
        mFirstFakePrinter = new PrinterInfo.Builder(generatePrinterId("Printer 1"),
                "SHGH-21344", PrinterInfo.STATUS_READY).create();
        mSecondFakePrinter = new PrinterInfo.Builder(generatePrinterId("Printer 2"),
                "OPPPP-09434", PrinterInfo.STATUS_READY).create();
    }

    @Override
    protected void onConnected() {
        Log.i(LOG_TAG, "#onConnected()");
        mHandler = new MyHandler(getMainLooper());
    }

    @Override
    protected void onDisconnected() {
        Log.i(LOG_TAG, "#onDisconnected()");
        if (mSession != null) {
            mSession.cancellAddingFakePrinters();
        }
    }

    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        return new FakePrinterDiscoverySession(this);
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
                "[STARTED] Printer: " + info.getPrinterId().getLocalId(),
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
                        "[COMPLETED] Printer: " + info.getPrinterId().getLocalId(),
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

    private final class MyHandler extends Handler {
        public static final int MSG_HANDLE_PRINT_JOB = 3;

        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_HANDLE_PRINT_JOB: {
                    PrintJob printJob = (PrintJob) message.obj;
                    handleHandleQueuedPrintJob(printJob);
                } break;
            }
        }
    }

    private final class FakePrinterDiscoverySession extends  PrinterDiscoverySession {
        private final Handler mSesionHandler = new SessionHandler(getMainLooper());

        public FakePrinterDiscoverySession(Context context) {
            super(context);
        }

        @Override
        public void onOpen(List<PrinterId> priorityList) {
            Log.i(LOG_TAG, "#onStartDiscoverPrinters()");
            Message message1 = mSesionHandler.obtainMessage(
                    SessionHandler.MSG_ADD_FIRST_FAKE_PRINTER, this);
            mSesionHandler.sendMessageDelayed(message1, 0);

            Message message2 = mSesionHandler.obtainMessage(
                    SessionHandler.MSG_ADD_SECOND_FAKE_PRINTER, this);
            mSesionHandler.sendMessageDelayed(message2, 10000);
        }

        @Override
        public void onClose() {
            cancellAddingFakePrinters();
            Log.i(LOG_TAG, "#onStopDiscoverPrinters()");
        }

        @Override
        public void onRequestPrinterUpdate(PrinterId printerId) {
            if (printerId.equals(mFirstFakePrinter.getId())) {
                PrinterCapabilitiesInfo capabilities =
                        new PrinterCapabilitiesInfo.Builder(printerId)
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

                PrinterInfo printer = new PrinterInfo.Builder(mFirstFakePrinter)
                        .setCapabilities(capabilities)
                        .create();

                List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
                printers.add(printer);
                updatePrinters(printers);

            } else if (printerId.equals(mSecondFakePrinter.getId())) {
                PrinterCapabilitiesInfo capabilities =
                        new PrinterCapabilitiesInfo.Builder(printerId)
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

                PrinterInfo printer = new PrinterInfo.Builder(mSecondFakePrinter)
                    .setCapabilities(capabilities)
                    .create();

                List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
                printers.add(printer);
                updatePrinters(printers);
            }
        }

        private void addFirstFakePrinter(PrinterDiscoverySession session) {
            List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
            printers.add(mFirstFakePrinter);
            session.addPrinters(printers);
        }

        private void addSecondFakePrinter(PrinterDiscoverySession session) {
            List<PrinterInfo> printers = new ArrayList<PrinterInfo>();
            printers.add(mSecondFakePrinter);
            session.addPrinters(printers);
        }

        private void cancellAddingFakePrinters() {
            mSesionHandler.removeMessages(SessionHandler.MSG_ADD_FIRST_FAKE_PRINTER);
            mSesionHandler.removeMessages(SessionHandler.MSG_ADD_SECOND_FAKE_PRINTER);
        }

        final class SessionHandler extends Handler {
            public static final int MSG_ADD_FIRST_FAKE_PRINTER = 1;
            public static final int MSG_ADD_SECOND_FAKE_PRINTER = 2;

            public SessionHandler(Looper looper) {
                super(looper, null, true);
            }

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_ADD_FIRST_FAKE_PRINTER: {
                        PrinterDiscoverySession session =
                                (PrinterDiscoverySession) message.obj;
                        addFirstFakePrinter(session);
                    } break;

                    case MSG_ADD_SECOND_FAKE_PRINTER: {
                        PrinterDiscoverySession session =
                                (PrinterDiscoverySession) message.obj;
                        addSecondFakePrinter(session);
                    } break;
                }
            }
        }
    }
}

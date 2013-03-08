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

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.print.PrintAttributes;
import android.print.PrintAttributes.Margins;
import android.print.PrintAttributes.MediaSize;
import android.print.PrintAttributes.Resolution;
import android.print.pdf.PdfDocument;
import android.print.pdf.PdfDocument.Page;
import android.print.pdf.PdfDocument.PageInfo;
import android.util.DisplayMetrics;
import android.view.Display;

import java.io.OutputStream;

/**
 * This class is a helper for printing content to a different media
 * size. The printed content will be as large as it would be on the
 * screen.
 *
 * THIS IS A TEST CODE AND IS HIDDEN
 *
 * @hide
 */
public final class PrintedPdfDocument {
    private static final int MILS_PER_INCH = 1000;

    /**
     * Maximum bitmap size as defined in Skia's native code (see SkCanvas.cpp,
     * SkDraw.cpp)
     */
    private static final int MAXMIMUM_BITMAP_SIZE = 32766;

    private final PdfDocument mDocument = PdfDocument.open();
    private final Rect mPageSize = new Rect();
    private final Rect mContentSize = new Rect();

    private final int mCanvasDensityDpi;
    private final Matrix mContentToPageTransform;

    private Matrix mTempMatrix;

    /**
     * Creates a new instance.
     *
     * @param context Context instance for accessing resources and services.
     * @param attributes The {@link PrintAttributes} to user.
     */
    public PrintedPdfDocument(Context context, PrintAttributes attributes) {
        MediaSize mediaSize = attributes.getMediaSize();
        Resolution resolution = attributes.getResolution();

        // TODO: What to do if horizontal and vertical DPI differ?
        mCanvasDensityDpi = Math.max(attributes.getResolution().getHorizontalDpi(), attributes
                .getResolution().getVerticalDpi());

        // Figure out the scale since the content and the target DPI may differ.
        DisplayMetrics metrics = new DisplayMetrics();
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        dm.getDisplay(Display.DEFAULT_DISPLAY).getMetrics(metrics);
        final float scaleFactor = ((float) mCanvasDensityDpi) / metrics.densityDpi;
        mContentToPageTransform = new Matrix();
        mContentToPageTransform.setScale(scaleFactor, scaleFactor);

        // Compute the size of the target canvas from the attributes.
        final int pageWidth = (mediaSize.getWidthMils() / MILS_PER_INCH)
                * resolution.getHorizontalDpi();
        final int pageHeight = (mediaSize.getHeightMils() / MILS_PER_INCH)
                * resolution.getVerticalDpi();
        mPageSize.set(0, 0, pageWidth, pageHeight);

        // Compute the content size from the attributes.
        Margins margins = attributes.getMargins();
        final int marginLeft = (margins.getLeftMils() / MILS_PER_INCH)
                * resolution.getHorizontalDpi();
        final int marginTop = (margins.getTopMils() / MILS_PER_INCH) * resolution.getVerticalDpi();
        final int marginRight = (margins.getRightMils() / MILS_PER_INCH)
                * resolution.getHorizontalDpi();
        final int marginBottom = (margins.getBottomMils() / MILS_PER_INCH)
                * resolution.getVerticalDpi();
        mContentSize.set(mPageSize.left + marginLeft, mPageSize.top + marginTop, mPageSize.right
                - marginRight, mPageSize.bottom - marginBottom);
    }

    /**
     * Starts a new page.
     *
     * @return The started page.
     */
    public Page startPage() {
        return startPage(null);
    }

    /**
     * Starts a new page.
     *
     * @param additionalTransform Additional initial transform.
     * @return The started page.
     */
    public Page startPage(Matrix additionalTransform) {
        Matrix transform = null;
        if (additionalTransform != null) {
            if (mTempMatrix == null) {
                transform = mTempMatrix = new Matrix();
            }
            transform.set(mContentToPageTransform);
            transform.postConcat(additionalTransform);
        } else {
            transform = mContentToPageTransform;
        }
        PageInfo pageInfo = new PageInfo.Builder(mPageSize, 0, mCanvasDensityDpi).create();
        Page page = mDocument.startPage(pageInfo);
        return page;
    }

    /**
     * Finishes a page.
     *
     * @param page The page to finish.
     */
    public void finishPage(Page page) {
        mDocument.finishPage(page);
    }

    /**
     * Writes the document to a stream.
     *
     * @param out The destination stream.
     */
    public void writeTo(OutputStream out) {
        mDocument.writeTo(out);
    }

    /**
     * Closes the document.
     */
    public void close() {
        mDocument.close();
    }
}

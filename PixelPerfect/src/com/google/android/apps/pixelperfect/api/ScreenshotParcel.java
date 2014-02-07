package com.google.android.apps.pixelperfect.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.common.logging.RecordedEvent.Screenshot;

import javax.annotation.Nullable;

/**
 * Screenshot parcel. Simply serializes and de-serializes the Screenshot proto
 * to/from the parcel.
 */
public class ScreenshotParcel implements Parcelable {
    // TODO(mukarram) We we probably want to implement something similar to
    // https://googleplex-android.googlesource.com/platform/vendor/
    // unbundled_google/packages/GoogleSearch/+/ub-now-lunchbox/src/com/google/
    // android/sidekick/shared/remoteapi/ProtoParcelable.java
    public static final Parcelable.Creator<ScreenshotParcel> CREATOR =
            new Parcelable.Creator<ScreenshotParcel>() {
        @Override
        public ScreenshotParcel createFromParcel(Parcel source) {
            return new ScreenshotParcel(source);
        }

        @Override
        public ScreenshotParcel[] newArray(int size) {
            return new ScreenshotParcel[size];
        }
    };

    private static final String TAG = "PixelPerfectPlatform.ScreenshotParcel";
    private Exception mException = null;
    public Screenshot screenshotProto = null;

    public ScreenshotParcel() {
        screenshotProto = null;
        mException = null;
    }

    public ScreenshotParcel(Parcel in) {
        readFromParcel(in);
    }

    public ScreenshotParcel(Screenshot screenshotProto) {
        this.screenshotProto = screenshotProto;
    }

    /**
     * Set the exception that is to be written to parcel.
     * @param e the {@link Exception} to parcel.
     */
    public void setException(@Nullable Exception e) {
        this.mException = e;
    }

    // TODO(mukarram) Also add capability to carry any other errors that may
    // occur while capturing screenshots.

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (mException != null) {
            out.writeException(mException);
        } else {
            out.writeNoException();
        }

        if (screenshotProto == null) {
            out.writeInt(0);
            return;
        }

        byte[] bytes = screenshotProto.toByteArray();
        out.writeInt(bytes.length);
        out.writeByteArray(bytes);
    }

    public void readFromParcel(Parcel in) {
        in.readException();
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readByteArray(bytes);
        try {
            Screenshot.Builder builder = Screenshot.newBuilder();
            builder.mergeFrom(bytes);
            screenshotProto = builder.build();
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Could not deserialize proto " + e);
            screenshotProto = null;
        }
    }
}

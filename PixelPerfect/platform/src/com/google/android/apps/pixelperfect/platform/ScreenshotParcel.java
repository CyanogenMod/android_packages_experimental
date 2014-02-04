package com.google.android.apps.pixelperfect.platform;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.common.logging.RecordedEvent.Screenshot;

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

    public Screenshot screenshotProto;

    public ScreenshotParcel() {
    }

    public ScreenshotParcel(Parcel in) {
        readFromParcel(in);
    }

    public ScreenshotParcel(Screenshot screenshotProto) {
        this.screenshotProto = screenshotProto;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        if (screenshotProto == null) {
            out.writeInt(0);
            return;
        }

        byte[] bytes = screenshotProto.toByteArray();
        out.writeInt(bytes.length);
        out.writeByteArray(bytes);
    }

    public void readFromParcel(Parcel in) {
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

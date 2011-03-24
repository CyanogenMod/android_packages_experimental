// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.vending.sectool.v1;

import com.android.vending.sectool.v1.GoogleSettingsContract;
import com.android.vending.sectool.v1.Gservices;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;

/**
 * A convenient way to read Gservices values, inspired by Flag.
 *
 * You must call {@link #init(Context)} before any calls to {@link #get()}.
 * The recommended place to call this is in {@link Application#onCreate()}.
 */
public abstract class GservicesValue<T> {
    private static GservicesReader sGservicesReader = null;

    public static void init(Context context) {
        sGservicesReader = new GservicesReaderImpl(context.getContentResolver());
    }

    public static void initForTests() {
        sGservicesReader = new GservicesReaderForTests();
    }

    protected final String mKey;
    protected final T mDefaultValue;
    private T mOverride = null;

    protected GservicesValue(String key, T defaultValue) {
        mKey = key;
        mDefaultValue = defaultValue;
    }

    /** For tests. */
    public void override(T value) {
        mOverride = value;
    }

    protected abstract T retrieve(String key);

    public final T get() {
        if (mOverride != null) {
            return mOverride;
        }
        return retrieve(mKey);
    }

    public static GservicesValue<Boolean> value(String key, boolean defaultValue) {
        return new GservicesValue<Boolean>(key, defaultValue) {
            @Override
            protected Boolean retrieve(String key) {
                return sGservicesReader.getBoolean(mKey, mDefaultValue);
            }
        };
    }

    public static GservicesValue<Long> value(String key, Long defaultValue) {
        return new GservicesValue<Long>(key, defaultValue) {
            @Override
            protected Long retrieve(String key) {
                return sGservicesReader.getLong(mKey, mDefaultValue);
            }
        };
    }

    public static GservicesValue<Integer> value(String key, Integer defaultValue) {
        return new GservicesValue<Integer>(key, defaultValue) {
            @Override
            protected Integer retrieve(String key) {
                return sGservicesReader.getInt(mKey, mDefaultValue);
            }
        };
    }

    public static GservicesValue<String> value(String key, String defaultValue) {
        return new GservicesValue<String>(key, defaultValue) {
            @Override
            protected String retrieve(String key) {
                return sGservicesReader.getString(mKey, mDefaultValue);
            }
        };
    }

    public static GservicesValue<String> partnerSetting(String key, String defaultValue) {
        return new GservicesValue<String>(key, defaultValue) {
            @Override
            protected String retrieve(String key) {
                return sGservicesReader.getPartnerString(mKey, mDefaultValue);
            }
        };
    }


    private interface GservicesReader {
        public Boolean getBoolean(String key, Boolean defaultValue);
        public Long getLong(String key, Long defaultValue);
        public Integer getInt(String key, Integer defaultValue);
        public String getString(String key, String defaultValue);
        public String getPartnerString(String key, String defaultValue);
    }

    /** The real Gservices reader. */
    private static class GservicesReaderImpl implements GservicesReader {
        private final ContentResolver mContentResolver;
        public GservicesReaderImpl(ContentResolver contentResolver) {
            mContentResolver = contentResolver;
        }

        public Boolean getBoolean(String key, Boolean defaultValue) {
            return Gservices.getBoolean(mContentResolver, key, defaultValue);
        }

        public Integer getInt(String key, Integer defaultValue) {
            return Gservices.getInt(mContentResolver, key, defaultValue);
        }

        public Long getLong(String key, Long defaultValue) {
            return Gservices.getLong(mContentResolver, key, defaultValue);
        }

        public String getString(String key, String defaultValue) {
            return Gservices.getString(mContentResolver, key, defaultValue);
        }

        public String getPartnerString(String key, String defaultValue) {
            return GoogleSettingsContract.Partner.getString(mContentResolver, key, defaultValue);
        }
    }

    /** Implementation of GservicesReader for testing. */
    private static class GservicesReaderForTests implements GservicesReader {
        public Boolean getBoolean(String key, Boolean defaultValue) { return defaultValue; }
        public Integer getInt(String key, Integer defaultValue) { return defaultValue; }
        public Long getLong(String key, Long defaultValue) { return defaultValue; }
        public String getString(String key, String defaultValue) { return defaultValue; }
        public String getPartnerString(String key, String defaultValue) { return defaultValue; }
    }
}

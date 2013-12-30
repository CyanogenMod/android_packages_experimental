LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

LOCAL_JAVA_LIBRARIES := android.test.runner

# To sign the app with the platform certificate.
LOCAL_CERTIFICATE := platform

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := PixelPerfectTests

LOCAL_INSTRUMENTATION_FOR := PixelPerfect

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

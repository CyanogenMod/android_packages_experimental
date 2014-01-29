LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

LOCAL_STATIC_JAVA_LIBRARIES := mockito-target

LOCAL_JAVA_LIBRARIES := android.test.runner

# Use Google certificate instead of platform certificate since GmsCore
# only allows usage from apps signed with the Google certificate.
# Note: the cert here and in ../Android.mk should match.
LOCAL_CERTIFICATE := vendor/unbundled_google/libraries/certs/app
# To sign the app with the platform certificate.
#LOCAL_CERTIFICATE := platform

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := PixelPerfectTests

LOCAL_INSTRUMENTATION_FOR := PixelPerfect

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

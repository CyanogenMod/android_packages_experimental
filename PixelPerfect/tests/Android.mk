LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_STATIC_JAVA_LIBRARIES := mockito-target \
        android-support-test

LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := PixelPerfectPlatformTests

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_INSTRUMENTATION_FOR := PixelPerfectPlatform

LOCAL_SDK_VERSION := 10

include $(BUILD_PACKAGE)

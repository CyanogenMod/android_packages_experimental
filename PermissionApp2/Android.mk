LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_SDK_VERSION := 19

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4

LOCAL_PACKAGE_NAME := Permission2

include $(BUILD_PACKAGE)

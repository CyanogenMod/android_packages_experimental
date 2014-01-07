LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# To sign the app with the platform certificate.
LOCAL_CERTIFICATE := platform

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := PixelPerfect

# Enable proguard.
LOCAL_PROGUARD_ENABLED := full
LOCAL_PROGUARD_FLAG_FILES := proguard.tests.flags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

# Include GMS Core's client library.
# The exact version is controlled by res/values/version.xml
include vendor/unbundled_google/packages/PrebuiltGmsCore/google-play-services-first-party.mk

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

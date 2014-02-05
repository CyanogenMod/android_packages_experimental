########
# Step 1 : Build the platform app

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# Sign the app with the platform certificate.
LOCAL_CERTIFICATE := platform

# Include all test java files.
LOCAL_SRC_FILES := \
        $(call all-java-files-under, ../src/com/google/android/apps/pixelperfect/platform)

LOCAL_PACKAGE_NAME := PixelPerfectPlatform

# Enable proguard.
LOCAL_PROGUARD_ENABLED := full

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/../res

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v13 \
        guava \
        libpixelperfect-protos \
        pixelperfect-api

LOCAL_MANIFEST_FILE := AndroidManifest.xml

# Note: setting LOCAL_SDK_VERSION causes the build to break as it can't find
# the Android private classes this code uses.
#LOCAL_SDK_VERSION := current

# Include GMS Core's client library.
# The exact version is controlled by res/values/version.xml
include vendor/unbundled_google/packages/PrebuiltGmsCore/google-play-services-first-party.mk

include $(BUILD_PACKAGE)


########
# Step 2 : Build sub-packages (e.g. tests)

# Use the following include to make our test apk.
include $(call all-makefiles-under, $(LOCAL_PATH))

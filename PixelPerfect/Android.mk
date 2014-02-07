########
# Step 1 : Build all protobufs in our tree into a separate
# static java library. Useful for IDEs etc. since the generated
# source folder can be excluded if required.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-proto-files-under, imported_protos/src)

LOCAL_PROTOC_OPTIMIZE_TYPE := lite
LOCAL_PROTOC_FLAGS := --proto_path=$(LOCAL_PATH)/imported_protos/src

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libpixelperfect-protos
LOCAL_SDK_VERSION := current
include $(BUILD_STATIC_JAVA_LIBRARY)


########
# Step 2 : Build the shared platform library
$(LOCAL_PATH) := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := pixelperfect-api
LOCAL_MODULE_TAG := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/google/android/apps/pixelperfect/api) \
        $(call all-Iaidl-files-under, src/com/google/android/apps/pixelperfect/api) \
        $(call all-java-files-under, src/com/google/android/apps/pixelperfect/util) \
        $(call all-proto-files-under, imported_protos/src)

LOCAL_PROTOC_OPTIMIZE_TYPE := lite
LOCAL_PROTOC_FLAGS := --proto_path=$(LOCAL_PATH)/imported_protos/src

LOCAL_JAVA_LIBRARIES := guava

include $(BUILD_STATIC_JAVA_LIBRARY)


########
# Step 3 : Build the main app

$(LOCAL_PATH) := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# Use Google certificate instead of platform certificate since GmsCore
# only allows usage from apps signed with the Google certificate.
LOCAL_CERTIFICATE := vendor/unbundled_google/libraries/certs/app

platform_source_file_patterns = src/com/google/android/apps/pixelperfect/platform/%
LOCAL_SRC_FILES := $(filter-out $(platform_source_file_patterns), $(call all-java-files-under, src))

LOCAL_PACKAGE_NAME := PixelPerfect

# Enable proguard.
LOCAL_PROGUARD_ENABLED := full
LOCAL_PROGUARD_FLAG_FILES := proguard.tests.flags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v13 \
        guava \
        pixelperfect-api \
        libpixelperfect-protos

LOCAL_MANIFEST_FILE := AndroidManifest.xml

# Include GMS Core's client library.
# The exact version is controlled by res/values/version.xml
include vendor/unbundled_google/packages/PrebuiltGmsCore/google-play-services-first-party.mk

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# Step 4 : Build sub-packages (e.g. platform & tests)

$(LOCAL_PATH) := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# Sign the app with the platform certificate.
LOCAL_CERTIFICATE := platform

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src/com/google/android/apps/pixelperfect/platform)

LOCAL_PACKAGE_NAME := PixelPerfectPlatform

# Enable proguard.
LOCAL_PROGUARD_ENABLED := full
LOCAL_PROGUARD_FLAG_FILES := proguard.tests.flags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v13 \
        guava \
        libpixelperfect-protos \
        pixelperfect-api

LOCAL_MANIFEST_FILE := platform/AndroidManifest.xml
# Include GMS Core's client library.
# The exact version is controlled by res/values/version.xml
include vendor/unbundled_google/packages/PrebuiltGmsCore/google-play-services-first-party.mk

# Note: setting LOCAL_SDK_VERSION causes the build to break as it can't find
# the Android private classes this code uses.
#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

########
# Step 5 : Build sub-packages (e.g. platform & tests)

# Use the following include to make our test apk.
include $(call all-makefiles-under, $(LOCAL_PATH))


#sdk_version := 19

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
# Use Google certificate instead of platform certificate since GmsCore
# only allows usage from apps signed with the Google certificate.
#LOCAL_CERTIFICATE := platform
LOCAL_CERTIFICATE := vendor/unbundled_google/libraries/certs/app
LOCAL_SDK_VERSION := $(sdk_version)
include $(BUILD_STATIC_JAVA_LIBRARY)


########
# Step 2 : Build the app

$(LOCAL_PATH) := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# Use Google certificate instead of platform certificate since GmsCore
# only allows usage from apps signed with the Google certificate.
#LOCAL_CERTIFICATE := platform
LOCAL_CERTIFICATE := vendor/unbundled_google/libraries/certs/app

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := PixelPerfect

# Enable proguard.
LOCAL_PROGUARD_ENABLED := full
LOCAL_PROGUARD_FLAG_FILES := proguard.tests.flags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v13 \
        guava \
        libpixelperfect-protos

LOCAL_MANIFEST_FILE := AndroidManifest.xml

LOCAL_PROTOC_OPTIMIZE_TYPE := lite

LOCAL_SDK_VERSION := $(sdk_version)

# Include GMS Core's client library.
# The exact version is controlled by res/values/version.xml
include vendor/unbundled_google/packages/PrebuiltGmsCore/google-play-services-first-party.mk

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

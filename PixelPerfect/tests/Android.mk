########
# Step 1 : tests for the main app

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

# Include all test java files.
api_source_file_patterns = src/com/google/android/apps/pixelperfect/api/%
platform_source_file_patterns = src/com/google/android/apps/pixelperfect/platform/%
LOCAL_SRC_FILES := $(filter-out $(api_source_file_patterns), \
        $(filter-out $(platform_source_file_patterns), $(call all-java-files-under, src)))

LOCAL_PACKAGE_NAME := PixelPerfectTests

LOCAL_INSTRUMENTATION_FOR := PixelPerfect

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)


########
# Step 2 : tests for the platform app

$(LOCAL_PATH) := $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

LOCAL_STATIC_JAVA_LIBRARIES := mockito-target

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_CERTIFICATE := platform

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src/com/google/android/apps/pixelperfect/platform) \
        $(call all-java-files-under, src/com/google/android/apps/pixelperfect/api)

LOCAL_PACKAGE_NAME := PixelPerfectPlatformTests

LOCAL_INSTRUMENTATION_FOR := PixelPerfectPlatform

LOCAL_MANIFEST_FILE := ../platform/tests/AndroidManifest.xml

LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

#
# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH:= $(call my-dir)

# Build service apk
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests
LOCAL_SRC_FILES := $(call all-java-files-under, service/src)
LOCAL_SRC_FILES += \
    service/src/com/android/testing/uiautomation/Provider.aidl
LOCAL_MANIFEST_FILE := service/AndroidManifest.xml
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/service/res
LOCAL_PACKAGE_NAME := UiAutomationService
LOCAL_CERTIFICATE := platform
include $(BUILD_PACKAGE)

# Build embeddable jar
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests
LOCAL_SRC_FILES := $(call all-java-files-under, library/src)
LOCAL_SRC_FILES += \
    service/src/com/android/testing/uiautomation/Provider.aidl
LOCAL_JAVA_LIBRARIES := android.test.runner
LOCAL_MODULE := UiAutomationLibrary
include $(BUILD_STATIC_JAVA_LIBRARY)

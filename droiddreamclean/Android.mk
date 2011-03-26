LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= droiddreamclean.c

LOCAL_MODULE:= droiddreamclean

LOCAL_MODULE_TAGS := optional

LOCAL_FORCE_STATIC_EXECUTABLE := true

LOCAL_STATIC_LIBRARIES := libc

include $(BUILD_EXECUTABLE)

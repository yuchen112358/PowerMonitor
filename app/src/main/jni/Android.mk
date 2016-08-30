LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := NativeLib
LOCAL_SRC_FILES := NativeLib.cpp

include $(BUILD_SHARED_LIBRARY)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= libbotwa
LOCAL_SRC_FILES:= botwa.c
include $(BUILD_SHARED_LIBRARY)

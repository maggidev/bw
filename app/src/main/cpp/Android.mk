LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libbotwa-bootstrap
LOCAL_SRC_FILES := botwa-bootstrap-zip.S botwa-bootstrap.c
include $(BUILD_SHARED_LIBRARY)

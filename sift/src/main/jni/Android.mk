LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on

include C:\Users\wyh56\AndroidStudioProjects\OpenCV-android-sdk\sdk\native\jni\OpenCV.mk

LOCAL_MODULE    := nonfree
LOCAL_SRC_FILES := nonfree_init.cpp \
sift.cpp \
surf.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)

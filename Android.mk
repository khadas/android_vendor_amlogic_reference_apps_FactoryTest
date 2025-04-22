
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := gson-2.8.0 zxing
LOCAL_PACKAGE_NAME := FactoryTest
LOCAL_CERTIFICATE := platform
LOCAL_JNI_SHARED_LIBRARIES := libserial_port_jni
#LOCAL_DEX_PREOPT := false


LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_STATIC_ANDROID_LIBRARIES += androidx.appcompat_appcompat
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := gson-2.8.0:libs/gson-2.8.0.jar zxing:libs/zxing.jar
include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under, $(LOCAL_PATH))
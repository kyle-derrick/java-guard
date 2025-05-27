#include <stdlib.h>
#include <string.h>

#include "lib.h"

//JNIEXPORT jint JNICALL
//Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
//    return 0;
//}

int set_file_load_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook) {
    jvmtiEnv *jvmti;
    jint jni_result = (*vm)->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION);
    if (jni_result != JNI_OK) {
        return jni_result;
    }
    // add capabilities
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(capabilities));
    capabilities.can_generate_all_class_hook_events = 1;
    capabilities.can_retransform_classes = 1;
    capabilities.can_retransform_any_class = 1;
    capabilities.can_redefine_classes = 1;
    capabilities.can_redefine_any_class = 1;
    jvmtiError error = (*jvmti)->AddCapabilities(jvmti, &capabilities);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }
    // set class transform hook
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.ClassFileLoadHook = class_file_load_hook;
    /*jvmtiError */error = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, (jint)sizeof(jvmtiEventCallbacks));
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }
    // enable file load hook
    error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }
    return 0;
}

jvmtiEnv* get_jvmti_from_vm(JavaVM *vm) {
    jvmtiEnv *jvmti;
    jint jni_result = (*vm)->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION);
    if (jni_result == JNI_OK) {
        return jvmti;
    }
    return NULL;
}

int jvmti_allocate(jvmtiEnv *jvmti, jlong size, unsigned char** mem_ptr) {
    jvmtiError err = (*jvmti)->Allocate(jvmti, size, mem_ptr);
    return err == JVMTI_ERROR_NONE && *mem_ptr != NULL;
}

int jvmti_retransform_class(jvmtiEnv *jvmti, jint class_count, const jclass *classes) {
    jvmtiError error = (*jvmti)->RetransformClasses(jvmti, class_count, classes);char* error_name;
    return error;
}

int struct_test() {
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(jvmtiEventCallbacks));
    printf("ptr: %p", callbacks.ClassFileLoadHook);
    return 0;
}

int test_base(int i) {
    return i + i;
}
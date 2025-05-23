#include <stdlib.h>
#include <string.h>

#include "lib.h"

int set_file_load_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook) {
    jvmtiEnv *jvmti;
    jint jni_result = (*vm)->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION);
    if (jni_result != JNI_OK) {
        return jni_result;
    }
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.ClassFileLoadHook = class_file_load_hook;
    jvmtiError error = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, (jint)sizeof(jvmtiEventCallbacks));
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }
    error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }
    return 0;
}

int jvmti_allocate(jvmtiEnv *env, jlong size, unsigned char** mem_ptr) {
    jvmtiError err = (*env)->Allocate(env, size, mem_ptr);
    return err == JVMTI_ERROR_NONE && *mem_ptr != NULL;
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
#include <stdlib.h>

#include "jvmti.h";

int set_file_load_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook) {
    jvmtiEnv *jvmti;
    (*vm)->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION);
    jvmtiEventCallbacks callbacks;
    callbacks.ClassFileLoadHook = class_file_load_hook;
    (*jvmti)->SetEventCallbacks(jvmti, &callbacks, (jint)sizeof(jvmtiEventCallbacks));
    return 0;
}

int jvmti_allocate(jvmtiEnv *env, jlong size, unsigned char** mem_ptr) {
    jvmtiError err = (*env)->Allocate(env, size, mem_ptr);
    return err == JVMTI_ERROR_NONE && *mem_ptr != NULL;
}
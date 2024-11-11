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
#include <stdlib.h>
#include <string.h>

#include "lib.h"

#define URL_CLASS_NAME "java/net/URL"

//JNIEXPORT jint JNICALL
//Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
//    return 0;
//}

int init_vm_and_set_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook, jint version) {
    jvmtiEnv *jvmti;
    jint jni_result = (*vm)->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION);
    if (jni_result != JNI_OK) {
        fprintf(stderr, "get jvmti env failed!");
        return jni_result;
    }
    // add capabilities
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(capabilities));
    capabilities.can_generate_all_class_hook_events = 1;
    capabilities.can_retransform_classes = 1;
    capabilities.can_retransform_any_class = 1;
    jvmtiError error = (*jvmti)->AddCapabilities(jvmti, &capabilities);
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "jvmti add capabilities failed!");
        return error;
    }
    // set class transform hook
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.ClassFileLoadHook = class_file_load_hook;
    /*jvmtiError */error = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, (jint)sizeof(jvmtiEventCallbacks));
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "jvmti set event callbacks failed!");
        return error;
    }
    // enable file load hook
    error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "jvmti set event notification mode failed!");
        return error;
    }

    JNIEnv *jni_env;
    jni_result = (*vm)->GetEnv(vm, (void**)&jni_env, version);
    if (jni_result != JNI_OK) {
        fprintf(stderr, "get jni env failed!");
        return jni_result;
    }
    jclass url_class = (*jni_env)->FindClass(jni_env, URL_CLASS_NAME);
    if (url_class == NULL) {
        fprintf(stderr, "get url class failed!");
        return JNI_ERR;
    }
    jclass classes[] = {url_class};
    error = (*jvmti)->RetransformClasses(jvmti, 1, classes);
    if (error != JVMTI_ERROR_NONE) {
        fprintf(stderr, "get extend url class failed!");
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
    jvmtiError error = (*jvmti)->RetransformClasses(jvmti, class_count, classes);
    return error;
}

//
//int jvmti_redefine_class(jvmtiEnv *jvmti, jint class_count, const jvmtiClassDefinition* class_definitions) {
//    jvmtiError error = (*jvmti)->RedefineClasses(jvmti, class_count, class_definitions);
//    return error;
//}

int jvmti_get_class_loader(jvmtiEnv *jvmti, const jclass klass, jobject *class_loader) {
    return (*jvmti)->GetClassLoader(jvmti, klass, class_loader);
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
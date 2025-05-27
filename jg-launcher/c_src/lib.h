
#include "jvmti.h"


extern int set_file_load_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook);

extern jvmtiEnv* get_jvmti_from_vm(JavaVM *vm);

extern int jvmti_allocate(jvmtiEnv *env, jlong size, unsigned char** mem_ptr);

extern int jvmti_retransform_class(jvmtiEnv *jvmti, jint class_count, const jclass *classes);

extern int struct_test();

extern int test_base(int i);

extern void set_jvmti_callbacks(JavaVM *jvm);
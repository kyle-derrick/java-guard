
#include "jvmti.h"


extern int set_file_load_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook);

extern int jvmti_allocate(jvmtiEnv *env, jlong size, unsigned char** mem_ptr);

extern int struct_test();

extern int test_base(int i);
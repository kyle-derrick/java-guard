
#include "jvmti.h"


extern int init_vm_and_set_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook, jint version);

extern jvmtiEnv* get_jvmti_from_vm(JavaVM *vm);

extern int jvmti_allocate(jvmtiEnv *env, jlong size, unsigned char** mem_ptr);

extern int jvmti_retransform_class(jvmtiEnv *jvmti, jint class_count, const jclass *classes);

//extern int jvmti_redefine_class(jvmtiEnv *jvmti, jint class_count, const jvmtiClassDefinition* class_definitions);

extern int jvmti_get_class_loader(jvmtiEnv *jvmti, const jclass class, jobject *class_loader);

extern int struct_test();

extern int test_base(int i);

extern void set_jvmti_callbacks(JavaVM *jvm);
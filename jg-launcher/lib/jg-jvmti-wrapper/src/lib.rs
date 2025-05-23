use jni_sys::{jclass, jint, jlong, jobject, JNIEnv, JavaVM};
use std::ffi::{c_int, c_void};

pub type jvmtiEventClassFileLoadHook = unsafe extern "system" fn(
    jvmti_env: *mut c_void,
    jni_env: *mut JNIEnv,
    class_being_redefined: jclass,
    loader: jobject,
    name: *const std::os::raw::c_char,
    protection_domain: jobject,
    class_data_len: jint,
    class_data: *const std::os::raw::c_uchar,
    new_class_data_len: *mut jint,
    new_class_data: *mut *mut std::os::raw::c_uchar,
);

extern "system" {
    pub fn set_file_load_callback(vm: *const JavaVM, class_file_load_hook: jvmtiEventClassFileLoadHook) -> c_int;
    pub fn jvmti_allocate(jvmti_env: *const c_void, size: jlong, mem_ptr: *mut *mut std::os::raw::c_uchar) -> c_int;
    pub fn struct_test() -> c_int;
    pub fn test_base(i: c_int) -> c_int;
}

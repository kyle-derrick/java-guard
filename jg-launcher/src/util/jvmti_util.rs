#![allow(non_camel_case_types, unused, non_upper_case_globals, non_snake_case)]
use jni_sys::{jint, jlong, jobject, JNIEnv, JavaVM};
use std::ffi::{c_int, c_void};

pub type jvmtiEventClassFileLoadHook = unsafe extern "system" fn(
    jvmti_env: *mut c_void,
    jni_env: *mut JNIEnv,
    class_being_redefined: jni_sys::jclass,
    loader: jobject,
    name: *const std::os::raw::c_char,
    protection_domain: jobject,
    class_data_len: jint,
    class_data: *const std::os::raw::c_uchar,
    new_class_data_len: *mut jint,
    new_class_data: *mut *mut std::os::raw::c_uchar,
);

pub type jvmtiEnv = *const c_void;

extern "system" {
    pub fn init_vm_and_set_callback(vm: *const JavaVM, class_file_load_hook: jvmtiEventClassFileLoadHook, version: jint) -> c_int;
    pub fn get_jvmti_from_vm(vm: *const JavaVM) -> jvmtiEnv;
    pub fn jvmti_allocate(jvmti_env: jvmtiEnv, size: jlong, mem_ptr: *mut *mut std::os::raw::c_uchar) -> c_int;
    pub fn jvmti_retransform_class(jvmti: jvmtiEnv, class_count: jint, classes: *const jni_sys::jclass) -> c_int;
    pub fn jvmti_get_class_loader(jvmti: jvmtiEnv, class: jni_sys::jclass, class_loader: *mut jobject) -> c_int;
    pub fn struct_test() -> c_int;
    pub fn test_base(i: c_int) -> c_int;
}
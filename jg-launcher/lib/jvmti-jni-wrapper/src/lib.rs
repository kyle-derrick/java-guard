use jni_sys::JavaVM;

// type jvmtiEventClassFileLoadHook = unsafe extern "system" fn(
// jvmtiEnv *jvmti_env,
// JNIEnv* jni_env,
// jclass class_being_redefined,
// jobject loader,
// const char* name,
// jobject protection_domain,
// jint class_data_len,
// const unsigned char* class_data,
// jint* new_class_data_len,
// unsigned char** new_class_data)
//
// extern "system" {
//     fn set_file_load_callback(vm: &JavaVM, )
// }
// int set_file_load_callback(JavaVM *vm, jvmtiEventClassFileLoadHook class_file_load_hook)

pub fn add(left: u64, right: u64) -> u64 {
    left + right
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }
}

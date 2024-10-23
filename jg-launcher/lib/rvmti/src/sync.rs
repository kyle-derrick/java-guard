extern crate jni;

use std::os::raw::{c_int, c_void};
use std::ptr;

use jni::JavaVM;

use crate::{JvmtiEnv, jvmtiEnv};

pub trait JvmtiSupplier {
    fn get_jvmti_env(&self, jvmti_version: c_int) -> JvmtiEnv;
}

impl JvmtiSupplier for JavaVM {
    fn get_jvmti_env(&self, jvmti_version: c_int) -> JvmtiEnv {
        let vm = self.get_java_vm_pointer();

        let mut jvmti: *mut c_void = ptr::null_mut();

        unsafe {
            (**vm).GetEnv.unwrap()(vm, &mut jvmti, jvmti_version);

            JvmtiEnv::from(jvmti as *mut jvmtiEnv)
        }
    }
}
use jni::errors::{jni_error_code_to_result, StartJvmError, StartJvmResult};
use jni::{sys, InitArgs};
use jni_sys::{jint, jsize, JavaVM, JavaVMInitArgs, JavaVMOption};
use libloading::Library;
use std::borrow::Cow;
use std::ffi::{c_void, CStr, OsStr};
use std::mem::transmute;
use std::path::PathBuf;
use std::rc::Rc;

pub struct JvmWrapper {
    pub library: Rc<Library>,
    pub get_default_java_vm_init_args: unsafe extern "system" fn (args: *mut c_void) -> jint,
    pub create_java_vm: unsafe extern "system" fn (
        pvm: *mut *mut JavaVM,
        penv: *mut *mut c_void,
        args: *mut c_void,
    ) -> jint,
    pub get_created_java_vms: unsafe extern "system" fn (vm_buf: *mut *mut JavaVM, buf_len: jsize, n_vms: *mut jsize) -> jint,
}

struct __InitArgs<'a> {
    inner: JavaVMInitArgs,

    // `JavaVMOption` structures are stored here. The JVM accesses this `Vec`'s contents through a
    // raw pointer.
    _opts: Vec<JavaVMOption>,

    // Option strings are stored here. This ensures that any that are owned aren't dropped before
    // the JVM is finished with them.
    _opt_strings: Vec<Cow<'a, CStr>>,
}

impl JvmWrapper {
    pub fn load_jvm() -> StartJvmResult<JvmWrapper> {
        let path = [
            java_locator::locate_jvm_dyn_library()
                .map_err(StartJvmError::NotFound)?
                .as_str(),
            java_locator::get_jvm_dyn_lib_file_name(),
        ]
            .iter()
            .collect::<PathBuf>();
        Self::load_jvm_with(path)
    }
    pub fn load_jvm_with<P: AsRef<OsStr>>(libjvm_path: P) -> StartJvmResult<JvmWrapper> {
        let libjvm_path_string = libjvm_path.as_ref().to_string_lossy().into_owned();

        // Try to load it.
        let libjvm = match unsafe { libloading::Library::new(libjvm_path.as_ref()) } {
            Ok(ok) => ok,
            Err(error) => return Err(StartJvmError::LoadError(libjvm_path_string, error)),
        };
        let libjvm = Rc::new(libjvm);

        unsafe {
            // Try to find the `JNI_CreateJavaVM` function in the loaded library.
            let create_fn = libjvm
                .get(b"JNI_CreateJavaVM\0")
                .map_err(|error| StartJvmError::LoadError(libjvm_path_string.to_owned(), error))?;
            let default_args_fn = libjvm
                .get(b"JNI_GetDefaultJavaVMInitArgs\0")
                .map_err(|error| StartJvmError::LoadError(libjvm_path_string.to_owned(), error))?;
            let get_created_fn = libjvm
                .get(b"JNI_GetCreatedJavaVMs\0")
                .map_err(|error| StartJvmError::LoadError(libjvm_path_string.to_owned(), error))?;


            Ok(JvmWrapper {
                library: libjvm.clone(),
                get_default_java_vm_init_args: *default_args_fn,
                create_java_vm: *create_fn,
                get_created_java_vms: *get_created_fn
            })
        }
    }

    pub fn create_java_vm(&self, args: InitArgs) -> jni::errors::Result<(jni::JavaVM, jni::JNIEnv)> {
        let mut ptr: *mut sys::JavaVM = ::std::ptr::null_mut();
        let mut env: *mut sys::JNIEnv = ::std::ptr::null_mut();

        unsafe {
            let args: __InitArgs = transmute(args);
            jni_error_code_to_result((self.create_java_vm)(
                &mut ptr as *mut _,
                &mut env as *mut *mut sys::JNIEnv as *mut *mut std::os::raw::c_void,
                &args.inner as *const _ as _,
            ))?;

            let vm = jni::JavaVM::from_raw(ptr)?;
            let env = jni::JNIEnv::from_raw(env)?;
            // java_vm_unchecked!(vm.0, DetachCurrentThread);

            Ok((vm, env))
        }
    }
}
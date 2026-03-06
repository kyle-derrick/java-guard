use crate::base::error::MessageError;
use crate::with_message;
use jni::errors::{jni_error_code_to_result, Error, StartJvmError, StartJvmResult};
use jni::objects::{JClass, JString, JValue, JValueOwned};
use jni::{sys, InitArgs, JNIEnv, JNIVersion};
use jni_sys::{jint, jsize, JavaVM, JavaVMInitArgs, JavaVMOption};
use libloading::Library;
use std::borrow::Cow;
use std::env;
use std::ffi::{c_void, CStr, OsStr};
use std::mem::transmute;
use std::path::PathBuf;
use std::rc::Rc;

#[allow(unused)]
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

    pub fn create_java_vm(&self, args: InitArgs) -> jni::errors::Result<(jni::JavaVM, jni::JNIEnv<'_>)> {
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

#[inline]
pub fn jni_error_handle(env: &JNIEnv,err: &jni::errors::Error, msg_prefix: &str) -> MessageError {
    let msg = if msg_prefix.is_empty() {
        format!("Error: {}", err.to_string())
    } else {
        format!("Error: {}: {}", msg_prefix, err.to_string())
    };
    match &err {
        Error::JavaException => {
            if let Ok(true) = env.exception_check() {
                if let Err(err) = env.exception_describe() {
                    eprintln!("print exception failed: {}", err.to_string());
                }
                if let Err(err) = env.exception_clear() {
                    eprintln!("clear exception failed: {}", err.to_string());
                }
            }
        }
        Error::JniCall(inner_err) => {
            eprintln!("JniCall Error: {}", inner_err.to_string());
        }
        _ => {}
    }
    MessageError::new(&msg)
}

#[macro_export]
macro_rules! jni_result_expect {
    ($env:expr, $result:expr) => {
        jni_result_expect!($env, $result, "")
    };
    ($env:expr, $result:expr, $msg_prefix:expr) => {
        $result.map_err(|e| crate::util::jvm_util::jni_error_handle($env, &e, $msg_prefix))
    };
}

#[inline]
pub fn parse_classpath(classpath_str: &str) -> Vec<String> {
    env::split_paths(&classpath_str).filter_map(|item| {
        if let Some(item) = item.to_str() {
            Some(item.to_string())
        } else {
            None
        }
    }).collect()
}

pub fn jstr_to_string(env: &mut JNIEnv, jstring: &JString) -> Result<Option<String>, MessageError> {
    if jstring.is_null() {
        return Ok(None)
    }
    let jstr = jni_result_expect!(env, env.get_string(jstring))?;
    Ok(Some(with_message!(jstr.to_str(), "failed to convert string to string")?.to_string()))
}

pub fn get_sys_property(env: &mut JNIEnv, sys_cls: &JClass, key: &str) -> Result<Option<String>, MessageError> {
    let ket_string = jni_result_expect!(env, env.new_string(key))?;
    let result = jni_result_expect!(env, env.call_static_method(sys_cls, "getProperty", "(Ljava/lang/String;)Ljava/lang/String;",
                        &[JValue::Object(&ket_string)]), &format!("get system property [{key}] failed"))?;
    jni_result_expect!(env, env.delete_local_ref(ket_string))?;
    if let JValueOwned::Object(obj) =  result {
        let jstring = JString::from(obj);
        let value_result = jstr_to_string(env, &jstring);
        if let Err(err) = &value_result {
            jni_result_expect!(env, env.delete_local_ref(jstring), &err.to_string())?;
        } else {
            jni_result_expect!(env, env.delete_local_ref(jstring))?;
        }
        value_result
        // let jstr = jni_result_expect!(env, env.get_string(&jstring))?;
        // Ok(with_message!(jstr.to_str(), "failed to convert string to string")?.to_string())
    } else {
        Err(MessageError::new("failed to get system property, result type is not string"))
    }
}

#[inline]
pub fn destroy_vm(jvm: &jni::JavaVM, env: &JNIEnv) {
    unsafe {
        jvm.detach_current_thread();
        if let Err(err) = jvm.destroy() {
            jni_error_handle(env, &err, "Error when destroy vm:");
        }
    }
}

pub fn print_version() {
    let init_args = jni::InitArgsBuilder::new()
        .version(JNIVersion::V8)
        .build()
        .expect("init Jvm args failed");

    let wrapper = JvmWrapper::load_jvm().expect("failed to load Java VM!");
    let (jvm, mut env) = wrapper.create_java_vm(init_args).expect("failed to create Java VM!");

    if let Err(err) = print_version_info(&mut env) {
        eprintln!("{}", err);
    }

    destroy_vm(&jvm, &env);
}

pub fn print_version_info(env: &mut JNIEnv) -> Result<(), MessageError> {
    let sys_cls = jni_result_expect!(env, env.find_class("java/lang/System"))?;
    let version = get_sys_property(env, &sys_cls, "java.version")?;
    let runtime_version = get_sys_property(env, &sys_cls, "java.runtime.version")?;
    let vm_name = get_sys_property(env, &sys_cls, "java.vm.name")?;
    let vm_version = get_sys_property(env, &sys_cls, "java.vm.version")?;
    let vm_info = get_sys_property(env, &sys_cls, "java.vm.info")?;

    let version_date = get_sys_property(env, &sys_cls, "java.version.date")?;
    let vm_specification_version = get_sys_property(env, &sys_cls, "java.vm.specification.version")?;

    eprint!("java version: {}", version.ok_or_else(|| MessageError::new("version is null"))?);
    if let Some(version_date) = version_date {
        eprint!(" {}", version_date);
        if let Some(vm_specification_version) = vm_specification_version {
            eprint!(" {}", vm_specification_version);
        }
    }
    eprintln!();

    if let Some(runtime_version) = runtime_version {
        eprintln!("{} (build {})", get_sys_property(env, &sys_cls, "java.runtime.name")?.ok_or_else(|| MessageError::new("runtime name is null"))?, runtime_version);
        eprint!("{} (build {}", vm_name.ok_or_else(|| MessageError::new("vm name is null"))?, vm_version.ok_or_else(|| MessageError::new("vm version is null"))?);
        if let Some(vm_info) = vm_info {
            eprint!(", {}", vm_info);
        }
        eprintln!(")");
    }
    Ok(())
}
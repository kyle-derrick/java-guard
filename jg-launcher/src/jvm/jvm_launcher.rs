use crate::args_parser::LaunchTarget::Jar;
use crate::args_parser::LauncherArg;
use crate::base::common::{runtime_classes, RESOURCE_DECRYPT_NATIVE_CLASS, RESOURCE_DECRYPT_NATIVE_DESC, RESOURCE_DECRYPT_NATIVE_METHOD};
use crate::jvm::jvm_util::JvmWrapper;
use crate::jvm::launcher_helper::{JvmLauncherHelper, LauncherHelper, SimpleLauncherHelper, SunLauncherHelper};
use crate::util::byte_utils::byte_be_to_u32_fast;
use crate::util::class_util;
use crate::util::class_util::url_extended_processing;
use crate::util::jvmti_util::{get_jvmti_from_vm, jvmti_allocate, jvmti_retransform_class, init_vm_and_set_callback, jvmti_get_class_loader};
use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::jsize;
use jni::{sys, JNIEnv, JavaVM, NativeMethod};
use jni::JNIVersion;
use jni_sys::{jarray, jbyteArray, jclass as java_class, jint, jlong, jobject, JavaVMInitArgs, JNI_VERSION_1_1};
use std::ffi::{c_void, CStr};
use std::process::exit;
use std::ptr::{null, null_mut};
use jni::errors::Error;
use jni::strings::JNIString;
use crate::util::aes_util::{decrypt, decrypt_resource};

const JAVA_CLASS_PATH_VM_ARG_PREFIX: &str = "-Djava.class.path=";
const URL_CLASS_NAME: &str = "java/net/URL";
const CLASS_ENCRYPT_FLAG: u8 = 1 << 7;

pub fn jvm_launch(launcher_arg: &LauncherArg) {
    let vm_ops = launcher_arg.vm_args();

    let mut args_builder = jni::InitArgsBuilder::new()
        .version(JNIVersion::V8);
    let target = launcher_arg.target();
    let mut java_class_path = String::from(JAVA_CLASS_PATH_VM_ARG_PREFIX);
    if let Jar(jar) = &target {
        java_class_path.push_str(jar.path());
        // args_builder = args_builder.option(&java_class_path);
        (jar.main_class().replace('.', "/"), jar.signature())
    } else {
        // todo not currently supported
        panic!("not currently supported run class")
    };
    for item in vm_ops.iter() {
        args_builder = args_builder.option(item.trim());
    };
    let init_args = args_builder
        .option("-Xlog:jvmti=debug")
        .option(&java_class_path)
        .build()
        .expect("init Jvm args failed");

    // let jvm = JavaVM::new(init_args).unwrap();
    // let jvm = JavaVM::with_libjvm(init_args,
    //                               || StartJvmResult::Ok(PathBuf::from("D:\\software\\install\\Java\\jdk1.8.0_202\\jre\\bin\\server\\jvm.dll"))).unwrap();

    let wrapper = JvmWrapper::load_jvm().unwrap();
    // let wrapper = JvmWrapper::load_jvm_with("D:\\software\\install\\Java\\jdk1.8.0_202\\jre\\bin\\server\\jvm.dll").unwrap();

    // ---------------------
    // test
    let mut args1 = JavaVMInitArgs {
        version: JNI_VERSION_1_1,
        nOptions: 0,
        options: null_mut(),
        ignoreUnrecognized: 0,
    };
    let result = unsafe {
        let p = &mut args1 as *mut JavaVMInitArgs;
        (wrapper.get_default_java_vm_init_args)(p as *mut c_void)
    };
    println!("get_default_java_vm_init_args result: {result}");
    // ---------------------

    let (jvm, mut env) = wrapper.create_java_vm(init_args).unwrap();
    let vers = env.get_version().expect("get jvm version failed!");
    set_callbacks(&jvm, vers.into());

    // get JNI env
    // let mut env = jvm.attach_current_thread().expect("get jni env failed");
    // let mut env = jvm.get_env().unwrap();

    let env_ref = &mut env;
    // extend_url_class(&jvm, env_ref);
    let default_launcher = SimpleLauncherHelper::from_env(env_ref).expect("cannot init default launcher helper");
    load_ext_runtime(&jvm, env_ref, &default_launcher);
    // extend_url_class(&jvm, env_ref);

    let app_args = launcher_arg.app_args();

    let helper = match SunLauncherHelper::from_env(env_ref) {
        Ok(helper) => LauncherHelper::SunLauncherHelper(helper),
        Err(_) => {
            println!("WARN: not found sun launcher helper");
            LauncherHelper::SimpleLauncherHelper(default_launcher)
        }
    };

    let main_class = helper.check_and_load_main(env_ref, target).expect(&format!("can not load main class: {}", target.main_class()));
    // let main_class = env.find_class(&main_class_name).expect(&format!("not found main class: {}", &main_class_name));

    call_main(env_ref, &main_class, app_args);

    unsafe {
        jvm.detach_current_thread();
        jvm.destroy().unwrap();
    }
}

fn extend_url_class(jvm: &JavaVM, env: &mut JNIEnv) {
    let jvmti = unsafe {
        get_jvmti_from_vm(jvm.get_java_vm_pointer())
    };
    if jvmti == null() {
        eprintln!("cannot get jvmti");
        exit(-1);
    }
    let classes = [env.find_class(URL_CLASS_NAME).expect("not found URL class").as_raw()];
    let result = unsafe {
        jvmti_retransform_class(jvmti, classes.len() as jint, classes.as_ptr())
    };
    if 0 != result {
        eprintln!("cannot extend URL: {result}");
        exit(-1);
    }
}

fn load_ext_runtime(jvm: &JavaVM, env: &mut JNIEnv, default_launcher: &SimpleLauncherHelper) {
    let url_class = env.find_class(URL_CLASS_NAME).expect("url class cannot found!");
    let jvmti = unsafe {
        get_jvmti_from_vm(jvm.get_java_vm_pointer())
    };
    let mut class_loader: jobject = null_mut();
    let result = unsafe {
        jvmti_get_class_loader(jvmti, url_class.as_raw(), &mut class_loader)
    };
    if result != 0 {
        eprintln!("ERROR: cannot found url class's loader!");
        exit(-1);
    }
    let classes = runtime_classes();
    let mut index = 0;
    while index < classes.len() {
        let start = index;
        index += 4;
        if index >= classes.len() {
            eprintln!("WARN: runtime class is damaged");
            break;
        }
        let name_len = byte_be_to_u32_fast(classes, start);
        let start = index;
        index += name_len as usize;
        let name_end = index;
        index += 4;
        if index >= classes.len() {
            eprintln!("WARN: runtime class is damaged");
            break;
        }
        let name = String::from_utf8_lossy(&classes[start..name_end]).to_string().replace(".", "/");

        let class_len = byte_be_to_u32_fast(classes, name_end);
        let start = index;
        index += class_len as usize;
        if index > classes.len() {
            eprintln!("WARN: runtime class is damaged");
            break;
        }
        let class_data = &classes[start..index];
        let class_loader = unsafe {
            JObject::from_raw(class_loader)
        };
        let class_obj = env.define_class(&name, &class_loader, class_data).expect("cannot load extend runtime class");

        if &name == RESOURCE_DECRYPT_NATIVE_CLASS {
            // let native_class = env.find_class(RESOURCE_DECRYPT_NATIVE_CLASS).expect("cannot bind ext runtime class");
            let native_method = NativeMethod {
                name: JNIString::from(RESOURCE_DECRYPT_NATIVE_METHOD),
                sig: JNIString::from(RESOURCE_DECRYPT_NATIVE_DESC),
                fn_ptr: resource_decrypt_native as *mut c_void,
            };
            env.register_native_methods(class_obj, &[native_method]).expect("cannot bind ext runtime clas");
        }
    }
}
extern "system" fn resource_decrypt_native(env: *mut sys::JNIEnv, object: jni_sys::jobject, data: jbyteArray, off: jint, len: jint) -> jbyteArray {
    let env = match unsafe {
        JNIEnv::from_raw(env)
    } {
        Ok(env) => {
            env
        }
        Err(err) => {
            eprintln!("ERROR: native method: cannot get env!");
            return data;
        }
    };
    let mut data_rs = match env.convert_byte_array(&unsafe { JByteArray::from_raw(data) }) {
        Ok(data) => {
            data
        }
        Err(err) => {
            eprintln!("ERROR: native method: cannot convert data: {err}");
            return data;
        }
    };
    let end = (off + len) as usize;
    let off = off as usize;
    let ddd = &data_rs[..];
    let result = match decrypt_resource(&mut data_rs[off..end]) {
        Ok(data) => {
            data
        }
        Err(err) => {
            eprintln!("ERROR: native method: decrypt resource failed: {err}");
            return data;
        }
    };
    match env.byte_array_from_slice(result) {
        Ok(data) => {
            data.as_raw()
        }
        Err(err) => {
            eprintln!("ERROR: native method: decrypt resource failed: {err}");
            data
        }
    }
}

fn set_callbacks(jvm: &JavaVM, version: i32) {
    let result = unsafe {
        init_vm_and_set_callback(jvm.get_java_vm_pointer(), jg_class_file_load_hook, version)
    };
    if 0 != result {
        eprintln!("set transformer hook failed");
    }
}

extern "system" fn jg_class_file_load_hook(
        jvmti_env: *mut c_void,
        jni_env: *mut jni_sys::JNIEnv,
        class_being_redefined: java_class,
        loader: jobject,
        name: *const std::os::raw::c_char,
        protection_domain: jobject,
        class_data_len: jint,
        class_data: *const std::os::raw::c_uchar,
        new_class_data_len: *mut jint,
        new_class_data: *mut *mut std::os::raw::c_uchar,
        ) {
    let class_data_arr = unsafe {
        // JNIEnv::from_raw(jni_env).unwrap(),
        std::slice::from_raw_parts(class_data as *const u8, class_data_len as usize)
    };
    let is_url = match unsafe { CStr::from_ptr(name) }.to_str() {
        Ok(name) => {
            // println!(">>>>>>: {name}");
            name == URL_CLASS_NAME
        },
        Err(err) => {
            eprintln!("WARN: class name to str failed: {}", err);
            false
        }
    };

    let transformed = if let Some(mut new_class_data_bytes) = class_util::try_decrypt_class(class_data_arr) {
        if is_url {
            if let Some(extended_class_data) = url_extended_processing(&new_class_data_bytes) {
                new_class_data_bytes = extended_class_data;
            }
        }
        set_new_class_data(jvmti_env, &new_class_data_bytes, new_class_data_len, new_class_data)
    } else if is_url {
        if let Some(extended_class_data) = url_extended_processing(class_data_arr) {
            set_new_class_data(jvmti_env, &extended_class_data, new_class_data_len, new_class_data)
        } else {
            false
        }
    } else {
        false
    };

    // if transformed {
    //     return;
    // }
    // unsafe {
    //     *new_class_data = class_data as *mut c_uchar;
    //     *new_class_data_len = class_data_len;
    // }
}

fn set_new_class_data(jvmti_env: *mut c_void, class_data: &[u8], new_class_data_len: *mut jint, new_class_data: *mut *mut std::os::raw::c_uchar) -> bool {
    let new_class_data_bytes_len = class_data.len();
    let mut new_class_data_ptr = std::ptr::null_mut();
    if 0 == unsafe { jvmti_allocate(jvmti_env, new_class_data_bytes_len as jlong, &mut new_class_data_ptr) } {
        eprintln!("allocate decrypted class data failed");
        return false;
    }
    unsafe {
        std::ptr::copy_nonoverlapping(class_data.as_ptr(), new_class_data_ptr, new_class_data_bytes_len);
        *new_class_data = new_class_data_ptr;
        *new_class_data_len = new_class_data_bytes_len as jint;
    }
    true
}

fn call_main(env: &mut JNIEnv, main_class: &JClass, app_args: &Vec<String>) {
    let args = env.new_object_array(jsize::from(app_args.len() as i32), "java/lang/String", JObject::null()).unwrap();

    for (i, item) in app_args.iter().enumerate() {
        env.set_object_array_element(&args, jsize::from(i as i32), env.new_string(item).unwrap()).unwrap();
    }

    let params = [jni::objects::JValue::Object(&args)];
    match env.call_static_method(main_class, "main", "([Ljava/lang/String;)V",
                           &params) {
        Ok(_) => {
        }
        Err(err) => {
            if let Ok(true) = env.exception_check() {
                env.exception_describe().expect("print error failed!");
                env.exception_clear().unwrap();
            } else {
                eprintln!("main method execution failed!")
            }
        }
    }
    // env.call_static_method(main_class, "main", "([Ljava/lang/String;)V",
    //                        &params).unwrap();
}
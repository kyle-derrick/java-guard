use crate::args_parser::LaunchTarget::Jar;
use crate::args_parser::LauncherArg;
use crate::base::common::runtime_classes;
use crate::jvm::jvm_util::JvmWrapper;
use crate::jvm::launcher_helper::{JvmLauncherHelper, LauncherHelper, SimpleLauncherHelper, SunLauncherHelper};
use crate::util::byte_utils::byte_be_to_u32_fast;
use crate::util::class_util;
use crate::util::class_util::url_extended_processing;
use crate::util::jvmti_util::{get_jvmti_from_vm, jvmti_allocate, jvmti_retransform_class, set_file_load_callback};
use jni::objects::{JClass, JObject};
use jni::sys::jsize;
use jni::{JNIEnv, JavaVM};
use jni::JNIVersion;
use jni_sys::{jclass as java_class, jint, jlong, jobject, JavaVMInitArgs, JNI_VERSION_1_1};
use std::ffi::{c_void, CStr};
use std::process::exit;
use std::ptr::null_mut;

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
    set_callbacks(&jvm);
    let vers = env.get_version();

    // get JNI env
    // let mut env = jvm.attach_current_thread().expect("get jni env failed");
    // let mut env = jvm.get_env().unwrap();

    let env_ref = &mut env;
    // extend_url_class(&jvm, env_ref);
    let default_launcher = SimpleLauncherHelper::from_env(env_ref).expect("cannot init default launcher helper");
    load_ext_runtime(env_ref, &default_launcher);
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
    let classes = [env.find_class(URL_CLASS_NAME).expect("not found URL class").as_raw()];
    let result = unsafe {
        jvmti_retransform_class(jvmti, classes.len() as jint, classes.as_ptr())
    };
    if 0 != result {
        eprintln!("cannot extend URL: {result}");
        exit(-1);
    }
}

fn load_ext_runtime(env: &mut JNIEnv, default_launcher: &SimpleLauncherHelper) {
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
        let _class_obj = env.define_class(name, &default_launcher.class_loader, class_data).expect("cannot load extend runtime class");
    }
}

fn set_callbacks(jvm: &JavaVM) {
    let result = unsafe {
        set_file_load_callback(jvm.get_java_vm_pointer(), jg_class_file_load_hook)
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
            println!(">>>>>>: {name}");
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
    env.call_static_method(main_class, "main", "([Ljava/lang/String;)V",
                           &params).unwrap();
}
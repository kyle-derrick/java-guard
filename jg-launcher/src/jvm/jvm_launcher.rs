use crate::args_parser::LaunchTarget::Jar;
use crate::args_parser::LauncherArg;
use crate::common::transform_mod;
use crate::jvm::jvm_util::JvmWrapper;
use crate::jvm::launcher_helper::{find_launcher_helper_from_env, JvmLauncherHelper};
use crate::util::byte_utils::byte_to_u32;
use jg_jvmti_wrapper::set_file_load_callback;
use jni::objects::{JByteArray, JClass, JObject, JValue};
use jni::strings::JNIString;
use jni::sys::jsize;
use jni::{JNIEnv, JavaVM};
use jni::{JNIVersion, NativeMethod};
use jni_sys::{_jobject, jclass, jint, jobject, JavaVMInitArgs, JNI_VERSION_1_1};
use std::ffi::{c_void, CStr};
use std::os::raw::c_uchar;
use std::ptr::null_mut;

const JAVA_CLASS_PATH_VM_ARG_PREFIX: &str = "-Djava.class.path=";
const URL_CLASS_NAME: &str = "java/net/URL";
const CLASS_ENCRYPT_FLAG: u8 = 1 << 7;

static mut TRANSFORMER_OBJ: Option<*mut _jobject> = None;

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
    println!("{result}");
    // ---------------------

    let (jvm, mut env) = wrapper.create_java_vm(init_args).unwrap();
    let vers = env.get_version();
    set_callbacks(&jvm);

    // get JNI env
    // let mut env = jvm.attach_current_thread().expect("get jni env failed");
    // let mut env = jvm.get_env().unwrap();

    let env_ref = &mut env;
    load_mod(env_ref);

    let app_args = launcher_arg.app_args();

    let helper = find_launcher_helper_from_env(env_ref);

    let main_class = helper.check_and_load_main(env_ref, target).expect(&format!("can not load main class: {}", target.main_class()));
    // let main_class = env.find_class(&main_class_name).expect(&format!("not found main class: {}", &main_class_name));

    call_main(env_ref, &main_class, app_args);

    unsafe {
        jvm.detach_current_thread();
        jvm.destroy().unwrap();
    }
}

fn load_mod(env: &mut JNIEnv) {
    let transform_mod_code = transform_mod();
    let len = byte_to_u32(&transform_mod_code[..4]) as usize;
    let loader_class = env.define_unnamed_class(&JObject::null(), &transform_mod_code[4..len]).expect("load mod failed");
    let transformer_index = 4+len;
    let transformer_len = byte_to_u32(&transform_mod_code[transformer_index..transformer_index + 4]) as usize;
    let javassist_index = transformer_index+transformer_len+4;
    let javassist_len = byte_to_u32(&transform_mod_code[javassist_index..javassist_index + 4]) as usize;
    let transformer_java_array = env.byte_array_from_slice(&transform_mod_code[transformer_index + 4..javassist_index])
        .unwrap();
    let javassist_java_array = env.byte_array_from_slice(&transform_mod_code[javassist_index + 4..javassist_index+4+javassist_len])
        .unwrap();

    let transformer = env.call_static_method(&loader_class, "decryption", "([B[B)Ljava/lang/Object;",
                                             &[JValue::Object(&transformer_java_array), JValue::Object(&javassist_java_array)])
        .expect("load transformer failed").l().expect("load transformer failed");
    let transformer_class = env.get_object_class(&transformer).unwrap();
    env.register_native_methods(&transformer_class, &[NativeMethod{
        name: JNIString::from("decryptData"),
        sig: JNIString::from("([B)[B"),
        fn_ptr: jni_native_transform as *mut c_void
    }]).unwrap();
    let transformer_ptr: *mut _jobject = transformer.as_raw().cast();
    unsafe {
        TRANSFORMER_OBJ = Some(transformer_ptr);
    }
}

fn set_callbacks(jvm: &JavaVM) {
    unsafe {
        set_file_load_callback(jvm.get_java_vm_pointer(), jg_class_file_load_hook);
    }
}

extern "system" fn jg_class_file_load_hook(
        jvmti_env: *mut c_void,
        jni_env: *mut jni_sys::JNIEnv,
        class_being_redefined: jclass,
        loader: jobject,
        name: *const std::os::raw::c_char,
        protection_domain: jobject,
        class_data_len: jint,
        class_data: *const std::os::raw::c_uchar,
        new_class_data_len: *mut jint,
        new_class_data: *mut *mut std::os::raw::c_uchar,
        ) {
    let name = unsafe { CStr::from_ptr(name) }.to_str().unwrap();//todo
    let class_data_prefix = unsafe {
        std::slice::from_raw_parts(class_data, 5)
    };
    // continue if not decrypt
    if class_data_len <= 4 || class_data_prefix[4] & CLASS_ENCRYPT_FLAG == 0 {
        if name != URL_CLASS_NAME {
            unsafe {
                *new_class_data = class_data as *mut c_uchar;
                *new_class_data_len = class_data_len;
            }
            return;
        }
    }
    let transformer = unsafe { &TRANSFORMER_OBJ };
    if let Some(transformer) = transformer {
        let (mut env, class_data_arr, transformer) = unsafe {
            (
                JNIEnv::from_raw(jni_env).unwrap(),
                CStr::from_ptr(class_data as *const std::os::raw::c_char).to_bytes(),
                JObject::from_raw(jobject::from(*transformer))
            )
        };
        let name_str = env.new_string(name).unwrap();//todo
        let class_data_java_array = env.byte_array_from_slice(class_data_arr).unwrap();//todo
        let (data, len) = match env.call_method(transformer, "decryptClass", "(Ljava/lang/String;[B)[B",
                                                &[JValue::Object(&name_str), JValue::Object(&class_data_java_array)]) {
            Ok(result) => {
                let result_java_array = JByteArray::from(result.l().unwrap());//todo
                // let len = env.get_array_length(&result_java_array).unwrap();//todo
                let result_array = env.convert_byte_array(&result_java_array).unwrap();
                let result_cstr = CStr::from_bytes_with_nul(&result_array).unwrap(); //todo
                // todo call transformer
                // todo check result_cstr.count_bytes() is i32
                (result_cstr.as_ptr() as *const std::os::raw::c_uchar, result_cstr.count_bytes() as jint)
            }
            Err(e) => {
                println!("class file transform failed: {}", e.to_string());
                (class_data, class_data_len)
            }
        };
        unsafe {
            *new_class_data = data as *mut c_uchar;
            *new_class_data_len = len;
        }
    } else {
        unsafe {
            *new_class_data = class_data as *mut c_uchar;
            *new_class_data_len = class_data_len;
        }
    }
}

// #[no_mangle]
extern "system" fn jni_native_transform<'l>(env: &mut JNIEnv<'l>, _class: &JClass<'l>, data: JByteArray<'l>) -> JByteArray<'l> {
    match env.convert_byte_array(&data) {
        Ok(data_arr) => {
            // todo convert
            match env.byte_array_from_slice(&data_arr) {
                Ok(data) => data,
                Err(e) => {
                    println!("ERROR: array convert to java byte array failed: {}", e.to_string());
                    data
                }
            }
        }
        Err(e) => {
            println!("ERROR: java byte array convert to array failed: {}", e.to_string());
            data
        }
    }
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
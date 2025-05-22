use crate::args_parser::LaunchTarget::Jar;
use crate::args_parser::LauncherArg;
use crate::base::common::ENCRYPT_DATA_TAG;
use crate::jvm::jvm_util::JvmWrapper;
use crate::jvm::launcher_helper::{find_launcher_helper_from_env, JvmLauncherHelper};
use crate::util::{aes_util, byte_utils};
use jclass::util::class_scan::fast_scan_class;
use jg_jvmti_wrapper::{jvmti_allocate, set_file_load_callback};
use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::jsize;
use jni::JNIVersion;
use jni::{JNIEnv, JavaVM};
use jni_sys::{jclass as java_class, jint, jlong, jobject, JavaVMInitArgs, JNI_VERSION_1_1};
use std::ffi::c_void;
use std::os::raw::c_uchar;
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
    println!("{result}");
    // ---------------------

    let (jvm, mut env) = wrapper.create_java_vm(init_args).unwrap();
    let vers = env.get_version();
    set_callbacks(&jvm);

    // get JNI env
    // let mut env = jvm.attach_current_thread().expect("get jni env failed");
    // let mut env = jvm.get_env().unwrap();

    let env_ref = &mut env;

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

fn set_callbacks(jvm: &JavaVM) {
    unsafe {
        set_file_load_callback(jvm.get_java_vm_pointer(), jg_class_file_load_hook);
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
    // let name = unsafe { CStr::from_ptr(name) }.to_str().unwrap();//todo
    // let class_data_prefix = unsafe {
    //     std::slice::from_raw_parts(class_data, 5)
    // };
    // // continue if not decrypt
    // if class_data_len <= 4 || class_data_prefix[4] & CLASS_ENCRYPT_FLAG == 0 {
    //     if name != URL_CLASS_NAME {
    //         unsafe {
    //             *new_class_data = class_data as *mut c_uchar;
    //             *new_class_data_len = class_data_len;
    //         }
    //         return;
    //     }
    // }
    let (mut env, class_data_arr) = unsafe {
        (
            JNIEnv::from_raw(jni_env).unwrap(),
            std::slice::from_raw_parts(class_data as *const u8, class_data_len as usize)
        )
    };
    if !try_decrypt_class(jvmti_env, &mut env, class_data_arr, new_class_data_len, new_class_data) {
        unsafe {
            *new_class_data = class_data as *mut c_uchar;
            *new_class_data_len = class_data_len;
        }
    }
}

fn try_decrypt_class(jvmti_env: *mut c_void,
                     env: &mut JNIEnv,
                     class_data_arr: &[u8],
                     new_class_data_len: *mut jint,
                     new_class_data: *mut *mut std::os::raw::c_uchar,) -> bool {
    match fast_scan_class(class_data_arr, ENCRYPT_DATA_TAG, false) {
        Ok(Some(info)) if info.specify_attribute.is_some() => {
            let data_range = info.specify_attribute.unwrap();
            let mut en_data = class_data_arr[data_range.start..data_range.end].to_vec();
            let data = match aes_util::decrypt(&mut en_data) {
                Ok(data) => data,
                Err(err) => {
                    eprintln!("decrypt class data attribute failed: {}", err);
                    return false;
                }
            };

            let mut new_class_data_bytes = Vec::with_capacity(class_data_arr.len());
            // todo decrypt class
            let mut index = 0;
            let mut copied_index = 0;
            loop {
                let start = index;
                index += 2;
                let const_index = byte_utils::be_byte_to_u16(&data[start..index]) as usize;
                let len = match data[index] as char {
                    'I' | 'F' => {
                        4
                    }
                    'L' | 'D' => {
                        8
                    }
                    'S' => {
                        let start = index;
                        index += 4;
                        let len = byte_utils::byte_to_u32(&data[(start + 1)..index + 1]) as usize;
                        len
                    }
                    _ => {
                        index += 1;
                        break
                    }
                };
                index += 1;
                let const_start = info.consts[const_index - 1] + 1;
                let const_end = info.consts[const_index];
                new_class_data_bytes.copy_from_slice(&class_data_arr[copied_index..const_start]);
                copied_index = const_end;
                let start = index;
                index += len;
                new_class_data_bytes.copy_from_slice(&data[start..index]);
                break
            }

            let start = index;
            index += 4;
            let codes_size = byte_utils::be_byte_to_u32(&data[start..index]) as usize;
            for i in 0..codes_size {
                let start = index;
                index += 4;
                let code_len = byte_utils::be_byte_to_u32(&data[start..index]) as usize;
                let code_range = info.method_codes[i];

                let code_start = code_range.0 + 2;
                new_class_data_bytes.copy_from_slice(&class_data_arr[copied_index..code_start]);
                copied_index = code_range.1;
                // let start = index;
                index += code_len;
                new_class_data_bytes.copy_from_slice(&data[start..index]);
            }
            new_class_data_bytes.copy_from_slice(&class_data_arr[copied_index..data_range.start - 4]);
            new_class_data_bytes.copy_from_slice(&[0, 0, 0, 0]);
            new_class_data_bytes.copy_from_slice(&class_data_arr[data_range.end..]);

            let new_class_data_bytes_len = new_class_data_bytes.len();
            let mut new_class_data_ptr = std::ptr::null_mut();
            if 0 == unsafe { jvmti_allocate(jvmti_env, new_class_data_bytes_len as jlong, &mut new_class_data_ptr) } {
                eprintln!("allocate decrypted class data failed");
                return false;
            }
            unsafe {
                std::ptr::copy_nonoverlapping(new_class_data_bytes.as_ptr(), new_class_data_ptr, new_class_data_bytes_len);
                *new_class_data = new_class_data_ptr;
                *new_class_data_len = new_class_data_bytes_len as jint;
            }
            return true;
        }
        Err(err) => {
            eprintln!("analysis class failed: {}", err);
            return false;
        }
        _ => {
            return false;
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
                    eprintln!("ERROR: array convert to java byte array failed: {}", e.to_string());
                    data
                }
            }
        }
        Err(e) => {
            eprintln!("ERROR: java byte array convert to array failed: {}", e.to_string());
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
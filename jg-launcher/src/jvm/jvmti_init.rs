use std::ffi::{c_void, CStr};
use std::process::exit;
use std::ptr::null_mut;
use jni::{sys, JNIEnv, JavaVM, NativeMethod};
use jni::objects::{JByteArray, JObject};
use jni::strings::JNIString;
use jni_sys::{jbyteArray, jint, jlong, jobject};
use ring::aead::chacha20_poly1305_openssh::TAG_LEN;
use ring::aead::NONCE_LEN;
use crate::base::common::{runtime_classes, RESOURCE_DECRYPT_NATIVE_CLASS, RESOURCE_DECRYPT_NATIVE_DESC, RESOURCE_DECRYPT_NATIVE_METHOD, URL_CLASS_NAME};
use crate::jni_result_expect;
use crate::util::aes_util::decrypt_resource;
use crate::util::byte_utils::byte_be_to_u32_fast;
use crate::util::class_util;
use crate::util::class_util::url_extended_processing;
use crate::util::jvm_util::jni_error_handle;
use crate::util::jvmti_util::{get_jvmti_from_vm, init_vm_and_set_callback, jvmti_allocate, jvmti_get_class_loader};

pub fn set_callbacks(jvm: &JavaVM, version: i32) {
    let result = unsafe {
        init_vm_and_set_callback(jvm.get_java_vm_pointer(), jg_class_file_load_hook, version)
    };
    if 0 != result {
        eprintln!("set transformer hook failed");
    }
}

pub fn load_ext_runtime(jvm: &JavaVM, env: &mut JNIEnv) {
    let url_class = jni_result_expect!(env, env.find_class(URL_CLASS_NAME), "url class cannot found!");
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
        let class_obj = jni_result_expect!(env, env.define_class(&name, &class_loader, class_data), "cannot load extend runtime class");

        if &name == RESOURCE_DECRYPT_NATIVE_CLASS {
            let native_method = NativeMethod {
                name: JNIString::from(RESOURCE_DECRYPT_NATIVE_METHOD),
                sig: JNIString::from(RESOURCE_DECRYPT_NATIVE_DESC),
                fn_ptr: resource_decrypt_native as *mut c_void,
            };
            jni_result_expect!(env, env.register_native_methods(class_obj, &[native_method]), "cannot bind ext runtime clas");
        }
    }
}

#[allow(unused)]
extern "system" fn resource_decrypt_native(env: *mut sys::JNIEnv, object: jobject, data: jbyteArray, off: jint, len: jint) -> jint {
    if (len as usize) < NONCE_LEN + TAG_LEN {
        return len;
    }
    let env = match unsafe {
        JNIEnv::from_raw(env)
    } {
        Ok(env) => {
            env
        }
        Err(err) => {
            eprintln!("ERROR: native method: cannot get env: {}", err.to_string());
            return len;
        }
    };
    let data_arr = unsafe { JByteArray::from_raw(data) };
    let mut data_rs = match env.convert_byte_array(&data_arr) {
        Ok(data) => {
            data
        }
        Err(err) => {
            jni_error_handle(&env, &err, "ERROR: native method: cannot convert data");
            // eprintln!("ERROR: native method: cannot convert data: {err}");
            return len;
        }
    };
    let end = (off + len) as usize;
    let off = off as usize;
    match decrypt_resource(&mut data_rs[off..end]) {
        Ok(de_data) => {
            let de_data_len = de_data.len();
            let de_data = unsafe {
                std::slice::from_raw_parts(de_data.as_ptr() as *const i8, de_data_len)
            };

            match env.set_byte_array_region(&data_arr, 0, de_data) {
                Ok(r) => r,
                Err(err) => {
                    jni_error_handle(&env, &err, "");
                    return len;
                }
            }
            de_data_len as jint
        }
        Err(err) => {
            eprintln!("ERROR: native method: decrypt resource failed: {err}");
            len
        }
    }
}

#[allow(unused)]
extern "system" fn jg_class_file_load_hook(
    jvmti_env: *mut c_void,
    jni_env: *mut jni_sys::JNIEnv,
    class_being_redefined: jni_sys::jclass,
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

    if let Some(mut new_class_data_bytes) = class_util::try_decrypt_class(class_data_arr) {
        if is_url {
            if let Some(extended_class_data) = url_extended_processing(&new_class_data_bytes) {
                new_class_data_bytes = extended_class_data;
            }
        }
        set_new_class_data(jvmti_env, &new_class_data_bytes, new_class_data_len, new_class_data);
    } else if is_url {
        if let Some(extended_class_data) = url_extended_processing(class_data_arr) {
            set_new_class_data(jvmti_env, &extended_class_data, new_class_data_len, new_class_data);
        }
    }

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
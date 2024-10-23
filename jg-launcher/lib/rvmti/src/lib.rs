extern crate core;

use std::{mem, ptr, slice};
use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_uchar, c_void};

use jni::sys::*;

use crate::bindings::*;
use crate::callback::JvmtiEventCallbacks;

pub mod sync;
pub mod bindings;
pub mod callback;

pub type JvmtiResult<T> = Result<T, jvmtiError>;

macro_rules! jvmti_unchecked {
    ($jvmti:expr, $name:tt $(, $args:expr)*) => {
        unsafe {
            (**$jvmti.internal).$name.unwrap()($jvmti.internal, $($args),*)
        }
    };
}

fn to_string(ptr: *const c_char) -> String {
    unsafe {
        let c_str = CStr::from_ptr(ptr);
        c_str.to_str().unwrap().to_string()
    }
}

fn as_slice<'a, T>(count: i32, ptr: *const T) -> &'a [T] {
    unsafe {
        let size = count as usize;

        slice::from_raw_parts(ptr, size)
    }
}

fn as_c_char(name: &str) -> *const c_char {
    let c_str = CString::new(name).unwrap();

    unsafe {
        slice::from_raw_parts(c_str.as_ptr(), name.len() + 1).to_vec().as_ptr()
    }
}

fn none<T>() -> T {
    unsafe {
        mem::zeroed()
    }
}

pub struct JvmtiEnv {
    pub internal: *mut jvmtiEnv,
}

impl JvmtiEnv {
    pub fn set_event_notification_mode(&self, event_type: jvmtiEvent, event_thread: jthread, enabled: bool) -> JvmtiResult<()> {
        let mode = if enabled {
            jvmtiEventMode::JVMTI_ENABLE
        } else {
            jvmtiEventMode::JVMTI_DISABLE
        };
        jvmti_unchecked!(self, SetEventNotificationMode, mode, event_type, event_thread).value(|| { () })
    }

    pub fn get_all_threads(&self) -> JvmtiResult<&[jthread]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;
        let mut threads: *mut jthread = none();
        let threads_ptr: *mut *mut jthread = &mut threads;

        let error = jvmti_unchecked!(self, GetAllThreads, count_ptr, threads_ptr);

        error.value(|| { as_slice(count, threads) })
    }

    pub fn suspend_thread(&self, thread: jthread) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SuspendThread, thread).value(|| { () })
    }

    pub fn resume_thread(&self, thread: jthread) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ResumeThread, thread).value(|| { () })
    }

    pub fn stop_thread(&self, thread: jthread, exception: jobject) -> JvmtiResult<()> {
        jvmti_unchecked!(self, StopThread, thread, exception).value(|| { () })
    }

    pub fn interrupt_thread(&self, thread: jthread) -> JvmtiResult<()> {
        jvmti_unchecked!(self, InterruptThread, thread).value(|| { () })
    }

    pub fn get_thread_info(&self, thread: jthread) -> JvmtiResult<jvmtiThreadInfo> {
        let mut info: jvmtiThreadInfo = none();
        let info_ptr: *mut jvmtiThreadInfo = &mut info;

        jvmti_unchecked!(self, GetThreadInfo, thread, info_ptr).value(|| { info })
    }

    pub fn get_owned_monitor_info(&self, thread: jthread) -> JvmtiResult<&[jobject]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;
        let mut monitors: *mut jobject = none();
        let monitors_ptr: *mut *mut jobject = &mut monitors;

        let error = jvmti_unchecked!(self, GetOwnedMonitorInfo, thread, count_ptr, monitors_ptr);

        error.value(|| { as_slice(count, monitors) })
    }

    pub fn get_current_contended_monitor(&self, thread: jthread) -> JvmtiResult<jobject> {
        let mut monitor: jobject = none();
        let monitor_ptr: *mut jobject = &mut monitor;

        jvmti_unchecked!(self, GetCurrentContendedMonitor, thread, monitor_ptr).value(|| { monitor })
    }

    pub fn run_agent_thread(&self, thread: jthread, _proc: jvmtiStartFunction, arg: *const c_void, priority: jint) -> JvmtiResult<()> {
        jvmti_unchecked!(self, RunAgentThread, thread, _proc, arg, priority).value(|| { () })
    }

    pub fn get_top_thread_groups(&self) -> JvmtiResult<&[jthreadGroup]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;
        let mut groups: *mut jthreadGroup = none();
        let groups_ptr: *mut *mut jthreadGroup = &mut groups;

        let error = jvmti_unchecked!(self, GetTopThreadGroups, count_ptr, groups_ptr);

        error.value(|| { as_slice(count, groups) })
    }

    pub fn get_thread_group_info(&self, group: jthreadGroup) -> JvmtiResult<jvmtiThreadGroupInfo> {
        let mut info: jvmtiThreadGroupInfo = none();
        let info_ptr: *mut jvmtiThreadGroupInfo = &mut info;

        jvmti_unchecked!(self, GetThreadGroupInfo, group, info_ptr).value(|| { info })
    }

    pub fn get_thread_group_children(&self, group: jthreadGroup) -> JvmtiResult<(&[jthread], &[jthreadGroup])> {
        let mut thread_count: i32 = none();
        let thread_count_ptr: *mut i32 = &mut thread_count;
        let mut threads: *mut jthread = none();
        let threads_ptr: *mut *mut jthread = &mut threads;

        let mut group_count: i32 = none();
        let group_count_ptr: *mut i32 = &mut group_count;
        let mut groups: *mut jthreadGroup = none();
        let groups_ptr: *mut *mut jthreadGroup = &mut groups;

        let error = jvmti_unchecked!(self, GetThreadGroupChildren,group, thread_count_ptr, threads_ptr, group_count_ptr, groups_ptr);

        error.value(|| { (as_slice(thread_count, threads), as_slice(group_count, groups)) })
    }

    pub fn get_frame_count(&self, thread: jthread) -> JvmtiResult<jint> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        jvmti_unchecked!(self, GetFrameCount ,thread, count_ptr).value(|| { count })
    }

    pub fn get_thread_state(&self, thread: jthread) -> JvmtiResult<jint> {
        let mut state: i32 = none();
        let state_ptr: *mut i32 = &mut state;

        jvmti_unchecked!(self, GetThreadState, thread, state_ptr).value(|| { state })
    }

    pub fn get_current_thread(&self) -> JvmtiResult<jthread> {
        let mut thread: jthread = none();
        let thread_ptr: *mut jthread = &mut thread;

        jvmti_unchecked!(self, GetCurrentThread, thread_ptr).value(|| { thread })
    }

    pub fn get_frame_location(&self, thread: jthread, depth: jint) -> JvmtiResult<(jmethodID, jlocation)> {
        let mut method: jmethodID = none();
        let method_ptr: *mut jmethodID = &mut method;
        let mut location: jlocation = none();
        let location_ptr: *mut jlocation = &mut location;

        jvmti_unchecked!(self, GetFrameLocation, thread, depth, method_ptr, location_ptr).value(|| { (method, location) })
    }

    pub fn notify_frame_pop(&self, thread: jthread, depth: jint) -> JvmtiResult<()> {
        jvmti_unchecked!(self, NotifyFramePop, thread, depth).value(|| {})
    }

    pub fn get_local_object(&self, thread: jthread, depth: jint, slot: jint) -> JvmtiResult<jobject> {
        let mut object: jobject = none();
        let object_ptr: *mut jobject = &mut object;

        jvmti_unchecked!(self, GetLocalObject, thread, depth, slot, object_ptr).value(|| { object })
    }

    pub fn get_local_int(&self, thread: jthread, depth: jint, slot: jint) -> JvmtiResult<jint> {
        let mut int: jint = none();
        let int_ptr: *mut jint = &mut int;

        jvmti_unchecked!(self, GetLocalInt, thread, depth, slot, int_ptr).value(|| { int })
    }

    pub fn get_local_long(&self, thread: jthread, depth: jint, slot: jint) -> JvmtiResult<jlong> {
        let mut long: jlong = none();
        let long_ptr: *mut jlong = &mut long;

        jvmti_unchecked!(self, GetLocalLong, thread, depth, slot, long_ptr).value(|| { long })
    }

    pub fn get_local_float(&self, thread: jthread, depth: jint, slot: jint) -> JvmtiResult<jfloat> {
        let mut float: jfloat = none();
        let float_ptr: *mut jfloat = &mut float;

        jvmti_unchecked!(self, GetLocalFloat, thread, depth, slot, float_ptr).value(|| { float })
    }

    pub fn get_local_double(&self, thread: jthread, depth: jint, slot: jint) -> JvmtiResult<jdouble> {
        let mut double: jdouble = none();
        let double_ptr: *mut jdouble = &mut double;

        jvmti_unchecked!(self, GetLocalDouble, thread, depth, slot, double_ptr).value(|| { double })
    }

    pub fn create_raw_monitor(&self, name: &str) -> JvmtiResult<jrawMonitorID> {
        let mut monitor: jrawMonitorID = none();
        let monitor_ptr: *mut jrawMonitorID = &mut monitor;

        let name_ptr = as_c_char(name);

        jvmti_unchecked!(self, CreateRawMonitor, name_ptr, monitor_ptr).value(|| { monitor })
    }

    pub fn destroy_raw_monitor(&self, monitor: jrawMonitorID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, DestroyRawMonitor, monitor).value(|| { () })
    }

    pub fn raw_monitor_enter(&self, monitor: jrawMonitorID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, RawMonitorEnter, monitor).value(|| { () })
    }

    pub fn raw_monitor_exit(&self, monitor: jrawMonitorID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, RawMonitorExit, monitor).value(|| { () })
    }

    pub fn raw_monitor_wait(&self, monitor: jrawMonitorID, millis: jlong) -> JvmtiResult<()> {
        jvmti_unchecked!(self, RawMonitorWait, monitor, millis).value(|| { () })
    }

    pub fn raw_monitor_notify(&self, monitor: jrawMonitorID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, RawMonitorNotify, monitor).value(|| { () })
    }

    pub fn raw_monitor_notify_all(&self, monitor: jrawMonitorID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, RawMonitorNotifyAll, monitor).value(|| { () })
    }

    pub fn set_break_point(&self, method: jmethodID, location: jlocation) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetBreakpoint, method, location).value(|| { () })
    }

    pub fn clear_breakpoint(&self, method: jmethodID, location: jlocation) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ClearBreakpoint, method, location).value(|| { () })
    }

    pub fn set_field_access_watch(&self, class: jclass, field: jfieldID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetFieldAccessWatch, class, field).value(|| { () })
    }

    pub fn clear_field_access_watch(&self, class: jclass, field: jfieldID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ClearFieldAccessWatch, class, field).value(|| { () })
    }

    pub fn set_field_modification_watch(&self, class: jclass, field: jfieldID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetFieldModificationWatch, class, field).value(|| { () })
    }

    pub fn clear_field_modification_watch(&self, class: jclass, field: jfieldID) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ClearFieldModificationWatch, class, field).value(|| { () })
    }

    pub fn is_modifiable_class(&self, class: jclass) -> JvmtiResult<bool> {
        let mut modifiable: jboolean = none();
        let modifiable_ptr: *mut jboolean = &mut modifiable;

        jvmti_unchecked!(self, IsModifiableClass, class, modifiable_ptr).value(|| { modifiable == 1 })
    }

    pub fn allocate(&self, size: jlong) -> JvmtiResult<*mut c_uchar> {
        let mut mem: *mut c_uchar = none();
        let mem_ptr: *mut *mut c_uchar = &mut mem;

        jvmti_unchecked!(self, Allocate, size, mem_ptr).value(|| { mem })
    }

    pub fn deallocate(&self, mem: *mut c_uchar) -> JvmtiResult<()> {
        jvmti_unchecked!(self, Deallocate, mem).value(|| { () })
    }

    /// Returns name, generic
    pub fn get_class_signature(&self, class: jclass) -> JvmtiResult<(String, Option<String>)> {
        let mut signature: *mut c_char = none();
        let signature_ptr: *mut *mut c_char = &mut signature;
        let mut generic: *mut c_char = none();
        let generic_ptr: *mut *mut c_char = &mut generic;

        let error = jvmti_unchecked!(self, GetClassSignature, class, signature_ptr, generic_ptr);

        let generic_option = unsafe {
            match generic.as_ref() {
                Some(_) => { Some(to_string(generic)) }
                None => { None }
            }
        };

        error.value(|| { (to_string(signature), generic_option) })
    }

    pub fn get_class_status(&self, class: jclass) -> JvmtiResult<jint> {
        let mut status: jint = none();
        let status_ptr: *mut jint = &mut status;

        jvmti_unchecked!(self,GetClassStatus , class, status_ptr).value(|| { status })
    }

    pub fn get_source_file_name(&self, class: jclass) -> JvmtiResult<String> {
        let mut name: *mut c_char = none();
        let name_ptr: *mut *mut c_char = &mut name;

        jvmti_unchecked!(self, GetSourceFileName, class, name_ptr).value(|| { to_string(name) })
    }

    pub fn get_class_modifiers(&self, class: jclass) -> JvmtiResult<jint> {
        let mut modifiers: jint = none();
        let modifiers_ptr: *mut jint = &mut modifiers;

        jvmti_unchecked!(self, GetClassModifiers, class, modifiers_ptr).value(|| { modifiers })
    }

    pub fn get_class_methods(&self, class: jclass) -> JvmtiResult<&[jmethodID]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut methods: *mut jmethodID = none();
        let methods_ptr: *mut *mut jmethodID = &mut methods;

        jvmti_unchecked!(self, GetClassMethods, class, count_ptr, methods_ptr).value(|| { as_slice(count, methods) })
    }

    pub fn get_class_fields(&self, class: jclass) -> JvmtiResult<&[jfieldID]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut fields: *mut jfieldID = none();
        let fields_ptr: *mut *mut jfieldID = &mut fields;


        jvmti_unchecked!(self, GetClassFields, class, count_ptr, fields_ptr).value(|| { as_slice(count, fields) })
    }

    pub fn get_implemented_interfaces(&self, class: jclass) -> JvmtiResult<&[jclass]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut interfaces: *mut jclass = ptr::null_mut();
        let interfaces_ptr: *mut *mut jclass = &mut interfaces;


        jvmti_unchecked!(self, GetImplementedInterfaces, class, count_ptr, interfaces_ptr).value(|| { as_slice(count, interfaces) })
    }

    pub fn is_interface(&self, class: jclass) -> JvmtiResult<bool> {
        let mut is_interface: jboolean = none();
        let is_interface_ptr: *mut jboolean = &mut is_interface;

        jvmti_unchecked!(self, IsInterface, class, is_interface_ptr).value(|| { is_interface == 1 })
    }

    pub fn is_array_class(&self, class: jclass) -> JvmtiResult<bool> {
        let mut is_array: jboolean = none();
        let is_array_ptr: *mut jboolean = &mut is_array;

        jvmti_unchecked!(self, IsArrayClass, class, is_array_ptr).value(|| { is_array == 1 })
    }

    pub fn get_class_loader(&self, class: jclass) -> JvmtiResult<jobject> {
        let mut loader: jobject = none();
        let loader_ptr: *mut jobject = &mut loader;

        jvmti_unchecked!(self, GetClassLoader, class, loader_ptr).value(|| { loader })
    }

    pub fn get_object_hash_code(&self, object: jobject) -> JvmtiResult<jint> {
        let mut hashcode: jint = none();
        let hashcode_ptr: *mut jint = &mut hashcode;

        jvmti_unchecked!(self, GetObjectHashCode, object, hashcode_ptr).value(|| { hashcode })
    }

    pub fn get_object_monitor_usage(&self, object: jobject) -> JvmtiResult<jvmtiMonitorUsage> {
        let mut usage: jvmtiMonitorUsage = none();
        let usage_ptr: *mut jvmtiMonitorUsage = &mut usage;

        jvmti_unchecked!(self, GetObjectMonitorUsage, object, usage_ptr).value(|| { usage })
    }

    /// Returns name, signature, generic
    pub fn get_field_name(&self, class: jclass, field: jfieldID) -> JvmtiResult<(String, String, Option<String>)> {
        let mut name: *mut c_char = none();
        let name_ptr: *mut *mut c_char = &mut name;

        let mut signature: *mut c_char = none();
        let signature_ptr: *mut *mut c_char = &mut signature;

        let mut generic: *mut c_char = none();
        let generic_ptr: *mut *mut c_char = &mut generic;

        let error = jvmti_unchecked!(self, GetFieldName, class, field, name_ptr, signature_ptr, generic_ptr);

        let generic_option = unsafe {
            match generic.as_ref() {
                None => { None }
                Some(_) => { Some(to_string(generic)) }
            }
        };

        error.value(|| { (to_string(name), to_string(signature), generic_option) })
    }

    pub fn get_field_declaring_class(&self, class: jclass, field: jfieldID) -> JvmtiResult<jclass> {
        let mut declaring: jclass = none();
        let declaring_ptr: *mut jclass = &mut declaring;

        jvmti_unchecked!(self, GetFieldDeclaringClass, class, field, declaring_ptr).value(|| { declaring })
    }

    pub fn get_field_modifiers(&self, class: jclass, field: jfieldID) -> JvmtiResult<jint> {
        let mut modifiers: jint = none();
        let modifiers_ptr: *mut jint = &mut modifiers;

        jvmti_unchecked!(self, GetFieldModifiers, class, field, modifiers_ptr).value(|| { modifiers })
    }

    pub fn is_field_synthetic(&self, class: jclass, field: jfieldID) -> JvmtiResult<bool> {
        let mut is_synthetic: jboolean = none();
        let is_synthetic_ptr: *mut jboolean = &mut is_synthetic;

        jvmti_unchecked!(self, IsFieldSynthetic, class, field, is_synthetic_ptr).value(|| { is_synthetic == 1 })
    }

    /// Returns name, signature, generic
    pub fn get_method_name(&self, method: jmethodID) -> JvmtiResult<(String, String, Option<String>)> {
        let mut name: *mut c_char = none();
        let name_ptr: *mut *mut c_char = &mut name;

        let mut signature: *mut c_char = none();
        let signature_ptr: *mut *mut c_char = &mut signature;

        let mut generic: *mut c_char = none();
        let generic_ptr: *mut *mut c_char = &mut generic;

        let error = jvmti_unchecked!(self, GetMethodName, method, name_ptr, signature_ptr, generic_ptr);

        let generic_option = unsafe {
            match generic.as_ref() {
                None => { None }
                Some(_) => { Some(to_string(generic)) }
            }
        };

        error.value(|| { (to_string(name), to_string(signature), generic_option) })
    }

    pub fn get_method_declaring_class(&self, method: jmethodID) -> JvmtiResult<jclass> {
        let mut declaring: jclass = none();
        let declaring_ptr: *mut jclass = &mut declaring;

        jvmti_unchecked!(self, GetMethodDeclaringClass, method, declaring_ptr).value(|| { declaring })
    }

    pub fn get_method_modifiers(&self, method: jmethodID) -> JvmtiResult<jint> {
        let mut modifiers: jint = none();
        let modifiers_ptr: *mut jint = &mut modifiers;

        jvmti_unchecked!(self, GetMethodModifiers, method, modifiers_ptr).value(|| { modifiers })
    }

    pub fn get_max_locals(&self, method: jmethodID) -> JvmtiResult<jint> {
        let mut max: jint = none();
        let max_ptr: *mut jint = &mut max;

        jvmti_unchecked!(self, GetMaxLocals, method, max_ptr).value(|| { max })
    }

    pub fn get_arguments_size(&self, method: jmethodID) -> JvmtiResult<jint> {
        let mut size: jint = none();
        let size_ptr: *mut jint = &mut size;

        jvmti_unchecked!(self, GetArgumentsSize, method, size_ptr).value(|| { size })
    }

    pub fn get_line_number_table(&self, method: jmethodID) -> JvmtiResult<&[jvmtiLineNumberEntry]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut table: *mut jvmtiLineNumberEntry = ptr::null_mut();
        let table_ptr: *mut *mut jvmtiLineNumberEntry = &mut table;

        jvmti_unchecked!(self, GetLineNumberTable, method, count_ptr, table_ptr).value(|| { as_slice(count, table) })
    }

    /// Returns start location, end location
    pub fn get_method_location(&self, method: jmethodID) -> JvmtiResult<(jlocation, jlocation)> {
        let mut start: jlocation = none();
        let start_ptr: *mut jlocation = &mut start;

        let mut end: jlocation = 0;
        let end_ptr: *mut jlocation = &mut end;

        jvmti_unchecked!(self, GetMethodLocation,method, start_ptr, end_ptr).value(|| { (start, end) })
    }

    pub fn get_local_variable_table(&self, method: jmethodID) -> JvmtiResult<&[jvmtiLocalVariableEntry]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut table: *mut jvmtiLocalVariableEntry = none();
        let table_ptr: *mut *mut jvmtiLocalVariableEntry = &mut table;

        jvmti_unchecked!(self, GetLocalVariableTable, method, count_ptr, table_ptr).value(|| { as_slice(count, table) })
    }

    pub fn set_native_method_prefix(&self, prefix: &str) -> JvmtiResult<()> {
        let prefix_ptr = as_c_char(prefix);
        jvmti_unchecked!(self, SetNativeMethodPrefix, prefix_ptr).value(|| { () })
    }

    pub fn set_native_method_prefixes(&self, prefixes: &[&str]) -> JvmtiResult<()> {
        let count = prefixes.len() as i32;
        let mut vec: Vec<*mut c_char> = Vec::new();

        for &x in prefixes {
            vec.push(as_c_char(x) as *mut c_char)
        }
        jvmti_unchecked!(self, SetNativeMethodPrefixes, count, vec.as_mut_ptr()).value(|| { () })
    }

    pub fn get_bytecodes(&self, method: jmethodID) -> JvmtiResult<&[c_uchar]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut bytecodes: *mut c_uchar = ptr::null_mut();
        let bytecodes_ptr: *mut *mut c_uchar = &mut bytecodes;

        jvmti_unchecked!(self, GetBytecodes, method, count_ptr, bytecodes_ptr).value(|| { as_slice(count, bytecodes) })
    }

    pub fn is_method_native(&self, method: jmethodID) -> JvmtiResult<bool> {
        let mut is_native: jboolean = none();
        let is_native_ptr: *mut jboolean = &mut is_native;

        jvmti_unchecked!(self, IsMethodNative, method, is_native_ptr).value(|| { is_native == 1 })
    }

    pub fn is_method_synthetic(&self, method: jmethodID) -> JvmtiResult<bool> {
        let mut is_synthetic: jboolean = none();
        let is_synthetic_ptr: *mut jboolean = &mut is_synthetic;

        jvmti_unchecked!(self, IsMethodSynthetic, method, is_synthetic_ptr).value(|| { is_synthetic == 0 })
    }

    pub fn get_loaded_classes(&self) -> JvmtiResult<&[jclass]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut classes: *mut jclass = none();
        let classes_ptr: *mut *mut jclass = &mut classes;

        jvmti_unchecked!(self, GetLoadedClasses, count_ptr, classes_ptr).value(|| { as_slice(count, classes) })
    }

    pub fn get_class_loader_classes(&self, initiating_loader: jobject) -> JvmtiResult<&[jclass]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut classes: *mut jclass = none();
        let classes_ptr: *mut *mut jclass = &mut classes;

        jvmti_unchecked!(self, GetClassLoaderClasses, initiating_loader, count_ptr, classes_ptr).value(|| { as_slice(count, classes) })
    }

    pub fn pop_frame(&self, thread: jthread) -> JvmtiResult<()> {
        jvmti_unchecked!(self, PopFrame, thread).value(|| { () })
    }

    pub fn force_early_return_object(&self, thread: jthread, value: jobject) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ForceEarlyReturnObject, thread, value).value(|| { () })
    }

    pub fn force_early_return_int(&self, thread: jthread, value: jint) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ForceEarlyReturnInt, thread, value).value(|| { () })
    }

    pub fn force_early_return_long(&self, thread: jthread, value: jlong) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ForceEarlyReturnLong, thread, value).value(|| { () })
    }

    pub fn force_early_return_float(&self, thread: jthread, value: jfloat) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ForceEarlyReturnFloat, thread, value).value(|| { () })
    }

    pub fn force_early_return_double(&self, thread: jthread, value: jdouble) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ForceEarlyReturnDouble, thread, value).value(|| { () })
    }

    pub fn force_early_return_void(&self, thread: jthread) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ForceEarlyReturnVoid, thread).value(|| { () })
    }

    pub fn redefine_classes(&self, class_definitions: Vec<jvmtiClassDefinition>) -> JvmtiResult<()> {
        let count = class_definitions.len() as i32;
        let ptr = class_definitions.as_ptr();

        jvmti_unchecked!(self, RedefineClasses, count, ptr).value(|| { () })
    }

    pub fn get_version_number(&self) -> JvmtiResult<jint> {
        let mut version: jint = none();
        let version_ptr: *mut jint = &mut version;

        jvmti_unchecked!(self, GetVersionNumber, version_ptr).value(|| { version })
    }

    pub fn get_capabilities(&self) -> JvmtiResult<jvmtiCapabilities> {
        let mut capabilities: jvmtiCapabilities = none();
        let capabilities_ptr: *mut jvmtiCapabilities = &mut capabilities;

        jvmti_unchecked!(self, GetCapabilities, capabilities_ptr).value(|| { capabilities })
    }

    pub fn get_source_debug_extension(&self, class: jclass) -> JvmtiResult<String> {
        let mut extension: *mut c_char = none();
        let extension_ptr: *mut *mut c_char = &mut extension;

        jvmti_unchecked!(self, GetSourceDebugExtension, class, extension_ptr).value(|| { to_string(extension) })
    }

    pub fn is_method_obsolete(&self, method: jmethodID) -> JvmtiResult<bool> {
        let mut is_obsolete: jboolean = none();
        let is_obsolete_ptr: *mut jboolean = &mut is_obsolete;

        jvmti_unchecked!(self, IsMethodObsolete, method, is_obsolete_ptr).value(|| { is_obsolete == 1 })
    }

    pub fn suspend_thread_list(&self, request_list: Vec<jthread>) -> JvmtiResult<jvmtiError> {
        let count = request_list.len() as i32;
        let ptr = request_list.as_ptr();

        let mut results: jvmtiError = none();
        let results_ptr: *mut jvmtiError = &mut results;

        jvmti_unchecked!(self, SuspendThreadList, count, ptr, results_ptr).value(|| { results })
    }

    pub fn resume_thread_list(&self, request_list: Vec<jthread>) -> JvmtiResult<jvmtiError> {
        let count = request_list.len() as i32;
        let ptr = request_list.as_ptr();

        let mut results: jvmtiError = none();
        let results_ptr: *mut jvmtiError = &mut results;

        jvmti_unchecked!(self, ResumeThreadList, count, ptr, results_ptr).value(|| { results })
    }

    pub fn get_all_stack_traces(&self, max_frame_count: i32) -> JvmtiResult<&[jvmtiStackInfo]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut stack_info: *mut jvmtiStackInfo = none();
        let stack_info_ptr: *mut *mut jvmtiStackInfo = &mut stack_info;

        jvmti_unchecked!(self, GetAllStackTraces, max_frame_count, stack_info_ptr, count_ptr).value(|| { as_slice(count, stack_info) })
    }

    pub fn get_thread_list_stack_traces(&self, thread_list: Vec<jthread>, max_frame_count: i32) -> JvmtiResult<*mut jvmtiStackInfo> {
        let count = thread_list.len() as i32;
        let ptr = thread_list.as_ptr();

        let mut stack_info: *mut jvmtiStackInfo = none();
        let stack_info_ptr: *mut *mut jvmtiStackInfo = &mut stack_info;

        jvmti_unchecked!(self, GetThreadListStackTraces, count, ptr, max_frame_count, stack_info_ptr).value(|| { stack_info })
    }

    pub fn get_thread_local_storage(&self, thread: jthread) -> JvmtiResult<*mut c_void> {
        let mut data: *mut c_void = none();
        let data_ptr: *mut *mut c_void = &mut data;

        jvmti_unchecked!(self, GetThreadLocalStorage, thread, data_ptr).value(|| { data })
    }

    pub fn set_thread_local_storage(&self, thread: jthread, data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetThreadLocalStorage, thread, data).value(|| { () })
    }

    pub fn get_stack_trace(&self) {
        panic!("Not implemented yet")
    }

    pub fn get_tag(&self, object: jobject) -> JvmtiResult<jlong> {
        let mut tag: jlong = none();
        let tag_ptr: *mut jlong = &mut tag;

        jvmti_unchecked!(self, GetTag, object, tag_ptr).value(|| { tag })
    }

    pub fn set_tag(&self, object: jobject, tag: jlong) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetTag, object, tag).value(|| { () })
    }

    pub fn force_garbage_collection(&self) -> JvmtiResult<()> {
        jvmti_unchecked!(self, ForceGarbageCollection).value(|| { () })
    }

    pub fn iterate_over_objects_reachable_from_object(&self, object: jobject, object_reference_callback: jvmtiObjectReferenceCallback, user_data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, IterateOverObjectsReachableFromObject, object, object_reference_callback, user_data).value(|| { () })
    }

    pub fn iterate_over_reachable_objects(&self, heap_root_callback: jvmtiHeapRootCallback, stack_ref_callback: jvmtiStackReferenceCallback, object_ref_callback: jvmtiObjectReferenceCallback, user_data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, IterateOverReachableObjects, heap_root_callback, stack_ref_callback, object_ref_callback, user_data).value(|| { () })
    }

    pub fn iterate_over_heap(&self, object_filter: jvmtiHeapObjectFilter, heap_object_callback: jvmtiHeapObjectCallback, user_data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, IterateOverHeap, object_filter, heap_object_callback, user_data).value(|| { () })
    }

    pub fn iterate_over_instances_of_class(&self, class: jclass, object_filter: jvmtiHeapObjectFilter, heap_object_callback: jvmtiHeapObjectCallback, user_data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, IterateOverInstancesOfClass, class, object_filter, heap_object_callback, user_data).value(|| { () })
    }

    pub fn get_objects_with_tags(&self, tags: Vec<jlong>) -> JvmtiResult<Vec<(jobject, jlong)>> {
        let count = tags.len() as i32;
        let ptr = tags.as_ptr();

        let mut result_count: i32 = none();
        let result_count_ptr: *mut i32 = &mut result_count;

        let mut object_result: *mut jobject = none();
        let object_result_ptr: *mut *mut jobject = &mut object_result;

        let mut tag_result: *mut jlong = none();
        let tag_result_ptr: *mut *mut jlong = &mut tag_result;

        let error = jvmti_unchecked!(self, GetObjectsWithTags, count, ptr, result_count_ptr, object_result_ptr, tag_result_ptr);

        let object_vec = as_slice(result_count, object_result);
        let tag_vec = as_slice(result_count, tag_result);
        let mut result: Vec<(jobject, jlong)> = Vec::new();

        for i in 0..result_count - 1 {
            let index = i as usize;
            let object = object_vec[index];
            let tag = tag_vec[index];

            result.push((object, tag));
        }

        error.value(|| { result })
    }

    pub fn follow_references(&self, heap_filter: i32, class: jclass, initial_object: jobject, callbacks: *const jvmtiHeapCallbacks, user_data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, FollowReferences, heap_filter, class, initial_object, callbacks, user_data).value(|| { () })
    }

    pub fn iterate_through_heap(&self, heap_filter: jint, class: jclass, callbacks: *const jvmtiHeapCallbacks, user_data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, IterateThroughHeap, heap_filter, class, callbacks, user_data).value(|| { () })
    }

    pub fn set_jni_function_table(&self, function_table: *const JNINativeInterface_) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetJNIFunctionTable, function_table).value(|| { () })
    }

    pub fn get_jni_function_table(&self) -> JvmtiResult<*mut JNINativeInterface_> {
        let mut function_table: *mut JNINativeInterface_ = none();
        let function_table_ptr: *mut *mut JNINativeInterface_ = &mut function_table;

        jvmti_unchecked!(self, GetJNIFunctionTable, function_table_ptr).value(|| { function_table })
    }

    pub fn set_event_callbacks(&self, callback: JvmtiEventCallbacks) -> JvmtiResult<()> {
        let ptr: *const jvmtiEventCallbacks = &callback.to_raw();
        let size = mem::size_of::<jvmtiEventCallbacks>();
        jvmti_unchecked!(self, SetEventCallbacks, ptr, size as i32).value(|| { () })
    }

    pub fn generate_events(&self, event_type: jvmtiEvent) -> JvmtiResult<()> {
        jvmti_unchecked!(self, GenerateEvents, event_type).value(|| { () })
    }

    pub fn get_extension_functions(&self) -> JvmtiResult<&[jvmtiExtensionFunctionInfo]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut extensions: *mut jvmtiExtensionFunctionInfo = ptr::null_mut();
        let extensions_ptr: *mut *mut jvmtiExtensionFunctionInfo = &mut extensions;


        jvmti_unchecked!(self, GetExtensionFunctions, count_ptr, extensions_ptr).value(|| { as_slice(count, extensions) })
    }

    pub fn get_extension_events(&self) -> JvmtiResult<&[jvmtiExtensionEventInfo]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut extensions: *mut jvmtiExtensionEventInfo = ptr::null_mut();
        let extensions_ptr: *mut *mut jvmtiExtensionEventInfo = &mut extensions;


        jvmti_unchecked!(self, GetExtensionEvents, count_ptr, extensions_ptr).value(|| { as_slice(count, extensions) })
    }

    pub fn set_extension_event_callback(&self, extension_event_index: i32, callback: jvmtiExtensionEvent) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetExtensionEventCallback, extension_event_index, callback).value(|| { () })
    }

    pub fn dispose_environment(&self) -> JvmtiResult<()> {
        jvmti_unchecked!(self, DisposeEnvironment).value(|| { () })
    }

    pub fn get_error_name(&self, error: jvmtiError) -> JvmtiResult<String> {
        let mut name: *mut c_char = none();
        let name_ptr: *mut *mut c_char = &mut name;


        jvmti_unchecked!(self, GetErrorName, error, name_ptr).value(|| { to_string(name) })
    }

    pub fn get_jlocation_format(&self) -> JvmtiResult<jvmtiJlocationFormat> {
        let mut format: jvmtiJlocationFormat = none();
        let format_ptr: *mut jvmtiJlocationFormat = &mut format;

        jvmti_unchecked!(self, GetJLocationFormat, format_ptr).value(|| { format })
    }

    pub fn get_system_properties(&self) -> JvmtiResult<()> {
        panic!("Not yet implemented")
    }

    pub fn get_system_property(&self, property: &str) -> JvmtiResult<String> {
        let mut value: *mut c_char = none();
        let value_ptr: *mut *mut c_char = &mut value;

        jvmti_unchecked!(self, GetSystemProperty, as_c_char(property), value_ptr).value(|| { to_string(value) })
    }

    pub fn set_system_property(&self, property: &str, value: &str) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetSystemProperty, as_c_char(property), as_c_char(value)).value(|| { () })
    }

    pub fn get_phase(&self) -> JvmtiResult<jvmtiPhase> {
        let mut phase: jvmtiPhase = none();
        let phase_ptr: *mut jvmtiPhase = &mut phase;

        jvmti_unchecked!(self, GetPhase, phase_ptr).value(|| { phase })
    }

    pub fn get_current_thread_cpu_timer_info(&self) -> JvmtiResult<jvmtiTimerInfo> {
        let mut info: jvmtiTimerInfo = none();
        let info_ptr: *mut jvmtiTimerInfo = &mut info;

        jvmti_unchecked!(self, GetCurrentThreadCpuTimerInfo, info_ptr).value(|| { info })
    }

    pub fn get_current_cpu_time(&self) -> JvmtiResult<jlong> {
        let mut nanos: jlong = none();
        let nanos_ptr: *mut jlong = &mut nanos;

        jvmti_unchecked!(self, GetCurrentThreadCpuTime, nanos_ptr).value(|| { nanos })
    }

    pub fn get_thread_cpu_timer_info(&self) -> JvmtiResult<jvmtiTimerInfo> {
        let mut info: jvmtiTimerInfo = none();
        let info_ptr: *mut jvmtiTimerInfo = &mut info;

        jvmti_unchecked!(self, GetThreadCpuTimerInfo, info_ptr).value(|| { info })
    }

    pub fn get_thread_cpu_time(&self, thread: jthread) -> JvmtiResult<jlong> {
        let mut nanos: jlong = none();
        let nanos_ptr: *mut jlong = &mut nanos;

        jvmti_unchecked!(self, GetThreadCpuTime, thread, nanos_ptr).value(|| { nanos })
    }

    pub fn get_timer_info(&self) -> JvmtiResult<jvmtiTimerInfo> {
        let mut info: jvmtiTimerInfo = none();
        let info_ptr: *mut jvmtiTimerInfo = &mut info;

        jvmti_unchecked!(self, GetTimerInfo, info_ptr).value(|| { info })
    }

    pub fn get_time(&self) -> JvmtiResult<jlong> {
        let mut nanos: jlong = none();
        let nanos_ptr: *mut jlong = &mut nanos;

        jvmti_unchecked!(self, GetTime, nanos_ptr).value(|| { nanos })
    }

    pub fn get_potential_capabilities(&self) -> JvmtiResult<jvmtiCapabilities> {
        let mut capabilities: jvmtiCapabilities = none();
        let capabilities_ptr: *mut jvmtiCapabilities = &mut capabilities;

        jvmti_unchecked!(self, GetPotentialCapabilities, capabilities_ptr).value(|| { capabilities })
    }

    pub fn add_capabilities(&self, capabilities: jvmtiCapabilities) -> JvmtiResult<()> {
        let ptr: *const jvmtiCapabilities = &capabilities;

        jvmti_unchecked!(self, AddCapabilities, ptr).value(|| { () })
    }

    pub fn relinquish_capabilities(&self, capabilities: jvmtiCapabilities) -> JvmtiResult<()> {
        let ptr: *const jvmtiCapabilities = &capabilities;

        jvmti_unchecked!(self, RelinquishCapabilities, ptr).value(|| { () })
    }

    pub fn get_available_processors(&self) -> JvmtiResult<jint> {
        let mut count: jint = none();
        let count_ptr: *mut jint = &mut count;

        jvmti_unchecked!(self, GetAvailableProcessors, count_ptr).value(|| { count })
    }

    /// Returns major, minor
    pub fn get_class_version_numbers(&self, class: jclass) -> JvmtiResult<(jint, jint)> {
        let mut major: jint = none();
        let major_ptr: *mut jint = &mut major;

        let mut minor: jint = none();
        let minor_ptr: *mut jint = &mut minor;

        jvmti_unchecked!(self, GetClassVersionNumbers, class, minor_ptr, major_ptr).value(|| { (major, minor) })
    }

    pub fn get_constant_pool(&self, class: jclass) -> JvmtiResult<(jint, &[c_uchar])> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut byte_count: i32 = none();
        let byte_count_ptr: *mut i32 = &mut byte_count;

        let mut bytes: *mut c_uchar = none();
        let bytes_ptr: *mut *mut c_uchar = &mut bytes;

        let error = jvmti_unchecked!(self, GetConstantPool, class, count_ptr, byte_count_ptr, bytes_ptr);

        error.value(|| { (count, as_slice(byte_count, bytes)) })
    }

    pub fn get_environment_local_storage(&self) -> JvmtiResult<*mut c_void> {
        let mut data: *mut c_void = none();
        let data_ptr: *mut *mut c_void = &mut data;

        jvmti_unchecked!(self, GetEnvironmentLocalStorage, data_ptr).value(|| { data })
    }

    pub fn set_environment_local_storage(&self, data: *const c_void) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetEnvironmentLocalStorage, data).value(|| { () })
    }

    pub fn add_to_bootstrap_class_loader_search(&self, segment: &str) -> JvmtiResult<()> {
        jvmti_unchecked!(self, AddToBootstrapClassLoaderSearch, as_c_char(segment)).value(|| { () })
    }

    pub fn set_verbose_flag(&self, flag: jvmtiVerboseFlag, value: bool) -> JvmtiResult<()> {
        jvmti_unchecked!(self, SetVerboseFlag, flag, value as jboolean).value(|| { () })
    }

    pub fn add_to_system_class_loader_search(&self, segment: &str) -> JvmtiResult<()> {
        jvmti_unchecked!(self, AddToSystemClassLoaderSearch, as_c_char(segment)).value(|| { () })
    }

    pub fn retransform_classes(&self, classes: Vec<jclass>) -> JvmtiResult<()> {
        let count = classes.len() as i32;
        let ptr = classes.as_ptr();

        jvmti_unchecked!(self, RetransformClasses, count, ptr).value(|| { () })
    }

    pub fn get_owned_monitor_stack_depth_info(&self, thread: jthread) -> JvmtiResult<&[jvmtiMonitorStackDepthInfo]> {
        let mut count: i32 = none();
        let count_ptr: *mut i32 = &mut count;

        let mut monitor_info: *mut jvmtiMonitorStackDepthInfo = none();
        let monitor_info_ptr: *mut *mut jvmtiMonitorStackDepthInfo = &mut monitor_info;

        let error = jvmti_unchecked!(self, GetOwnedMonitorStackDepthInfo, thread, count_ptr, monitor_info_ptr);

        error.value(|| { as_slice(count, monitor_info) })
    }

    pub fn get_object_size(&self, object: jobject) -> JvmtiResult<jlong> {
        let mut size: jlong = none();
        let size_ptr: *mut jlong = &mut size;

        jvmti_unchecked!(self, GetObjectSize, object, size_ptr).value(|| { size })
    }

    pub fn get_local_instance(&self, thread: jthread, depth: i32) -> JvmtiResult<jobject> {
        let mut value: jobject = none();
        let value_ptr: *mut jobject = &mut value;

        jvmti_unchecked!(self, GetLocalInstance, thread, depth, value_ptr).value(|| { value })
    }
}

impl From<*mut jvmtiEnv> for JvmtiEnv {
    fn from(jvmti: *mut jvmtiEnv) -> Self {
        JvmtiEnv {
            internal: jvmti
        }
    }
}

trait ErrorOr {
    fn value<T, F: FnOnce() -> T>(self, value: F) -> JvmtiResult<T>;
}

impl ErrorOr for jvmtiError {
    fn value<T, F: FnOnce() -> T>(self, value: F) -> JvmtiResult<T> {
        return if matches!(self, jvmtiError::JVMTI_ERROR_NONE) {
            Ok(value())
        } else {
            Err(self)
        };
    }
}
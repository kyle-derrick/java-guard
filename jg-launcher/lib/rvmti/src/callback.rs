use std::os::raw::{c_char, c_uchar, c_void};

use jni::JNIEnv;
use jni::sys::{jboolean, jclass, jfieldID, jint, jlong, jmethodID, jobject, jvalue};

use crate::{as_slice, jlocation, jthread, jvmtiAddrLocationMap, JvmtiEnv, jvmtiEnv, jvmtiEventCallbacks, to_string};

pub type JvmtiEventVMInit = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread);

pub type JvmtiEventVMDeath = fn(jvmti: JvmtiEnv, env: JNIEnv);

pub type JvmtiEventThreadStart = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread);

pub type JvmtiEventThreadEnd = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread);

pub type JvmtiEventClassFileLoadHook = fn(jvmti: JvmtiEnv, env: JNIEnv, class_being_redefined: jclass, loader: jobject, name: String, protection_domain: jobject, class_data: &[c_uchar]) -> Option<Vec<c_uchar>>;

pub type JvmtiEventClassLoad = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, class: jclass);

pub type JvmtiEventClassPrepare = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, class: jclass);

pub type JvmtiEventVMStart = fn(jvmti: JvmtiEnv, env: JNIEnv);

pub type JvmtiEventException = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, location: jlocation, exception: jobject, catch_method: jmethodID, catch_location: jlocation);

pub type JvmtiEventExceptionCatch = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, location: jlocation, exception: jobject);

pub type JvmtiEventSingleStep = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, location: jlocation);

pub type JvmtiEventFramePop = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, was_popped_by_exception: bool);

pub type JvmtiEventBreakpoint = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, location: jlocation);

pub type JvmtiEventFieldAccess = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, location: jlocation, field_class: jclass, object: jobject, field: jfieldID);

pub type JvmtiEventFieldModification = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, location: jlocation, field_class: jclass, object: jobject, field: jfieldID, signature_type: c_char, new_value: jvalue);

pub type JvmtiEventMethodEntry = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID);

pub type JvmtiEventMethodExit = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, was_popped_by_exception: bool, return_value: jvalue);

pub type JvmtiEventNativeMethodBind = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, method: jmethodID, address: *mut c_void) -> Option<*mut c_void>;

pub type JvmtiEventCompiledMethodLoad = fn(jvmti: JvmtiEnv, method: jmethodID, code: &[c_void], map: &[jvmtiAddrLocationMap], compiler_info: *const c_void);

pub type JvmtiEventCompiledMethodUnload = fn(jvmti: JvmtiEnv, method: jmethodID, code_addr: *const c_void);

pub type JvmtiEventDynamicCodeGenerated = fn(jvmti: JvmtiEnv, name: String, code: &[c_void]);

pub type JvmtiEventDataDumpRequest = fn(jvmti: JvmtiEnv);

pub type JvmtiEventMonitorWait = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, object: jobject, timeout: jlong);

pub type JvmtiEventMonitorWaited = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, object: jobject, timed_out: bool);

pub type JvmtiEventMonitorContendedEnter = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, object: jobject);

pub type JvmtiEventMonitorContendedEntered = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, object: jobject);

pub type JvmtiEventResourceExhausted = fn(jvmti: JvmtiEnv, env: JNIEnv, flags: jint, reserved: *const c_void, description: String);

pub type JvmtiEventGarbageCollectionStart = fn(jvmti: JvmtiEnv);

pub type JvmtiEventGarbageCollectionFinish = fn(jvmti: JvmtiEnv);

pub type JvmtiEventObjectFree = fn(jvmti: JvmtiEnv, tag: jlong);

pub type JvmtiEventVMObjectAlloc = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, object: jobject, object_class: jclass, size: jlong);

pub type JvmtiEventSampledObjectAlloc = fn(jvmti: JvmtiEnv, env: JNIEnv, thread: jthread, object: jobject, object_class: jclass, size: jlong);

#[derive(Default, Clone)]
pub struct JvmtiEventCallbacks {
    pub vm_init: Option<JvmtiEventVMInit>,
    pub vm_death: Option<JvmtiEventVMDeath>,
    pub thread_start: Option<JvmtiEventThreadStart>,
    pub thread_end: Option<JvmtiEventThreadEnd>,
    pub class_file_load_hook: Option<JvmtiEventClassFileLoadHook>,
    pub class_load: Option<JvmtiEventClassLoad>,
    pub class_prepare: Option<JvmtiEventClassPrepare>,
    pub vm_start: Option<JvmtiEventVMStart>,
    pub exception: Option<JvmtiEventException>,
    pub exception_catch: Option<JvmtiEventExceptionCatch>,
    pub single_step: Option<JvmtiEventSingleStep>,
    pub frame_pop: Option<JvmtiEventFramePop>,
    pub breakpoint: Option<JvmtiEventBreakpoint>,
    pub field_access: Option<JvmtiEventFieldAccess>,
    pub field_modification: Option<JvmtiEventFieldModification>,
    pub method_entry: Option<JvmtiEventMethodEntry>,
    pub method_exit: Option<JvmtiEventMethodExit>,
    pub native_method_bind: Option<JvmtiEventNativeMethodBind>,
    pub compiled_method_load: Option<JvmtiEventCompiledMethodLoad>,
    pub compiled_method_unload: Option<JvmtiEventCompiledMethodUnload>,
    pub dynamic_code_generated: Option<JvmtiEventDynamicCodeGenerated>,
    pub data_dump_request: Option<JvmtiEventDataDumpRequest>,
    pub monitor_wait: Option<JvmtiEventMonitorWait>,
    pub monitor_waited: Option<JvmtiEventMonitorWaited>,
    pub monitor_contended_enter: Option<JvmtiEventMonitorContendedEnter>,
    pub monitor_contended_entered: Option<JvmtiEventMonitorContendedEntered>,
    pub resource_exhausted: Option<JvmtiEventResourceExhausted>,
    pub garbage_collection_start: Option<JvmtiEventGarbageCollectionStart>,
    pub garbage_collection_finish: Option<JvmtiEventGarbageCollectionFinish>,
    pub object_free: Option<JvmtiEventObjectFree>,
    pub vm_object_alloc: Option<JvmtiEventVMObjectAlloc>,
    pub sampled_object_alloc: Option<JvmtiEventSampledObjectAlloc>,
}

macro_rules! callback_or {
    ($name:tt) => {
        unsafe {
            match CURRENT_CALLBACK.$name {
                None => None,
                Some(_) => Some($name)
            }
        }
    };
}

impl JvmtiEventCallbacks {
    pub fn to_raw(self) -> jvmtiEventCallbacks {
        unsafe {
            CURRENT_CALLBACK = self;
        }

        jvmtiEventCallbacks {
            VMInit: callback_or!(vm_init),
            VMDeath: callback_or!(vm_death),
            ThreadStart: callback_or!(thread_start),
            ThreadEnd: callback_or!(thread_end),
            ClassFileLoadHook: callback_or!(class_file_load_hook),
            ClassLoad: callback_or!(class_load),
            ClassPrepare: callback_or!(class_prepare),
            VMStart: callback_or!(vm_start),
            Exception: callback_or!(exception),
            ExceptionCatch: callback_or!(exception_catch),
            SingleStep: callback_or!(single_step),
            FramePop: callback_or!(frame_pop),
            Breakpoint: callback_or!(breakpoint),
            FieldAccess: callback_or!(field_access),
            FieldModification: callback_or!(field_modification),
            MethodEntry: callback_or!(method_entry),
            MethodExit: callback_or!(method_exit),
            NativeMethodBind: callback_or!(native_method_bind),
            CompiledMethodLoad: callback_or!(compiled_method_load),
            CompiledMethodUnload: callback_or!(compiled_method_unload),
            DynamicCodeGenerated: callback_or!(dynamic_code_generated),
            DataDumpRequest: callback_or!(data_dump_request),
            reserved72: None,
            MonitorWait: callback_or!(monitor_wait),
            MonitorWaited: callback_or!(monitor_waited),
            MonitorContendedEnter: callback_or!(monitor_contended_enter),
            MonitorContendedEntered: callback_or!(monitor_contended_entered),
            reserved77: None,
            reserved78: None,
            reserved79: None,
            ResourceExhausted: callback_or!(resource_exhausted),
            GarbageCollectionStart: callback_or!(garbage_collection_start),
            GarbageCollectionFinish: callback_or!(garbage_collection_finish),
            ObjectFree: callback_or!(object_free),
            VMObjectAlloc: callback_or!(vm_object_alloc),
            reserved85: None,
            SampledObjectAlloc: callback_or!(sampled_object_alloc),
        }
    }
}

static mut CURRENT_CALLBACK: JvmtiEventCallbacks = JvmtiEventCallbacks {
    vm_init: None,
    vm_death: None,
    thread_start: None,
    thread_end: None,
    class_file_load_hook: None,
    class_load: None,
    class_prepare: None,
    vm_start: None,
    exception: None,
    exception_catch: None,
    single_step: None,
    frame_pop: None,
    breakpoint: None,
    field_access: None,
    field_modification: None,
    method_entry: None,
    method_exit: None,
    native_method_bind: None,
    compiled_method_load: None,
    compiled_method_unload: None,
    dynamic_code_generated: None,
    data_dump_request: None,
    monitor_wait: None,
    monitor_waited: None,
    monitor_contended_enter: None,
    monitor_contended_entered: None,
    resource_exhausted: None,
    garbage_collection_start: None,
    garbage_collection_finish: None,
    object_free: None,
    vm_object_alloc: None,
    sampled_object_alloc: None,
};

macro_rules! jvmti_call {
    ($name:tt, $jvmti:expr $(, $args:expr)*) => {
        CURRENT_CALLBACK.$name.expect(stringify!($name))(JvmtiEnv::from($jvmti), $($args),*)
    };
}

macro_rules! jvmti_call_env {
    ($name:tt, $jvmti:expr, $env:expr) => {
        jvmti_call!($name, $jvmti, JNIEnv::from_raw($env).unwrap())
    };
    ($name:tt, $jvmti:expr, $env:expr $(, $args:expr)*) => {
        jvmti_call!($name, $jvmti, JNIEnv::from_raw($env).unwrap(), $($args),*)
    };
}

unsafe extern "C" fn vm_init(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread) {
    jvmti_call_env!(vm_init, jvmti, env, thread);
}

unsafe extern "C" fn vm_death(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv) {
    jvmti_call_env!(vm_death, jvmti, env);
}

unsafe extern "C" fn thread_start(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread) {
    jvmti_call_env!(thread_start, jvmti, env, thread);
}

unsafe extern "C" fn thread_end(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread) {
    jvmti_call_env!(thread_end, jvmti, env, thread);
}

unsafe extern "C" fn class_file_load_hook(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, class_being_redefined: jclass, loader: jobject, name: *const c_char, protection_domain: jobject, class_data_len: jint, class_data: *const c_uchar, new_class_data_len: *mut jint, new_class_data: *mut *mut c_uchar) {
    let data = jvmti_call_env!(class_file_load_hook, jvmti, env, class_being_redefined, loader, to_string(name), protection_domain, as_slice(class_data_len, class_data));

    match data {
        None => {}
        Some(vec) => {
            let jvmti = JvmtiEnv::from(jvmti);
            let size = vec.len();

            let mut allocated = jvmti.allocate(size as i64).unwrap();

            allocated.copy_from(vec.as_ptr(), size);

            *new_class_data_len = size as i32;
            *new_class_data = allocated;
        }
    };
}

unsafe extern "C" fn class_load(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, class: jclass) {
    jvmti_call_env!(class_load, jvmti, env, thread, class);
}

unsafe extern "C" fn class_prepare(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, class: jclass) {
    jvmti_call_env!(class_prepare, jvmti, env, thread, class);
}

unsafe extern "C" fn vm_start(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv) {
    jvmti_call_env!(vm_start, jvmti, env);
}

unsafe extern "C" fn exception(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, location: jlocation, exception: jobject, catch_method: jmethodID, catch_location: jlocation) {
    jvmti_call_env!(exception, jvmti, env, thread, method, location, exception, catch_method, catch_location);
}

unsafe extern "C" fn exception_catch(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, location: jlocation, exception: jobject) {
    jvmti_call_env!(exception_catch, jvmti, env, thread, method, location, exception);
}

unsafe extern "C" fn single_step(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, location: jlocation) {
    jvmti_call_env!(single_step, jvmti, env, thread, method, location);
}

unsafe extern "C" fn frame_pop(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, was_popped_by_exception: jboolean) {
    jvmti_call_env!(frame_pop, jvmti, env, thread, method, was_popped_by_exception == 1);
}

unsafe extern "C" fn breakpoint(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, location: jlocation) {
    jvmti_call_env!(breakpoint, jvmti, env, thread, method, location);
}

unsafe extern "C" fn field_access(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, location: jlocation, field_class: jclass, object: jobject, field: jfieldID) {
    jvmti_call_env!(field_access, jvmti, env, thread, method, location, field_class, object, field);
}

unsafe extern "C" fn field_modification(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, location: jlocation, field_class: jclass, object: jobject, field: jfieldID, signature_type: c_char, new_value: jvalue) {
    jvmti_call_env!(field_modification, jvmti, env, thread, method, location, field_class, object, field, signature_type, new_value);
}

unsafe extern "C" fn method_entry(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID) {
    jvmti_call_env!(method_entry, jvmti, env, thread, method);
}

unsafe extern "C" fn method_exit(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, was_popped_by_exception: jboolean, return_value: jvalue) {
    jvmti_call_env!(method_exit, jvmti, env, thread, method, was_popped_by_exception == 1, return_value);
}

unsafe extern "C" fn native_method_bind(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, method: jmethodID, address: *mut c_void, new_address_ptr: *mut *mut c_void) {
    let addr = jvmti_call_env!(native_method_bind, jvmti, env, thread, method, address);

    match addr {
        None => {}
        Some(new_addr) => {
            *new_address_ptr = new_addr;
        }
    }
}

unsafe extern "C" fn compiled_method_load(jvmti: *mut jvmtiEnv, method: jmethodID, code_size: jint, code_addr: *const c_void, map_length: jint, map: *const jvmtiAddrLocationMap, compile_info: *const c_void) {
    jvmti_call!(compiled_method_load, jvmti, method, as_slice(code_size, code_addr), as_slice(map_length, map), compile_info);
}

unsafe extern "C" fn compiled_method_unload(jvmti: *mut jvmtiEnv, method: jmethodID, code_addr: *const c_void) {
    jvmti_call!(compiled_method_unload, jvmti, method, code_addr);
}

unsafe extern "C" fn dynamic_code_generated(jvmti: *mut jvmtiEnv, name: *const c_char, address: *const c_void, length: jint) {
    jvmti_call!(dynamic_code_generated, jvmti, to_string(name), as_slice(length, address));
}

unsafe extern "C" fn data_dump_request(jvmti: *mut jvmtiEnv) {
    jvmti_call!(data_dump_request, jvmti);
}

unsafe extern "C" fn monitor_wait(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, object: jobject, timeout: jlong) {
    jvmti_call_env!(monitor_wait, jvmti, env, thread, object, timeout);
}

unsafe extern "C" fn monitor_waited(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, object: jobject, timed_out: jboolean) {
    jvmti_call_env!(monitor_waited, jvmti, env, thread, object, timed_out == 1);
}

unsafe extern "C" fn monitor_contended_enter(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, object: jobject) {
    jvmti_call_env!(monitor_contended_enter, jvmti, env, thread, object);
}

unsafe extern "C" fn monitor_contended_entered(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, object: jobject) {
    jvmti_call_env!(monitor_contended_entered, jvmti, env, thread, object);
}

unsafe extern "C" fn resource_exhausted(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, flags: jint, reserved: *const c_void, description: *const c_char) {
    jvmti_call_env!(resource_exhausted, jvmti, env, flags, reserved, to_string(description));
}

unsafe extern "C" fn garbage_collection_start(jvmti: *mut jvmtiEnv) {
    jvmti_call!(garbage_collection_start, jvmti);
}

unsafe extern "C" fn garbage_collection_finish(jvmti: *mut jvmtiEnv) {
    jvmti_call!(garbage_collection_finish, jvmti);
}

unsafe extern "C" fn object_free(jvmti: *mut jvmtiEnv, tag: jlong) {
    jvmti_call!(object_free, jvmti, tag);
}

unsafe extern "C" fn vm_object_alloc(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, object: jobject, object_class: jclass, size: jlong) {
    jvmti_call_env!(vm_object_alloc, jvmti, env, thread, object, object_class, size);
}

unsafe extern "C" fn sampled_object_alloc(jvmti: *mut jvmtiEnv, env: *mut jni::sys::JNIEnv, thread: jthread, object: jobject, object_class: jclass, size: jlong) {
    jvmti_call_env!(sampled_object_alloc, jvmti, env, thread, object, object_class, size);
}
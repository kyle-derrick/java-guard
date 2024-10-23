#![allow(non_snake_case)]
#![allow(non_camel_case_types)]
extern crate jni;

use std::os::raw::c_int;

use jni::sys::*;

#[repr(C)]
#[derive(Copy, Clone, Debug, Default, Eq, Hash, Ord, PartialEq, PartialOrd)]
pub struct __BindgenBitfieldUnit<Storage> {
    storage: Storage,
}

impl<Storage> __BindgenBitfieldUnit<Storage> {
    #[inline]
    pub const fn new(storage: Storage) -> Self {
        Self { storage }
    }
}

impl<Storage> __BindgenBitfieldUnit<Storage>
    where
        Storage: AsRef<[u8]> + AsMut<[u8]>,
{
    #[inline]
    pub fn get_bit(&self, index: usize) -> bool {
        debug_assert!(index / 8 < self.storage.as_ref().len());
        let byte_index = index / 8;
        let byte = self.storage.as_ref()[byte_index];
        let bit_index = if cfg!(target_endian = "big") {
            7 - (index % 8)
        } else {
            index % 8
        };
        let mask = 1 << bit_index;
        byte & mask == mask
    }
    #[inline]
    pub fn set_bit(&mut self, index: usize, val: bool) {
        debug_assert!(index / 8 < self.storage.as_ref().len());
        let byte_index = index / 8;
        let byte = &mut self.storage.as_mut()[byte_index];
        let bit_index = if cfg!(target_endian = "big") {
            7 - (index % 8)
        } else {
            index % 8
        };
        let mask = 1 << bit_index;
        if val {
            *byte |= mask;
        } else {
            *byte &= !mask;
        }
    }
    #[inline]
    pub fn get(&self, bit_offset: usize, bit_width: u8) -> u64 {
        debug_assert!(bit_width <= 64);
        debug_assert!(bit_offset / 8 < self.storage.as_ref().len());
        debug_assert!((bit_offset + (bit_width as usize)) / 8 <= self.storage.as_ref().len());
        let mut val = 0;
        for i in 0..(bit_width as usize) {
            if self.get_bit(i + bit_offset) {
                let index = if cfg!(target_endian = "big") {
                    bit_width as usize - 1 - i
                } else {
                    i
                };
                val |= 1 << index;
            }
        }
        val
    }
    #[inline]
    pub fn set(&mut self, bit_offset: usize, bit_width: u8, val: u64) {
        debug_assert!(bit_width <= 64);
        debug_assert!(bit_offset / 8 < self.storage.as_ref().len());
        debug_assert!((bit_offset + (bit_width as usize)) / 8 <= self.storage.as_ref().len());
        for i in 0..(bit_width as usize) {
            let mask = 1 << i;
            let val_bit_is_set = val & mask == mask;
            let index = if cfg!(target_endian = "big") {
                bit_width as usize - 1 - i
            } else {
                i
            };
            self.set_bit(index + bit_offset, val_bit_is_set);
        }
    }
}

pub const JVMTI_VERSION_1: c_int = 805371904;
pub const JVMTI_VERSION_1_1: c_int = 805372160;
pub const JVMTI_VERSION_1_2: c_int = 805372416;
pub const JVMTI_VERSION_9: c_int = 805896192;
pub const JVMTI_VERSION_11: c_int = 80602726;
pub const JVMTI_VERSION: c_int = 806420480;

pub type jvmtiEnv = *const jvmtiInterface_1_;
pub type jthread = jobject;
pub type jthreadGroup = jobject;
pub type jlocation = jlong;

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct _jrawMonitorID {
    _unused: [u8; 0],
}

pub type jrawMonitorID = *mut _jrawMonitorID;
pub type jniNativeInterface = JNINativeInterface_;

pub const JVMTI_THREAD_STATE_ALIVE: c_int = 1;
pub const JVMTI_THREAD_STATE_TERMINATED: c_int = 2;
pub const JVMTI_THREAD_STATE_RUNNABLE: c_int = 4;
pub const JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER: c_int = 1024;
pub const JVMTI_THREAD_STATE_WAITING: c_int = 128;
pub const JVMTI_THREAD_STATE_WAITING_INDEFINITELY: c_int = 16;
pub const JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT: c_int = 32;
pub const JVMTI_THREAD_STATE_SLEEPING: c_int = 64;
pub const JVMTI_THREAD_STATE_IN_OBJECT_WAIT: c_int = 256;
pub const JVMTI_THREAD_STATE_PARKED: c_int = 512;
pub const JVMTI_THREAD_STATE_SUSPENDED: c_int = 1048576;
pub const JVMTI_THREAD_STATE_INTERRUPTED: c_int = 2097152;
pub const JVMTI_THREAD_STATE_IN_NATIVE: c_int = 4194304;
pub const JVMTI_THREAD_STATE_VENDOR_1: c_int = 268435456;
pub const JVMTI_THREAD_STATE_VENDOR_2: c_int = 536870912;
pub const JVMTI_THREAD_STATE_VENDOR_3: c_int = 1073741824;

pub const JVMTI_JAVA_LANG_THREAD_STATE_MASK: _bindgen_ty_3 =
    _bindgen_ty_3::JVMTI_JAVA_LANG_THREAD_STATE_MASK;
pub const JVMTI_JAVA_LANG_THREAD_STATE_NEW: _bindgen_ty_3 =
    _bindgen_ty_3::JVMTI_JAVA_LANG_THREAD_STATE_NEW;
pub const JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED: _bindgen_ty_3 =
    _bindgen_ty_3::JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED;
pub const JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE: _bindgen_ty_3 =
    _bindgen_ty_3::JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE;
pub const JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED: _bindgen_ty_3 =
    _bindgen_ty_3::JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED;
pub const JVMTI_JAVA_LANG_THREAD_STATE_WAITING: _bindgen_ty_3 =
    _bindgen_ty_3::JVMTI_JAVA_LANG_THREAD_STATE_WAITING;
pub const JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING: _bindgen_ty_3 =
    _bindgen_ty_3::JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING;

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum _bindgen_ty_3 {
    JVMTI_JAVA_LANG_THREAD_STATE_MASK = 1207,
    JVMTI_JAVA_LANG_THREAD_STATE_NEW = 0,
    JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED = 2,
    JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE = 5,
    JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED = 1025,
    JVMTI_JAVA_LANG_THREAD_STATE_WAITING = 145,
    JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING = 161,
}

pub const JVMTI_THREAD_MIN_PRIORITY: _bindgen_ty_4 = _bindgen_ty_4::JVMTI_THREAD_MIN_PRIORITY;
pub const JVMTI_THREAD_NORM_PRIORITY: _bindgen_ty_4 = _bindgen_ty_4::JVMTI_THREAD_NORM_PRIORITY;
pub const JVMTI_THREAD_MAX_PRIORITY: _bindgen_ty_4 = _bindgen_ty_4::JVMTI_THREAD_MAX_PRIORITY;

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum _bindgen_ty_4 {
    JVMTI_THREAD_MIN_PRIORITY = 1,
    JVMTI_THREAD_NORM_PRIORITY = 5,
    JVMTI_THREAD_MAX_PRIORITY = 10,
}

pub const JVMTI_HEAP_FILTER_TAGGED: c_int = 4;
pub const JVMTI_HEAP_FILTER_UNTAGGED: c_int = 8;
pub const JVMTI_HEAP_FILTER_CLASS_TAGGED: c_int = 16;
pub const JVMTI_HEAP_FILTER_CLASS_UNTAGGED: c_int = 32;

pub const JVMTI_VISIT_OBJECTS: c_int = 256;
pub const JVMTI_VISIT_ABORT: c_int = 32768;

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiHeapReferenceKind {
    JVMTI_HEAP_REFERENCE_CLASS = 1,
    JVMTI_HEAP_REFERENCE_FIELD = 2,
    JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT = 3,
    JVMTI_HEAP_REFERENCE_CLASS_LOADER = 4,
    JVMTI_HEAP_REFERENCE_SIGNERS = 5,
    JVMTI_HEAP_REFERENCE_PROTECTION_DOMAIN = 6,
    JVMTI_HEAP_REFERENCE_INTERFACE = 7,
    JVMTI_HEAP_REFERENCE_STATIC_FIELD = 8,
    JVMTI_HEAP_REFERENCE_CONSTANT_POOL = 9,
    JVMTI_HEAP_REFERENCE_SUPERCLASS = 10,
    JVMTI_HEAP_REFERENCE_JNI_GLOBAL = 21,
    JVMTI_HEAP_REFERENCE_SYSTEM_CLASS = 22,
    JVMTI_HEAP_REFERENCE_MONITOR = 23,
    JVMTI_HEAP_REFERENCE_STACK_LOCAL = 24,
    JVMTI_HEAP_REFERENCE_JNI_LOCAL = 25,
    JVMTI_HEAP_REFERENCE_THREAD = 26,
    JVMTI_HEAP_REFERENCE_OTHER = 27,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiPrimitiveType {
    JVMTI_PRIMITIVE_TYPE_BOOLEAN = 90,
    JVMTI_PRIMITIVE_TYPE_BYTE = 66,
    JVMTI_PRIMITIVE_TYPE_CHAR = 67,
    JVMTI_PRIMITIVE_TYPE_SHORT = 83,
    JVMTI_PRIMITIVE_TYPE_INT = 73,
    JVMTI_PRIMITIVE_TYPE_LONG = 74,
    JVMTI_PRIMITIVE_TYPE_FLOAT = 70,
    JVMTI_PRIMITIVE_TYPE_DOUBLE = 68,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiHeapObjectFilter {
    JVMTI_HEAP_OBJECT_TAGGED = 1,
    JVMTI_HEAP_OBJECT_UNTAGGED = 2,
    JVMTI_HEAP_OBJECT_EITHER = 3,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiHeapRootKind {
    JVMTI_HEAP_ROOT_JNI_GLOBAL = 1,
    JVMTI_HEAP_ROOT_SYSTEM_CLASS = 2,
    JVMTI_HEAP_ROOT_MONITOR = 3,
    JVMTI_HEAP_ROOT_STACK_LOCAL = 4,
    JVMTI_HEAP_ROOT_JNI_LOCAL = 5,
    JVMTI_HEAP_ROOT_THREAD = 6,
    JVMTI_HEAP_ROOT_OTHER = 7,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiObjectReferenceKind {
    JVMTI_REFERENCE_CLASS = 1,
    JVMTI_REFERENCE_FIELD = 2,
    JVMTI_REFERENCE_ARRAY_ELEMENT = 3,
    JVMTI_REFERENCE_CLASS_LOADER = 4,
    JVMTI_REFERENCE_SIGNERS = 5,
    JVMTI_REFERENCE_PROTECTION_DOMAIN = 6,
    JVMTI_REFERENCE_INTERFACE = 7,
    JVMTI_REFERENCE_STATIC_FIELD = 8,
    JVMTI_REFERENCE_CONSTANT_POOL = 9,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiIterationControl {
    JVMTI_ITERATION_CONTINUE = 1,
    JVMTI_ITERATION_IGNORE = 2,
    JVMTI_ITERATION_ABORT = 0,
}

pub const JVMTI_CLASS_STATUS_VERIFIED: c_int = 1;
pub const JVMTI_CLASS_STATUS_PREPARED: c_int = 2;
pub const JVMTI_CLASS_STATUS_INITIALIZED: c_int = 4;
pub const JVMTI_CLASS_STATUS_ERROR: c_int = 8;
pub const JVMTI_CLASS_STATUS_ARRAY: c_int = 16;
pub const JVMTI_CLASS_STATUS_PRIMITIVE: c_int = 32;

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiEventMode {
    JVMTI_ENABLE = 1,
    JVMTI_DISABLE = 0,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiParamTypes {
    JVMTI_TYPE_JBYTE = 101,
    JVMTI_TYPE_JCHAR = 102,
    JVMTI_TYPE_JSHORT = 103,
    JVMTI_TYPE_JINT = 104,
    JVMTI_TYPE_JLONG = 105,
    JVMTI_TYPE_JFLOAT = 106,
    JVMTI_TYPE_JDOUBLE = 107,
    JVMTI_TYPE_JBOOLEAN = 108,
    JVMTI_TYPE_JOBJECT = 109,
    JVMTI_TYPE_JTHREAD = 110,
    JVMTI_TYPE_JCLASS = 111,
    JVMTI_TYPE_JVALUE = 112,
    JVMTI_TYPE_JFIELDID = 113,
    JVMTI_TYPE_JMETHODID = 114,
    JVMTI_TYPE_CCHAR = 115,
    JVMTI_TYPE_CVOID = 116,
    JVMTI_TYPE_JNIENV = 117,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiParamKind {
    JVMTI_KIND_IN = 91,
    JVMTI_KIND_IN_PTR = 92,
    JVMTI_KIND_IN_BUF = 93,
    JVMTI_KIND_ALLOC_BUF = 94,
    JVMTI_KIND_ALLOC_ALLOC_BUF = 95,
    JVMTI_KIND_OUT = 96,
    JVMTI_KIND_OUT_BUF = 97,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiTimerKind {
    JVMTI_TIMER_USER_CPU = 30,
    JVMTI_TIMER_TOTAL_CPU = 31,
    JVMTI_TIMER_ELAPSED = 32,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiPhase {
    JVMTI_PHASE_ONLOAD = 1,
    JVMTI_PHASE_PRIMORDIAL = 2,
    JVMTI_PHASE_START = 6,
    JVMTI_PHASE_LIVE = 4,
    JVMTI_PHASE_DEAD = 8,
}

pub const JVMTI_VERSION_INTERFACE_JNI: c_int = 0;
pub const JVMTI_VERSION_INTERFACE_JVMTI: c_int = 805306368;

pub const JVMTI_VERSION_MASK_INTERFACE_TYPE: c_int = 1879048192;
pub const JVMTI_VERSION_MASK_MAJOR: c_int = 268369920;
pub const JVMTI_VERSION_MASK_MINOR: c_int = 65280;
pub const JVMTI_VERSION_MASK_MICRO: c_int = 255;

pub const JVMTI_VERSION_SHIFT_MAJOR: c_int = 16;
pub const JVMTI_VERSION_SHIFT_MINOR: c_int = 8;
pub const JVMTI_VERSION_SHIFT_MICRO: c_int = 0;

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiVerboseFlag {
    JVMTI_VERBOSE_OTHER = 0,
    JVMTI_VERBOSE_GC = 1,
    JVMTI_VERBOSE_CLASS = 2,
    JVMTI_VERBOSE_JNI = 4,
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiJlocationFormat {
    JVMTI_JLOCATION_JVMBCI = 1,
    JVMTI_JLOCATION_MACHINEPC = 2,
    JVMTI_JLOCATION_OTHER = 0,
}

pub const JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR: c_int = 1;
pub const JVMTI_RESOURCE_EXHAUSTED_JAVA_HEAP: c_int = 2;
pub const JVMTI_RESOURCE_EXHAUSTED_THREADS: c_int = 4;

impl jvmtiError {
    pub const JVMTI_ERROR_MAX: jvmtiError = jvmtiError::JVMTI_ERROR_INVALID_ENVIRONMENT;
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiError {
    JVMTI_ERROR_NONE = 0,
    JVMTI_ERROR_INVALID_THREAD = 10,
    JVMTI_ERROR_INVALID_THREAD_GROUP = 11,
    JVMTI_ERROR_INVALID_PRIORITY = 12,
    JVMTI_ERROR_THREAD_NOT_SUSPENDED = 13,
    JVMTI_ERROR_THREAD_SUSPENDED = 14,
    JVMTI_ERROR_THREAD_NOT_ALIVE = 15,
    JVMTI_ERROR_INVALID_OBJECT = 20,
    JVMTI_ERROR_INVALID_CLASS = 21,
    JVMTI_ERROR_CLASS_NOT_PREPARED = 22,
    JVMTI_ERROR_INVALID_METHODID = 23,
    JVMTI_ERROR_INVALID_LOCATION = 24,
    JVMTI_ERROR_INVALID_FIELDID = 25,
    JVMTI_ERROR_INVALID_MODULE = 26,
    JVMTI_ERROR_NO_MORE_FRAMES = 31,
    JVMTI_ERROR_OPAQUE_FRAME = 32,
    JVMTI_ERROR_TYPE_MISMATCH = 34,
    JVMTI_ERROR_INVALID_SLOT = 35,
    JVMTI_ERROR_DUPLICATE = 40,
    JVMTI_ERROR_NOT_FOUND = 41,
    JVMTI_ERROR_INVALID_MONITOR = 50,
    JVMTI_ERROR_NOT_MONITOR_OWNER = 51,
    JVMTI_ERROR_INTERRUPT = 52,
    JVMTI_ERROR_INVALID_CLASS_FORMAT = 60,
    JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION = 61,
    JVMTI_ERROR_FAILS_VERIFICATION = 62,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED = 63,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED = 64,
    JVMTI_ERROR_INVALID_TYPESTATE = 65,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED = 66,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED = 67,
    JVMTI_ERROR_UNSUPPORTED_VERSION = 68,
    JVMTI_ERROR_NAMES_DONT_MATCH = 69,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED = 70,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED = 71,
    JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED = 72,
    JVMTI_ERROR_UNMODIFIABLE_CLASS = 79,
    JVMTI_ERROR_UNMODIFIABLE_MODULE = 80,
    JVMTI_ERROR_NOT_AVAILABLE = 98,
    JVMTI_ERROR_MUST_POSSESS_CAPABILITY = 99,
    JVMTI_ERROR_NULL_POINTER = 100,
    JVMTI_ERROR_ABSENT_INFORMATION = 101,
    JVMTI_ERROR_INVALID_EVENT_TYPE = 102,
    JVMTI_ERROR_ILLEGAL_ARGUMENT = 103,
    JVMTI_ERROR_NATIVE_METHOD = 104,
    JVMTI_ERROR_CLASS_LOADER_UNSUPPORTED = 106,
    JVMTI_ERROR_OUT_OF_MEMORY = 110,
    JVMTI_ERROR_ACCESS_DENIED = 111,
    JVMTI_ERROR_WRONG_PHASE = 112,
    JVMTI_ERROR_INTERNAL = 113,
    JVMTI_ERROR_UNATTACHED_THREAD = 115,
    JVMTI_ERROR_INVALID_ENVIRONMENT = 116,
}

impl jvmtiEvent {
    pub const JVMTI_EVENT_VM_INIT: jvmtiEvent = jvmtiEvent::JVMTI_MIN_EVENT_TYPE_VAL;
}

impl jvmtiEvent {
    pub const JVMTI_MAX_EVENT_TYPE_VAL: jvmtiEvent = jvmtiEvent::JVMTI_EVENT_SAMPLED_OBJECT_ALLOC;
}

#[repr(i32)]
#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub enum jvmtiEvent {
    JVMTI_MIN_EVENT_TYPE_VAL = 50,
    JVMTI_EVENT_VM_DEATH = 51,
    JVMTI_EVENT_THREAD_START = 52,
    JVMTI_EVENT_THREAD_END = 53,
    JVMTI_EVENT_CLASS_FILE_LOAD_HOOK = 54,
    JVMTI_EVENT_CLASS_LOAD = 55,
    JVMTI_EVENT_CLASS_PREPARE = 56,
    JVMTI_EVENT_VM_START = 57,
    JVMTI_EVENT_EXCEPTION = 58,
    JVMTI_EVENT_EXCEPTION_CATCH = 59,
    JVMTI_EVENT_SINGLE_STEP = 60,
    JVMTI_EVENT_FRAME_POP = 61,
    JVMTI_EVENT_BREAKPOINT = 62,
    JVMTI_EVENT_FIELD_ACCESS = 63,
    JVMTI_EVENT_FIELD_MODIFICATION = 64,
    JVMTI_EVENT_METHOD_ENTRY = 65,
    JVMTI_EVENT_METHOD_EXIT = 66,
    JVMTI_EVENT_NATIVE_METHOD_BIND = 67,
    JVMTI_EVENT_COMPILED_METHOD_LOAD = 68,
    JVMTI_EVENT_COMPILED_METHOD_UNLOAD = 69,
    JVMTI_EVENT_DYNAMIC_CODE_GENERATED = 70,
    JVMTI_EVENT_DATA_DUMP_REQUEST = 71,
    JVMTI_EVENT_MONITOR_WAIT = 73,
    JVMTI_EVENT_MONITOR_WAITED = 74,
    JVMTI_EVENT_MONITOR_CONTENDED_ENTER = 75,
    JVMTI_EVENT_MONITOR_CONTENDED_ENTERED = 76,
    JVMTI_EVENT_RESOURCE_EXHAUSTED = 80,
    JVMTI_EVENT_GARBAGE_COLLECTION_START = 81,
    JVMTI_EVENT_GARBAGE_COLLECTION_FINISH = 82,
    JVMTI_EVENT_OBJECT_FREE = 83,
    JVMTI_EVENT_VM_OBJECT_ALLOC = 84,
    JVMTI_EVENT_SAMPLED_OBJECT_ALLOC = 86,
}

pub type jvmtiStartFunction = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        arg: *mut std::os::raw::c_void,
    ),
>;
pub type jvmtiHeapIterationCallback = Option<
    unsafe extern "C" fn(
        class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        length: jint,
        user_data: *mut std::os::raw::c_void,
    ) -> jint,
>;
pub type jvmtiHeapReferenceCallback = Option<
    unsafe extern "C" fn(
        reference_kind: jvmtiHeapReferenceKind,
        reference_info: *const jvmtiHeapReferenceInfo,
        class_tag: jlong,
        referrer_class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        referrer_tag_ptr: *mut jlong,
        length: jint,
        user_data: *mut std::os::raw::c_void,
    ) -> jint,
>;
pub type jvmtiPrimitiveFieldCallback = Option<
    unsafe extern "C" fn(
        kind: jvmtiHeapReferenceKind,
        info: *const jvmtiHeapReferenceInfo,
        object_class_tag: jlong,
        object_tag_ptr: *mut jlong,
        value: jvalue,
        value_type: jvmtiPrimitiveType,
        user_data: *mut std::os::raw::c_void,
    ) -> jint,
>;
pub type jvmtiArrayPrimitiveValueCallback = Option<
    unsafe extern "C" fn(
        class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        element_count: jint,
        element_type: jvmtiPrimitiveType,
        elements: *const std::os::raw::c_void,
        user_data: *mut std::os::raw::c_void,
    ) -> jint,
>;
pub type jvmtiStringPrimitiveValueCallback = Option<
    unsafe extern "C" fn(
        class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        value: *const jchar,
        value_length: jint,
        user_data: *mut std::os::raw::c_void,
    ) -> jint,
>;
pub type jvmtiReservedCallback = Option<unsafe extern "C" fn() -> jint>;
pub type jvmtiHeapObjectCallback = Option<
    unsafe extern "C" fn(
        class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        user_data: *mut std::os::raw::c_void,
    ) -> jvmtiIterationControl,
>;
pub type jvmtiHeapRootCallback = Option<
    unsafe extern "C" fn(
        root_kind: jvmtiHeapRootKind,
        class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        user_data: *mut std::os::raw::c_void,
    ) -> jvmtiIterationControl,
>;
pub type jvmtiStackReferenceCallback = Option<
    unsafe extern "C" fn(
        root_kind: jvmtiHeapRootKind,
        class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        thread_tag: jlong,
        depth: jint,
        method: jmethodID,
        slot: jint,
        user_data: *mut std::os::raw::c_void,
    ) -> jvmtiIterationControl,
>;
pub type jvmtiObjectReferenceCallback = Option<
    unsafe extern "C" fn(
        reference_kind: jvmtiObjectReferenceKind,
        class_tag: jlong,
        size: jlong,
        tag_ptr: *mut jlong,
        referrer_tag: jlong,
        referrer_index: jint,
        user_data: *mut std::os::raw::c_void,
    ) -> jvmtiIterationControl,
>;
pub type jvmtiExtensionFunction =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, ...) -> jvmtiError>;
pub type jvmtiExtensionEvent =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, ...)>;

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiThreadInfo {
    pub name: *mut std::os::raw::c_char,
    pub priority: jint,
    pub is_daemon: jboolean,
    pub thread_group: jthreadGroup,
    pub context_class_loader: jobject,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiMonitorStackDepthInfo {
    pub monitor: jobject,
    pub stack_depth: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiThreadGroupInfo {
    pub parent: jthreadGroup,
    pub name: *mut std::os::raw::c_char,
    pub max_priority: jint,
    pub is_daemon: jboolean,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiFrameInfo {
    pub method: jmethodID,
    pub location: jlocation,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiStackInfo {
    pub thread: jthread,
    pub state: jint,
    pub frame_buffer: *mut jvmtiFrameInfo,
    pub frame_count: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiHeapReferenceInfoField {
    pub index: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiHeapReferenceInfoArray {
    pub index: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiHeapReferenceInfoConstantPool {
    pub index: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiHeapReferenceInfoStackLocal {
    pub thread_tag: jlong,
    pub thread_id: jlong,
    pub depth: jint,
    pub method: jmethodID,
    pub location: jlocation,
    pub slot: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiHeapReferenceInfoJniLocal {
    pub thread_tag: jlong,
    pub thread_id: jlong,
    pub depth: jint,
    pub method: jmethodID,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiHeapReferenceInfoReserved {
    pub reserved1: jlong,
    pub reserved2: jlong,
    pub reserved3: jlong,
    pub reserved4: jlong,
    pub reserved5: jlong,
    pub reserved6: jlong,
    pub reserved7: jlong,
    pub reserved8: jlong,
}

#[repr(C)]
#[derive(Copy, Clone)]
pub union jvmtiHeapReferenceInfo {
    pub field: jvmtiHeapReferenceInfoField,
    pub array: jvmtiHeapReferenceInfoArray,
    pub constant_pool: jvmtiHeapReferenceInfoConstantPool,
    pub stack_local: jvmtiHeapReferenceInfoStackLocal,
    pub jni_local: jvmtiHeapReferenceInfoJniLocal,
    pub other: jvmtiHeapReferenceInfoReserved,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiHeapCallbacks {
    pub heap_iteration_callback: jvmtiHeapIterationCallback,
    pub heap_reference_callback: jvmtiHeapReferenceCallback,
    pub primitive_field_callback: jvmtiPrimitiveFieldCallback,
    pub array_primitive_value_callback: jvmtiArrayPrimitiveValueCallback,
    pub string_primitive_value_callback: jvmtiStringPrimitiveValueCallback,
    pub reserved5: jvmtiReservedCallback,
    pub reserved6: jvmtiReservedCallback,
    pub reserved7: jvmtiReservedCallback,
    pub reserved8: jvmtiReservedCallback,
    pub reserved9: jvmtiReservedCallback,
    pub reserved10: jvmtiReservedCallback,
    pub reserved11: jvmtiReservedCallback,
    pub reserved12: jvmtiReservedCallback,
    pub reserved13: jvmtiReservedCallback,
    pub reserved14: jvmtiReservedCallback,
    pub reserved15: jvmtiReservedCallback,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiClassDefinition {
    pub klass: jclass,
    pub class_byte_count: jint,
    pub class_bytes: *const std::os::raw::c_uchar,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiMonitorUsage {
    pub owner: jthread,
    pub entry_count: jint,
    pub waiter_count: jint,
    pub waiters: *mut jthread,
    pub notify_waiter_count: jint,
    pub notify_waiters: *mut jthread,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiLineNumberEntry {
    pub start_location: jlocation,
    pub line_number: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiLocalVariableEntry {
    pub start_location: jlocation,
    pub length: jint,
    pub name: *mut std::os::raw::c_char,
    pub signature: *mut std::os::raw::c_char,
    pub generic_signature: *mut std::os::raw::c_char,
    pub slot: jint,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiParamInfo {
    pub name: *mut std::os::raw::c_char,
    pub kind: jvmtiParamKind,
    pub base_type: jvmtiParamTypes,
    pub null_ok: jboolean,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiExtensionFunctionInfo {
    pub func: jvmtiExtensionFunction,
    pub id: *mut std::os::raw::c_char,
    pub short_description: *mut std::os::raw::c_char,
    pub param_count: jint,
    pub params: *mut jvmtiParamInfo,
    pub error_count: jint,
    pub errors: *mut jvmtiError,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiExtensionEventInfo {
    pub extension_event_index: jint,
    pub id: *mut std::os::raw::c_char,
    pub short_description: *mut std::os::raw::c_char,
    pub param_count: jint,
    pub params: *mut jvmtiParamInfo,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiTimerInfo {
    pub max_value: jlong,
    pub may_skip_forward: jboolean,
    pub may_skip_backward: jboolean,
    pub kind: jvmtiTimerKind,
    pub reserved1: jlong,
    pub reserved2: jlong,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiAddrLocationMap {
    pub start_address: *const std::os::raw::c_void,
    pub location: jlocation,
}

#[repr(C)]
#[repr(align(4))]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiCapabilities {
    pub _bitfield_align_1: [u8; 0],
    pub _bitfield_1: __BindgenBitfieldUnit<[u8; 16usize]>,
}

impl jvmtiCapabilities {
    #[inline]
    pub fn can_tag_objects(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(0usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_tag_objects(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(0usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_field_modification_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(1usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_field_modification_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(1usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_field_access_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(2usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_field_access_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(2usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_bytecodes(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(3usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_bytecodes(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(3usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_synthetic_attribute(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(4usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_synthetic_attribute(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(4usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_owned_monitor_info(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(5usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_owned_monitor_info(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(5usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_current_contended_monitor(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(6usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_current_contended_monitor(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(6usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_monitor_info(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(7usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_monitor_info(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(7usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_pop_frame(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(8usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_pop_frame(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(8usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_redefine_classes(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(9usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_redefine_classes(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(9usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_signal_thread(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(10usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_signal_thread(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(10usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_source_file_name(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(11usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_source_file_name(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(11usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_line_numbers(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(12usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_line_numbers(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(12usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_source_debug_extension(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(13usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_source_debug_extension(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(13usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_access_local_variables(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(14usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_access_local_variables(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(14usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_maintain_original_method_order(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(15usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_maintain_original_method_order(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(15usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_single_step_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(16usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_single_step_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(16usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_exception_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(17usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_exception_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(17usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_frame_pop_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(18usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_frame_pop_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(18usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_breakpoint_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(19usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_breakpoint_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(19usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_suspend(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(20usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_suspend(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(20usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_redefine_any_class(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(21usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_redefine_any_class(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(21usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_current_thread_cpu_time(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(22usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_current_thread_cpu_time(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(22usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_thread_cpu_time(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(23usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_thread_cpu_time(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(23usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_method_entry_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(24usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_method_entry_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(24usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_method_exit_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(25usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_method_exit_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(25usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_all_class_hook_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(26usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_all_class_hook_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(26usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_compiled_method_load_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(27usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_compiled_method_load_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(27usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_monitor_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(28usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_monitor_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(28usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_vm_object_alloc_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(29usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_vm_object_alloc_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(29usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_native_method_bind_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(30usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_native_method_bind_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(30usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_garbage_collection_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(31usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_garbage_collection_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(31usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_object_free_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(32usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_object_free_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(32usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_force_early_return(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(33usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_force_early_return(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(33usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_owned_monitor_stack_depth_info(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(34usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_owned_monitor_stack_depth_info(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(34usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_get_constant_pool(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(35usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_get_constant_pool(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(35usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_set_native_method_prefix(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(36usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_set_native_method_prefix(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(36usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_retransform_classes(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(37usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_retransform_classes(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(37usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_retransform_any_class(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(38usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_retransform_any_class(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(38usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_resource_exhaustion_heap_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(39usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_resource_exhaustion_heap_events(
        &mut self,
        val: std::os::raw::c_uint,
    ) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(39usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_resource_exhaustion_threads_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(40usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_resource_exhaustion_threads_events(
        &mut self,
        val: std::os::raw::c_uint,
    ) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(40usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_early_vmstart(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(41usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_early_vmstart(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(41usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_early_class_hook_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(42usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_early_class_hook_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(42usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn can_generate_sampled_object_alloc_events(&self) -> std::os::raw::c_uint {
        unsafe { std::mem::transmute(self._bitfield_1.get(43usize, 1u8) as u32) }
    }
    #[inline]
    pub fn set_can_generate_sampled_object_alloc_events(&mut self, val: std::os::raw::c_uint) {
        unsafe {
            let val: u32 = std::mem::transmute(val);
            self._bitfield_1.set(43usize, 1u8, val as u64)
        }
    }
    #[inline]
    pub fn new_bitfield_1(
        can_tag_objects: std::os::raw::c_uint,
        can_generate_field_modification_events: std::os::raw::c_uint,
        can_generate_field_access_events: std::os::raw::c_uint,
        can_get_bytecodes: std::os::raw::c_uint,
        can_get_synthetic_attribute: std::os::raw::c_uint,
        can_get_owned_monitor_info: std::os::raw::c_uint,
        can_get_current_contended_monitor: std::os::raw::c_uint,
        can_get_monitor_info: std::os::raw::c_uint,
        can_pop_frame: std::os::raw::c_uint,
        can_redefine_classes: std::os::raw::c_uint,
        can_signal_thread: std::os::raw::c_uint,
        can_get_source_file_name: std::os::raw::c_uint,
        can_get_line_numbers: std::os::raw::c_uint,
        can_get_source_debug_extension: std::os::raw::c_uint,
        can_access_local_variables: std::os::raw::c_uint,
        can_maintain_original_method_order: std::os::raw::c_uint,
        can_generate_single_step_events: std::os::raw::c_uint,
        can_generate_exception_events: std::os::raw::c_uint,
        can_generate_frame_pop_events: std::os::raw::c_uint,
        can_generate_breakpoint_events: std::os::raw::c_uint,
        can_suspend: std::os::raw::c_uint,
        can_redefine_any_class: std::os::raw::c_uint,
        can_get_current_thread_cpu_time: std::os::raw::c_uint,
        can_get_thread_cpu_time: std::os::raw::c_uint,
        can_generate_method_entry_events: std::os::raw::c_uint,
        can_generate_method_exit_events: std::os::raw::c_uint,
        can_generate_all_class_hook_events: std::os::raw::c_uint,
        can_generate_compiled_method_load_events: std::os::raw::c_uint,
        can_generate_monitor_events: std::os::raw::c_uint,
        can_generate_vm_object_alloc_events: std::os::raw::c_uint,
        can_generate_native_method_bind_events: std::os::raw::c_uint,
        can_generate_garbage_collection_events: std::os::raw::c_uint,
        can_generate_object_free_events: std::os::raw::c_uint,
        can_force_early_return: std::os::raw::c_uint,
        can_get_owned_monitor_stack_depth_info: std::os::raw::c_uint,
        can_get_constant_pool: std::os::raw::c_uint,
        can_set_native_method_prefix: std::os::raw::c_uint,
        can_retransform_classes: std::os::raw::c_uint,
        can_retransform_any_class: std::os::raw::c_uint,
        can_generate_resource_exhaustion_heap_events: std::os::raw::c_uint,
        can_generate_resource_exhaustion_threads_events: std::os::raw::c_uint,
        can_generate_early_vmstart: std::os::raw::c_uint,
        can_generate_early_class_hook_events: std::os::raw::c_uint,
        can_generate_sampled_object_alloc_events: std::os::raw::c_uint,
    ) -> __BindgenBitfieldUnit<[u8; 16usize]> {
        let mut __bindgen_bitfield_unit: __BindgenBitfieldUnit<[u8; 16usize]> = Default::default();
        __bindgen_bitfield_unit.set(0usize, 1u8, {
            let can_tag_objects: u32 = unsafe { std::mem::transmute(can_tag_objects) };
            can_tag_objects as u64
        });
        __bindgen_bitfield_unit.set(1usize, 1u8, {
            let can_generate_field_modification_events: u32 =
                unsafe { std::mem::transmute(can_generate_field_modification_events) };
            can_generate_field_modification_events as u64
        });
        __bindgen_bitfield_unit.set(2usize, 1u8, {
            let can_generate_field_access_events: u32 =
                unsafe { std::mem::transmute(can_generate_field_access_events) };
            can_generate_field_access_events as u64
        });
        __bindgen_bitfield_unit.set(3usize, 1u8, {
            let can_get_bytecodes: u32 = unsafe { std::mem::transmute(can_get_bytecodes) };
            can_get_bytecodes as u64
        });
        __bindgen_bitfield_unit.set(4usize, 1u8, {
            let can_get_synthetic_attribute: u32 =
                unsafe { std::mem::transmute(can_get_synthetic_attribute) };
            can_get_synthetic_attribute as u64
        });
        __bindgen_bitfield_unit.set(5usize, 1u8, {
            let can_get_owned_monitor_info: u32 =
                unsafe { std::mem::transmute(can_get_owned_monitor_info) };
            can_get_owned_monitor_info as u64
        });
        __bindgen_bitfield_unit.set(6usize, 1u8, {
            let can_get_current_contended_monitor: u32 =
                unsafe { std::mem::transmute(can_get_current_contended_monitor) };
            can_get_current_contended_monitor as u64
        });
        __bindgen_bitfield_unit.set(7usize, 1u8, {
            let can_get_monitor_info: u32 = unsafe { std::mem::transmute(can_get_monitor_info) };
            can_get_monitor_info as u64
        });
        __bindgen_bitfield_unit.set(8usize, 1u8, {
            let can_pop_frame: u32 = unsafe { std::mem::transmute(can_pop_frame) };
            can_pop_frame as u64
        });
        __bindgen_bitfield_unit.set(9usize, 1u8, {
            let can_redefine_classes: u32 = unsafe { std::mem::transmute(can_redefine_classes) };
            can_redefine_classes as u64
        });
        __bindgen_bitfield_unit.set(10usize, 1u8, {
            let can_signal_thread: u32 = unsafe { std::mem::transmute(can_signal_thread) };
            can_signal_thread as u64
        });
        __bindgen_bitfield_unit.set(11usize, 1u8, {
            let can_get_source_file_name: u32 =
                unsafe { std::mem::transmute(can_get_source_file_name) };
            can_get_source_file_name as u64
        });
        __bindgen_bitfield_unit.set(12usize, 1u8, {
            let can_get_line_numbers: u32 = unsafe { std::mem::transmute(can_get_line_numbers) };
            can_get_line_numbers as u64
        });
        __bindgen_bitfield_unit.set(13usize, 1u8, {
            let can_get_source_debug_extension: u32 =
                unsafe { std::mem::transmute(can_get_source_debug_extension) };
            can_get_source_debug_extension as u64
        });
        __bindgen_bitfield_unit.set(14usize, 1u8, {
            let can_access_local_variables: u32 =
                unsafe { std::mem::transmute(can_access_local_variables) };
            can_access_local_variables as u64
        });
        __bindgen_bitfield_unit.set(15usize, 1u8, {
            let can_maintain_original_method_order: u32 =
                unsafe { std::mem::transmute(can_maintain_original_method_order) };
            can_maintain_original_method_order as u64
        });
        __bindgen_bitfield_unit.set(16usize, 1u8, {
            let can_generate_single_step_events: u32 =
                unsafe { std::mem::transmute(can_generate_single_step_events) };
            can_generate_single_step_events as u64
        });
        __bindgen_bitfield_unit.set(17usize, 1u8, {
            let can_generate_exception_events: u32 =
                unsafe { std::mem::transmute(can_generate_exception_events) };
            can_generate_exception_events as u64
        });
        __bindgen_bitfield_unit.set(18usize, 1u8, {
            let can_generate_frame_pop_events: u32 =
                unsafe { std::mem::transmute(can_generate_frame_pop_events) };
            can_generate_frame_pop_events as u64
        });
        __bindgen_bitfield_unit.set(19usize, 1u8, {
            let can_generate_breakpoint_events: u32 =
                unsafe { std::mem::transmute(can_generate_breakpoint_events) };
            can_generate_breakpoint_events as u64
        });
        __bindgen_bitfield_unit.set(20usize, 1u8, {
            let can_suspend: u32 = unsafe { std::mem::transmute(can_suspend) };
            can_suspend as u64
        });
        __bindgen_bitfield_unit.set(21usize, 1u8, {
            let can_redefine_any_class: u32 =
                unsafe { std::mem::transmute(can_redefine_any_class) };
            can_redefine_any_class as u64
        });
        __bindgen_bitfield_unit.set(22usize, 1u8, {
            let can_get_current_thread_cpu_time: u32 =
                unsafe { std::mem::transmute(can_get_current_thread_cpu_time) };
            can_get_current_thread_cpu_time as u64
        });
        __bindgen_bitfield_unit.set(23usize, 1u8, {
            let can_get_thread_cpu_time: u32 =
                unsafe { std::mem::transmute(can_get_thread_cpu_time) };
            can_get_thread_cpu_time as u64
        });
        __bindgen_bitfield_unit.set(24usize, 1u8, {
            let can_generate_method_entry_events: u32 =
                unsafe { std::mem::transmute(can_generate_method_entry_events) };
            can_generate_method_entry_events as u64
        });
        __bindgen_bitfield_unit.set(25usize, 1u8, {
            let can_generate_method_exit_events: u32 =
                unsafe { std::mem::transmute(can_generate_method_exit_events) };
            can_generate_method_exit_events as u64
        });
        __bindgen_bitfield_unit.set(26usize, 1u8, {
            let can_generate_all_class_hook_events: u32 =
                unsafe { std::mem::transmute(can_generate_all_class_hook_events) };
            can_generate_all_class_hook_events as u64
        });
        __bindgen_bitfield_unit.set(27usize, 1u8, {
            let can_generate_compiled_method_load_events: u32 =
                unsafe { std::mem::transmute(can_generate_compiled_method_load_events) };
            can_generate_compiled_method_load_events as u64
        });
        __bindgen_bitfield_unit.set(28usize, 1u8, {
            let can_generate_monitor_events: u32 =
                unsafe { std::mem::transmute(can_generate_monitor_events) };
            can_generate_monitor_events as u64
        });
        __bindgen_bitfield_unit.set(29usize, 1u8, {
            let can_generate_vm_object_alloc_events: u32 =
                unsafe { std::mem::transmute(can_generate_vm_object_alloc_events) };
            can_generate_vm_object_alloc_events as u64
        });
        __bindgen_bitfield_unit.set(30usize, 1u8, {
            let can_generate_native_method_bind_events: u32 =
                unsafe { std::mem::transmute(can_generate_native_method_bind_events) };
            can_generate_native_method_bind_events as u64
        });
        __bindgen_bitfield_unit.set(31usize, 1u8, {
            let can_generate_garbage_collection_events: u32 =
                unsafe { std::mem::transmute(can_generate_garbage_collection_events) };
            can_generate_garbage_collection_events as u64
        });
        __bindgen_bitfield_unit.set(32usize, 1u8, {
            let can_generate_object_free_events: u32 =
                unsafe { std::mem::transmute(can_generate_object_free_events) };
            can_generate_object_free_events as u64
        });
        __bindgen_bitfield_unit.set(33usize, 1u8, {
            let can_force_early_return: u32 =
                unsafe { std::mem::transmute(can_force_early_return) };
            can_force_early_return as u64
        });
        __bindgen_bitfield_unit.set(34usize, 1u8, {
            let can_get_owned_monitor_stack_depth_info: u32 =
                unsafe { std::mem::transmute(can_get_owned_monitor_stack_depth_info) };
            can_get_owned_monitor_stack_depth_info as u64
        });
        __bindgen_bitfield_unit.set(35usize, 1u8, {
            let can_get_constant_pool: u32 =
                unsafe { std::mem::transmute(can_get_constant_pool) };
            can_get_constant_pool as u64
        });
        __bindgen_bitfield_unit.set(36usize, 1u8, {
            let can_set_native_method_prefix: u32 =
                unsafe { std::mem::transmute(can_set_native_method_prefix) };
            can_set_native_method_prefix as u64
        });
        __bindgen_bitfield_unit.set(37usize, 1u8, {
            let can_retransform_classes: u32 =
                unsafe { std::mem::transmute(can_retransform_classes) };
            can_retransform_classes as u64
        });
        __bindgen_bitfield_unit.set(38usize, 1u8, {
            let can_retransform_any_class: u32 =
                unsafe { std::mem::transmute(can_retransform_any_class) };
            can_retransform_any_class as u64
        });
        __bindgen_bitfield_unit.set(39usize, 1u8, {
            let can_generate_resource_exhaustion_heap_events: u32 =
                unsafe { std::mem::transmute(can_generate_resource_exhaustion_heap_events) };
            can_generate_resource_exhaustion_heap_events as u64
        });
        __bindgen_bitfield_unit.set(40usize, 1u8, {
            let can_generate_resource_exhaustion_threads_events: u32 =
                unsafe { std::mem::transmute(can_generate_resource_exhaustion_threads_events) };
            can_generate_resource_exhaustion_threads_events as u64
        });
        __bindgen_bitfield_unit.set(41usize, 1u8, {
            let can_generate_early_vmstart: u32 =
                unsafe { std::mem::transmute(can_generate_early_vmstart) };
            can_generate_early_vmstart as u64
        });
        __bindgen_bitfield_unit.set(42usize, 1u8, {
            let can_generate_early_class_hook_events: u32 =
                unsafe { std::mem::transmute(can_generate_early_class_hook_events) };
            can_generate_early_class_hook_events as u64
        });
        __bindgen_bitfield_unit.set(43usize, 1u8, {
            let can_generate_sampled_object_alloc_events: u32 =
                unsafe { std::mem::transmute(can_generate_sampled_object_alloc_events) };
            can_generate_sampled_object_alloc_events as u64
        });
        __bindgen_bitfield_unit
    }
}

pub type jvmtiEventReserved = Option<unsafe extern "C" fn()>;
pub type jvmtiEventBreakpoint = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        location: jlocation,
    ),
>;
pub type jvmtiEventClassFileLoadHook = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        class_being_redefined: jclass,
        loader: jobject,
        name: *const std::os::raw::c_char,
        protection_domain: jobject,
        class_data_len: jint,
        class_data: *const std::os::raw::c_uchar,
        new_class_data_len: *mut jint,
        new_class_data: *mut *mut std::os::raw::c_uchar,
    ),
>;
pub type jvmtiEventClassLoad = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        klass: jclass,
    ),
>;
pub type jvmtiEventClassPrepare = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        klass: jclass,
    ),
>;
pub type jvmtiEventCompiledMethodLoad = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        method: jmethodID,
        code_size: jint,
        code_addr: *const std::os::raw::c_void,
        map_length: jint,
        map: *const jvmtiAddrLocationMap,
        compile_info: *const std::os::raw::c_void,
    ),
>;
pub type jvmtiEventCompiledMethodUnload = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        method: jmethodID,
        code_addr: *const std::os::raw::c_void,
    ),
>;
pub type jvmtiEventDataDumpRequest =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv)>;
pub type jvmtiEventDynamicCodeGenerated = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        name: *const std::os::raw::c_char,
        address: *const std::os::raw::c_void,
        length: jint,
    ),
>;
pub type jvmtiEventException = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        location: jlocation,
        exception: jobject,
        catch_method: jmethodID,
        catch_location: jlocation,
    ),
>;
pub type jvmtiEventExceptionCatch = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        location: jlocation,
        exception: jobject,
    ),
>;
pub type jvmtiEventFieldAccess = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        location: jlocation,
        field_klass: jclass,
        object: jobject,
        field: jfieldID,
    ),
>;
pub type jvmtiEventFieldModification = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        location: jlocation,
        field_klass: jclass,
        object: jobject,
        field: jfieldID,
        signature_type: std::os::raw::c_char,
        new_value: jvalue,
    ),
>;
pub type jvmtiEventFramePop = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        was_popped_by_exception: jboolean,
    ),
>;
pub type jvmtiEventGarbageCollectionFinish =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv)>;
pub type jvmtiEventGarbageCollectionStart =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv)>;
pub type jvmtiEventMethodEntry = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
    ),
>;
pub type jvmtiEventMethodExit = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        was_popped_by_exception: jboolean,
        return_value: jvalue,
    ),
>;
pub type jvmtiEventMonitorContendedEnter = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        object: jobject,
    ),
>;
pub type jvmtiEventMonitorContendedEntered = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        object: jobject,
    ),
>;
pub type jvmtiEventMonitorWait = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        object: jobject,
        timeout: jlong,
    ),
>;
pub type jvmtiEventMonitorWaited = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        object: jobject,
        timed_out: jboolean,
    ),
>;
pub type jvmtiEventNativeMethodBind = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        address: *mut std::os::raw::c_void,
        new_address_ptr: *mut *mut std::os::raw::c_void,
    ),
>;
pub type jvmtiEventObjectFree =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, tag: jlong)>;
pub type jvmtiEventResourceExhausted = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        flags: jint,
        reserved: *const std::os::raw::c_void,
        description: *const std::os::raw::c_char,
    ),
>;
pub type jvmtiEventSampledObjectAlloc = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        object: jobject,
        object_klass: jclass,
        size: jlong,
    ),
>;
pub type jvmtiEventSingleStep = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        method: jmethodID,
        location: jlocation,
    ),
>;
pub type jvmtiEventThreadEnd = Option<
    unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, jni_env: *mut JNIEnv, thread: jthread),
>;
pub type jvmtiEventThreadStart = Option<
    unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, jni_env: *mut JNIEnv, thread: jthread),
>;
pub type jvmtiEventVMDeath =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, jni_env: *mut JNIEnv)>;
pub type jvmtiEventVMInit = Option<
    unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, jni_env: *mut JNIEnv, thread: jthread),
>;
pub type jvmtiEventVMObjectAlloc = Option<
    unsafe extern "C" fn(
        jvmti_env: *mut jvmtiEnv,
        jni_env: *mut JNIEnv,
        thread: jthread,
        object: jobject,
        object_klass: jclass,
        size: jlong,
    ),
>;
pub type jvmtiEventVMStart =
Option<unsafe extern "C" fn(jvmti_env: *mut jvmtiEnv, jni_env: *mut JNIEnv)>;

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiEventCallbacks {
    pub VMInit: jvmtiEventVMInit,
    pub VMDeath: jvmtiEventVMDeath,
    pub ThreadStart: jvmtiEventThreadStart,
    pub ThreadEnd: jvmtiEventThreadEnd,
    pub ClassFileLoadHook: jvmtiEventClassFileLoadHook,
    pub ClassLoad: jvmtiEventClassLoad,
    pub ClassPrepare: jvmtiEventClassPrepare,
    pub VMStart: jvmtiEventVMStart,
    pub Exception: jvmtiEventException,
    pub ExceptionCatch: jvmtiEventExceptionCatch,
    pub SingleStep: jvmtiEventSingleStep,
    pub FramePop: jvmtiEventFramePop,
    pub Breakpoint: jvmtiEventBreakpoint,
    pub FieldAccess: jvmtiEventFieldAccess,
    pub FieldModification: jvmtiEventFieldModification,
    pub MethodEntry: jvmtiEventMethodEntry,
    pub MethodExit: jvmtiEventMethodExit,
    pub NativeMethodBind: jvmtiEventNativeMethodBind,
    pub CompiledMethodLoad: jvmtiEventCompiledMethodLoad,
    pub CompiledMethodUnload: jvmtiEventCompiledMethodUnload,
    pub DynamicCodeGenerated: jvmtiEventDynamicCodeGenerated,
    pub DataDumpRequest: jvmtiEventDataDumpRequest,
    pub reserved72: jvmtiEventReserved,
    pub MonitorWait: jvmtiEventMonitorWait,
    pub MonitorWaited: jvmtiEventMonitorWaited,
    pub MonitorContendedEnter: jvmtiEventMonitorContendedEnter,
    pub MonitorContendedEntered: jvmtiEventMonitorContendedEntered,
    pub reserved77: jvmtiEventReserved,
    pub reserved78: jvmtiEventReserved,
    pub reserved79: jvmtiEventReserved,
    pub ResourceExhausted: jvmtiEventResourceExhausted,
    pub GarbageCollectionStart: jvmtiEventGarbageCollectionStart,
    pub GarbageCollectionFinish: jvmtiEventGarbageCollectionFinish,
    pub ObjectFree: jvmtiEventObjectFree,
    pub VMObjectAlloc: jvmtiEventVMObjectAlloc,
    pub reserved85: jvmtiEventReserved,
    pub SampledObjectAlloc: jvmtiEventSampledObjectAlloc,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct jvmtiInterface_1_ {
    pub reserved1: *mut std::os::raw::c_void,
    pub SetEventNotificationMode: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            mode: jvmtiEventMode,
            event_type: jvmtiEvent,
            event_thread: jthread,
            ...
        ) -> jvmtiError,
    >,
    pub GetAllModules: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            module_count_ptr: *mut jint,
            modules_ptr: *mut *mut jobject,
        ) -> jvmtiError,
    >,
    pub GetAllThreads: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            threads_count_ptr: *mut jint,
            threads_ptr: *mut *mut jthread,
        ) -> jvmtiError,
    >,
    pub SuspendThread: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread) -> jvmtiError,
    >,
    pub ResumeThread: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread) -> jvmtiError,
    >,
    pub StopThread: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread, exception: jobject) -> jvmtiError,
    >,
    pub InterruptThread: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread) -> jvmtiError,
    >,
    pub GetThreadInfo: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            info_ptr: *mut jvmtiThreadInfo,
        ) -> jvmtiError,
    >,
    pub GetOwnedMonitorInfo: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            owned_monitor_count_ptr: *mut jint,
            owned_monitors_ptr: *mut *mut jobject,
        ) -> jvmtiError,
    >,
    pub GetCurrentContendedMonitor: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            monitor_ptr: *mut jobject,
        ) -> jvmtiError,
    >,
    pub RunAgentThread: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            proc_: jvmtiStartFunction,
            arg: *const std::os::raw::c_void,
            priority: jint,
        ) -> jvmtiError,
    >,
    pub GetTopThreadGroups: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            group_count_ptr: *mut jint,
            groups_ptr: *mut *mut jthreadGroup,
        ) -> jvmtiError,
    >,
    pub GetThreadGroupInfo: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            group: jthreadGroup,
            info_ptr: *mut jvmtiThreadGroupInfo,
        ) -> jvmtiError,
    >,
    pub GetThreadGroupChildren: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            group: jthreadGroup,
            thread_count_ptr: *mut jint,
            threads_ptr: *mut *mut jthread,
            group_count_ptr: *mut jint,
            groups_ptr: *mut *mut jthreadGroup,
        ) -> jvmtiError,
    >,
    pub GetFrameCount: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            count_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetThreadState: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            thread_state_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetCurrentThread: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread_ptr: *mut jthread) -> jvmtiError,
    >,
    pub GetFrameLocation: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            method_ptr: *mut jmethodID,
            location_ptr: *mut jlocation,
        ) -> jvmtiError,
    >,
    pub NotifyFramePop: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread, depth: jint) -> jvmtiError,
    >,
    pub GetLocalObject: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value_ptr: *mut jobject,
        ) -> jvmtiError,
    >,
    pub GetLocalInt: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetLocalLong: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value_ptr: *mut jlong,
        ) -> jvmtiError,
    >,
    pub GetLocalFloat: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value_ptr: *mut jfloat,
        ) -> jvmtiError,
    >,
    pub GetLocalDouble: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value_ptr: *mut jdouble,
        ) -> jvmtiError,
    >,
    pub SetLocalObject: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value: jobject,
        ) -> jvmtiError,
    >,
    pub SetLocalInt: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value: jint,
        ) -> jvmtiError,
    >,
    pub SetLocalLong: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value: jlong,
        ) -> jvmtiError,
    >,
    pub SetLocalFloat: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value: jfloat,
        ) -> jvmtiError,
    >,
    pub SetLocalDouble: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            slot: jint,
            value: jdouble,
        ) -> jvmtiError,
    >,
    pub CreateRawMonitor: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            name: *const std::os::raw::c_char,
            monitor_ptr: *mut jrawMonitorID,
        ) -> jvmtiError,
    >,
    pub DestroyRawMonitor: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, monitor: jrawMonitorID) -> jvmtiError,
    >,
    pub RawMonitorEnter: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, monitor: jrawMonitorID) -> jvmtiError,
    >,
    pub RawMonitorExit: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, monitor: jrawMonitorID) -> jvmtiError,
    >,
    pub RawMonitorWait: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            monitor: jrawMonitorID,
            millis: jlong,
        ) -> jvmtiError,
    >,
    pub RawMonitorNotify: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, monitor: jrawMonitorID) -> jvmtiError,
    >,
    pub RawMonitorNotifyAll: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, monitor: jrawMonitorID) -> jvmtiError,
    >,
    pub SetBreakpoint: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            location: jlocation,
        ) -> jvmtiError,
    >,
    pub ClearBreakpoint: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            location: jlocation,
        ) -> jvmtiError,
    >,
    pub GetNamedModule: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            class_loader: jobject,
            package_name: *const std::os::raw::c_char,
            module_ptr: *mut jobject,
        ) -> jvmtiError,
    >,
    pub SetFieldAccessWatch: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, klass: jclass, field: jfieldID) -> jvmtiError,
    >,
    pub ClearFieldAccessWatch: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, klass: jclass, field: jfieldID) -> jvmtiError,
    >,
    pub SetFieldModificationWatch: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, klass: jclass, field: jfieldID) -> jvmtiError,
    >,
    pub ClearFieldModificationWatch: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, klass: jclass, field: jfieldID) -> jvmtiError,
    >,
    pub IsModifiableClass: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            is_modifiable_class_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub Allocate: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            size: jlong,
            mem_ptr: *mut *mut std::os::raw::c_uchar,
        ) -> jvmtiError,
    >,
    pub Deallocate: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, mem: *mut std::os::raw::c_uchar) -> jvmtiError,
    >,
    pub GetClassSignature: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            signature_ptr: *mut *mut std::os::raw::c_char,
            generic_ptr: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetClassStatus: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            status_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetSourceFileName: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            source_name_ptr: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetClassModifiers: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            modifiers_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetClassMethods: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            method_count_ptr: *mut jint,
            methods_ptr: *mut *mut jmethodID,
        ) -> jvmtiError,
    >,
    pub GetClassFields: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            field_count_ptr: *mut jint,
            fields_ptr: *mut *mut jfieldID,
        ) -> jvmtiError,
    >,
    pub GetImplementedInterfaces: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            interface_count_ptr: *mut jint,
            interfaces_ptr: *mut *mut jclass,
        ) -> jvmtiError,
    >,
    pub IsInterface: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            is_interface_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub IsArrayClass: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            is_array_class_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub GetClassLoader: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            classloader_ptr: *mut jobject,
        ) -> jvmtiError,
    >,
    pub GetObjectHashCode: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            object: jobject,
            hash_code_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetObjectMonitorUsage: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            object: jobject,
            info_ptr: *mut jvmtiMonitorUsage,
        ) -> jvmtiError,
    >,
    pub GetFieldName: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            field: jfieldID,
            name_ptr: *mut *mut std::os::raw::c_char,
            signature_ptr: *mut *mut std::os::raw::c_char,
            generic_ptr: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetFieldDeclaringClass: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            field: jfieldID,
            declaring_class_ptr: *mut jclass,
        ) -> jvmtiError,
    >,
    pub GetFieldModifiers: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            field: jfieldID,
            modifiers_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub IsFieldSynthetic: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            field: jfieldID,
            is_synthetic_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub GetMethodName: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            name_ptr: *mut *mut std::os::raw::c_char,
            signature_ptr: *mut *mut std::os::raw::c_char,
            generic_ptr: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetMethodDeclaringClass: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            declaring_class_ptr: *mut jclass,
        ) -> jvmtiError,
    >,
    pub GetMethodModifiers: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            modifiers_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub reserved67: *mut std::os::raw::c_void,
    pub GetMaxLocals: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            max_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetArgumentsSize: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            size_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetLineNumberTable: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            entry_count_ptr: *mut jint,
            table_ptr: *mut *mut jvmtiLineNumberEntry,
        ) -> jvmtiError,
    >,
    pub GetMethodLocation: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            start_location_ptr: *mut jlocation,
            end_location_ptr: *mut jlocation,
        ) -> jvmtiError,
    >,
    pub GetLocalVariableTable: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            entry_count_ptr: *mut jint,
            table_ptr: *mut *mut jvmtiLocalVariableEntry,
        ) -> jvmtiError,
    >,
    pub SetNativeMethodPrefix: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            prefix: *const std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub SetNativeMethodPrefixes: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            prefix_count: jint,
            prefixes: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetBytecodes: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            bytecode_count_ptr: *mut jint,
            bytecodes_ptr: *mut *mut std::os::raw::c_uchar,
        ) -> jvmtiError,
    >,
    pub IsMethodNative: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            is_native_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub IsMethodSynthetic: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            is_synthetic_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub GetLoadedClasses: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            class_count_ptr: *mut jint,
            classes_ptr: *mut *mut jclass,
        ) -> jvmtiError,
    >,
    pub GetClassLoaderClasses: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            initiating_loader: jobject,
            class_count_ptr: *mut jint,
            classes_ptr: *mut *mut jclass,
        ) -> jvmtiError,
    >,
    pub PopFrame: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread) -> jvmtiError,
    >,
    pub ForceEarlyReturnObject: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread, value: jobject) -> jvmtiError,
    >,
    pub ForceEarlyReturnInt: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread, value: jint) -> jvmtiError,
    >,
    pub ForceEarlyReturnLong: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread, value: jlong) -> jvmtiError,
    >,
    pub ForceEarlyReturnFloat: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread, value: jfloat) -> jvmtiError,
    >,
    pub ForceEarlyReturnDouble: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread, value: jdouble) -> jvmtiError,
    >,
    pub ForceEarlyReturnVoid: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, thread: jthread) -> jvmtiError,
    >,
    pub RedefineClasses: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            class_count: jint,
            class_definitions: *const jvmtiClassDefinition,
        ) -> jvmtiError,
    >,
    pub GetVersionNumber: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, version_ptr: *mut jint) -> jvmtiError,
    >,
    pub GetCapabilities: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            capabilities_ptr: *mut jvmtiCapabilities,
        ) -> jvmtiError,
    >,
    pub GetSourceDebugExtension: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            source_debug_extension_ptr: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub IsMethodObsolete: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            method: jmethodID,
            is_obsolete_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub SuspendThreadList: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            request_count: jint,
            request_list: *const jthread,
            results: *mut jvmtiError,
        ) -> jvmtiError,
    >,
    pub ResumeThreadList: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            request_count: jint,
            request_list: *const jthread,
            results: *mut jvmtiError,
        ) -> jvmtiError,
    >,
    pub AddModuleReads: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, module: jobject, to_module: jobject) -> jvmtiError,
    >,
    pub AddModuleExports: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            module: jobject,
            pkg_name: *const std::os::raw::c_char,
            to_module: jobject,
        ) -> jvmtiError,
    >,
    pub AddModuleOpens: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            module: jobject,
            pkg_name: *const std::os::raw::c_char,
            to_module: jobject,
        ) -> jvmtiError,
    >,
    pub AddModuleUses: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, module: jobject, service: jclass) -> jvmtiError,
    >,
    pub AddModuleProvides: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            module: jobject,
            service: jclass,
            impl_class: jclass,
        ) -> jvmtiError,
    >,
    pub IsModifiableModule: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            module: jobject,
            is_modifiable_module_ptr: *mut jboolean,
        ) -> jvmtiError,
    >,
    pub GetAllStackTraces: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            max_frame_count: jint,
            stack_info_ptr: *mut *mut jvmtiStackInfo,
            thread_count_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetThreadListStackTraces: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread_count: jint,
            thread_list: *const jthread,
            max_frame_count: jint,
            stack_info_ptr: *mut *mut jvmtiStackInfo,
        ) -> jvmtiError,
    >,
    pub GetThreadLocalStorage: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            data_ptr: *mut *mut std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub SetThreadLocalStorage: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            data: *const std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub GetStackTrace: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            start_depth: jint,
            max_frame_count: jint,
            frame_buffer: *mut jvmtiFrameInfo,
            count_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub reserved105: *mut std::os::raw::c_void,
    pub GetTag: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            object: jobject,
            tag_ptr: *mut jlong,
        ) -> jvmtiError,
    >,
    pub SetTag: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, object: jobject, tag: jlong) -> jvmtiError,
    >,
    pub ForceGarbageCollection:
    Option<unsafe extern "C" fn(env: *mut jvmtiEnv) -> jvmtiError>,
    pub IterateOverObjectsReachableFromObject: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            object: jobject,
            object_reference_callback: jvmtiObjectReferenceCallback,
            user_data: *const std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub IterateOverReachableObjects: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            heap_root_callback: jvmtiHeapRootCallback,
            stack_ref_callback: jvmtiStackReferenceCallback,
            object_ref_callback: jvmtiObjectReferenceCallback,
            user_data: *const std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub IterateOverHeap: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            object_filter: jvmtiHeapObjectFilter,
            heap_object_callback: jvmtiHeapObjectCallback,
            user_data: *const std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub IterateOverInstancesOfClass: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            object_filter: jvmtiHeapObjectFilter,
            heap_object_callback: jvmtiHeapObjectCallback,
            user_data: *const std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub reserved113: *mut std::os::raw::c_void,
    pub GetObjectsWithTags: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            tag_count: jint,
            tags: *const jlong,
            count_ptr: *mut jint,
            object_result_ptr: *mut *mut jobject,
            tag_result_ptr: *mut *mut jlong,
        ) -> jvmtiError,
    >,
    pub FollowReferences: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            heap_filter: jint,
            klass: jclass,
            initial_object: jobject,
            callbacks: *const jvmtiHeapCallbacks,
            user_data: *const std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub IterateThroughHeap: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            heap_filter: jint,
            klass: jclass,
            callbacks: *const jvmtiHeapCallbacks,
            user_data: *const std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub reserved117: *mut std::os::raw::c_void,
    pub reserved118: *mut std::os::raw::c_void,
    pub reserved119: *mut std::os::raw::c_void,
    pub SetJNIFunctionTable: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            function_table: *const jniNativeInterface,
        ) -> jvmtiError,
    >,
    pub GetJNIFunctionTable: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            function_table: *mut *mut jniNativeInterface,
        ) -> jvmtiError,
    >,
    pub SetEventCallbacks: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            callbacks: *const jvmtiEventCallbacks,
            size_of_callbacks: jint,
        ) -> jvmtiError,
    >,
    pub GenerateEvents: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, event_type: jvmtiEvent) -> jvmtiError,
    >,
    pub GetExtensionFunctions: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            extension_count_ptr: *mut jint,
            extensions: *mut *mut jvmtiExtensionFunctionInfo,
        ) -> jvmtiError,
    >,
    pub GetExtensionEvents: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            extension_count_ptr: *mut jint,
            extensions: *mut *mut jvmtiExtensionEventInfo,
        ) -> jvmtiError,
    >,
    pub SetExtensionEventCallback: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            extension_event_index: jint,
            callback: jvmtiExtensionEvent,
        ) -> jvmtiError,
    >,
    pub DisposeEnvironment:
    Option<unsafe extern "C" fn(env: *mut jvmtiEnv) -> jvmtiError>,
    pub GetErrorName: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            error: jvmtiError,
            name_ptr: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetJLocationFormat: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            format_ptr: *mut jvmtiJlocationFormat,
        ) -> jvmtiError,
    >,
    pub GetSystemProperties: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            count_ptr: *mut jint,
            property_ptr: *mut *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetSystemProperty: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            property: *const std::os::raw::c_char,
            value_ptr: *mut *mut std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub SetSystemProperty: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            property: *const std::os::raw::c_char,
            value_ptr: *const std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub GetPhase: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, phase_ptr: *mut jvmtiPhase) -> jvmtiError,
    >,
    pub GetCurrentThreadCpuTimerInfo: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, info_ptr: *mut jvmtiTimerInfo) -> jvmtiError,
    >,
    pub GetCurrentThreadCpuTime: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, nanos_ptr: *mut jlong) -> jvmtiError,
    >,
    pub GetThreadCpuTimerInfo: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, info_ptr: *mut jvmtiTimerInfo) -> jvmtiError,
    >,
    pub GetThreadCpuTime: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            nanos_ptr: *mut jlong,
        ) -> jvmtiError,
    >,
    pub GetTimerInfo: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, info_ptr: *mut jvmtiTimerInfo) -> jvmtiError,
    >,
    pub GetTime: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, nanos_ptr: *mut jlong) -> jvmtiError,
    >,
    pub GetPotentialCapabilities: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            capabilities_ptr: *mut jvmtiCapabilities,
        ) -> jvmtiError,
    >,
    pub reserved141: *mut std::os::raw::c_void,
    pub AddCapabilities: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            capabilities_ptr: *const jvmtiCapabilities,
        ) -> jvmtiError,
    >,
    pub RelinquishCapabilities: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            capabilities_ptr: *const jvmtiCapabilities,
        ) -> jvmtiError,
    >,
    pub GetAvailableProcessors: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, processor_count_ptr: *mut jint) -> jvmtiError,
    >,
    pub GetClassVersionNumbers: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            minor_version_ptr: *mut jint,
            major_version_ptr: *mut jint,
        ) -> jvmtiError,
    >,
    pub GetConstantPool: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            klass: jclass,
            constant_pool_count_ptr: *mut jint,
            constant_pool_byte_count_ptr: *mut jint,
            constant_pool_bytes_ptr: *mut *mut std::os::raw::c_uchar,
        ) -> jvmtiError,
    >,
    pub GetEnvironmentLocalStorage: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            data_ptr: *mut *mut std::os::raw::c_void,
        ) -> jvmtiError,
    >,
    pub SetEnvironmentLocalStorage: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, data: *const std::os::raw::c_void) -> jvmtiError,
    >,
    pub AddToBootstrapClassLoaderSearch: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            segment: *const std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub SetVerboseFlag: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            flag: jvmtiVerboseFlag,
            value: jboolean,
        ) -> jvmtiError,
    >,
    pub AddToSystemClassLoaderSearch: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            segment: *const std::os::raw::c_char,
        ) -> jvmtiError,
    >,
    pub RetransformClasses: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            class_count: jint,
            classes: *const jclass,
        ) -> jvmtiError,
    >,
    pub GetOwnedMonitorStackDepthInfo: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            monitor_info_count_ptr: *mut jint,
            monitor_info_ptr: *mut *mut jvmtiMonitorStackDepthInfo,
        ) -> jvmtiError,
    >,
    pub GetObjectSize: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            object: jobject,
            size_ptr: *mut jlong,
        ) -> jvmtiError,
    >,
    pub GetLocalInstance: Option<
        unsafe extern "C" fn(
            env: *mut jvmtiEnv,
            thread: jthread,
            depth: jint,
            value_ptr: *mut jobject,
        ) -> jvmtiError,
    >,
    pub SetHeapSamplingInterval: Option<
        unsafe extern "C" fn(env: *mut jvmtiEnv, sampling_interval: jint) -> jvmtiError,
    >,
}

pub type jvmtiInterface_1 = jvmtiInterface_1_;

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct _jvmtiEnv {
    pub functions: *const jvmtiInterface_1_,
}
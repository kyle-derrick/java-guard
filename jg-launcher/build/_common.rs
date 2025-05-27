pub const VERSION: &str = "{version}";
pub const KEY_VERSION: &str = "{key_version}";
pub const INTERNAL_URL_CONNECTION_CLASS: &str = "{internalUrlConnectionClass}";
pub const INTERNAL_URL_CONNECTION_METHOD: &str = "{internalUrlConnectionMethod}";
pub const INTERNAL_URL_CONNECTION_DESC: &str = "{internalUrlConnectionDesc}";
pub const RESOURCE_DECRYPT_NATIVE_CLASS: &str = "{resourceDecryptNativeClass}";
pub const RESOURCE_DECRYPT_NATIVE_METHOD: &str = "{resourceDecryptNativeMethod}";
pub const RESOURCE_DECRYPT_NATIVE_DESC: &str = "{resourceDecryptNativeDesc}";
pub fn runtime_classes() -> &'static [u8] {{
    include_bytes!("runtime.classes")
}}
// pub fn transform_mod() -> &'static [u8] {{
//     // include_bytes!("transform.mod")
// }}
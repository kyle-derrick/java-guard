pub const VERSION: &str = "{version}";
pub const KEY_VERSION: &str = "{key_version}";
pub const KEY_CONTENT: &str = "{}";
pub fn runtime_classes() -> &'static [u8] {{
    include_bytes!("runtime.classes")
}}
pub fn transform_mod() -> &'static [u8] {{
    include_bytes!("transform.mod")
}}

pub const SIGN_LEN_HEX_LEN: usize = 4;
pub const SIGN_SUFFIX: &str = ".sign";
pub const SIGNS_SUFFIX: &str = ".signs";
pub const MANIFEST_FILE: &str = "META-INF/MANIFEST.MF";
pub const MAIN_CLASS_PREFIX: &str = "Main-Class:";


include!(concat!(env!("OUT_DIR"), "/_common.rs"));
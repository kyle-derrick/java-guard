#[cfg(not(feature = "dev"))]
use ring::signature::{UnparsedPublicKey, ED25519};

#[allow(unused)]
#[cfg(unix)]
const PATH_LIST_SEPARATOR: char = ':';

#[allow(unused)]
#[cfg(windows)]
const PATH_LIST_SEPARATOR: char = ';';

pub const MAGIC_LEN:usize = 5;
pub const SIGN_LEN:usize = 64;

#[cfg(not(feature = "dev"))]
pub const SIGN_LEN_HEX_LEN: usize = 4;
// pub const SIGN_SUFFIX: &str = ".sign";
// pub const SIGNS_SUFFIX: &str = ".signs";
pub const MANIFEST_FILE: &str = "META-INF/MANIFEST.MF";
pub const MAIN_CLASS_PREFIX: &str = "Main-Class:";

#[allow(unused)]
pub const URL_CLASS_NAME: &str = "java/net/URL";

pub const GET_SYSTEM_CLASS_LOADER_METHOD: &str = "getSystemClassLoader";
pub const GET_SYSTEM_CLASS_LOADER_METHOD_DESC: &str = "()Ljava/lang/ClassLoader;";
#[allow(unused)]
pub const ENCRYPT_BLOCK: usize = 8 * 1024;

include!(concat!(env!("OUT_DIR"), "/_common.rs"));

#[cfg(not(feature = "dev"))]
pub fn pub_key_pair() -> UnparsedPublicKey<[u8;32]> {
    UnparsedPublicKey::new(&ED25519, pub_key())
}
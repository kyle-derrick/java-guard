use ring::signature::{UnparsedPublicKey, ED25519};

#[allow(unused)]
#[cfg(unix)]
const PATH_LIST_SEPARATOR: char = ':';

#[allow(unused)]
#[cfg(windows)]
const PATH_LIST_SEPARATOR: char = ';';

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

pub const ENCRYPT_DATA_TAG: &[u8] = "<SecretBox>".as_bytes();

include!(concat!(env!("OUT_DIR"), "/_common.rs"));

pub fn pub_key_pair() -> UnparsedPublicKey<&'static [u8]> {
    UnparsedPublicKey::new(&ED25519, PUB_KEY.as_slice())
}
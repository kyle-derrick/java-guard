use ring::signature::{UnparsedPublicKey, ED25519};

#[cfg(unix)]
const PATH_LIST_SEPARATOR: char = ':';

#[cfg(windows)]
const PATH_LIST_SEPARATOR: char = ';';

pub const SIGN_LEN_HEX_LEN: usize = 4;
pub const SIGN_SUFFIX: &str = ".sign";
pub const SIGNS_SUFFIX: &str = ".signs";
pub const MANIFEST_FILE: &str = "META-INF/MANIFEST.MF";
pub const MAIN_CLASS_PREFIX: &str = "Main-Class:";

pub const ENCRYPT_DATA_TAG: &[u8] = "<SecretBox>".as_bytes();

include!(concat!(env!("OUT_DIR"), "/_common.rs"));

pub fn pub_key_pair() -> UnparsedPublicKey<&'static [u8]> {
    UnparsedPublicKey::new(&ED25519, PUB_KEY.as_slice())
}
mod build_config;

use std::env;
use std::fs;
use std::path::Path;

use crate::build_config::*;

fn main() {
    let out_dir = env::var("OUT_DIR").unwrap();
    println!("{out_dir}");
    let dest_path = Path::new(&out_dir).join("_common.rs");

    let version = env::var("CARGO_PKG_VERSION").unwrap();
    
    // if !Path::exists(Path::new(PUB_KEY_FILE)) {
    //     panic!("not found pub key file: {}", PUB_KEY_FILE);
    // }
    // let key_content = match fs::read(PUB_KEY_FILE) {
    //     Err(e) => panic!("pub key file read failed: {}", e),
    //     Ok(content) => content
    // };
    // let mut hasher = Sha256::new();
    // hasher.update(SALT_SOURCE);
    // let salt = hasher.finalize().to_vec();
    //
    // let mut hasher = Sha256::new();
    // let salt_split_index = salt.len()/2;
    // hasher.update(&salt[0..salt_split_index]);
    // hasher.update(&key_content);
    // hasher.update(&salt[salt_split_index..]);
    // let key_version = hasher.finalize().encode_hex::<String>();

    println!(">>> {:#?}", PUB_KEY);
    let common_content = format!(r#"
pub const VERSION: &str = "{}";
pub const KEY_VERSION: &str = "{}";
pub const KEY_CONTENT: &str = "{}";"#, 
        version, SIGN_KEY_VERSION, "");
    fs::write(&dest_path, &common_content).unwrap();
}

mod build_config;
mod bytes_get_generator;

use std::fs::File;
use std::io::Write;
use std::path::Path;
use std::{env, fs};

use crate::build_config::*;

const RUNTIME_CLASSES: &str = "runtime.classes";
// const TRANSFORM_MOD: &str = "transform.mod";
const PUB_KEY_NAME: &str = "pub_key";
const INNER_KEY_NAME: &str = "inner_key";
const RESOURCE_KEY_NAME: &str = "resource_key";

fn main() {
    let cargo_dir = env::var("CARGO_MANIFEST_DIR").unwrap();
    let out_dir = env::var("OUT_DIR").unwrap();
    println!("{out_dir}");
    let out_path = Path::new(&out_dir);
    let dest_path = out_path.join("_common.rs");

    let app_version = env::var("CARGO_PKG_VERSION").unwrap();

    let ext_path = Path::new(&cargo_dir).join("build").join("ext");
    let runtime_classes_path = ext_path.join(RUNTIME_CLASSES);
    // let transform_mod_path = ext_path.join(TRANSFORM_MOD);

    fs::copy(runtime_classes_path, out_path.join(RUNTIME_CLASSES)).unwrap();
    // fs::copy(transform_mod_path, out_path.join(TRANSFORM_MOD)).unwrap();
    #[warn(named_arguments_used_positionally)]
    let common_content = format!(include_str!("_common.rs"),
                                 version = app_version, key_version = SIGN_KEY_VERSION);
    let mut file = File::create(&dest_path).expect("cannot generate common.rs");
    let f = &mut file;
    write_file(f, &common_content);

    write_file(f, &generate_func_field(PUB_KEY_NAME));
    write_file(f, &generate_func_field(INNER_KEY_NAME));
    write_file(f, &generate_func_field(RESOURCE_KEY_NAME));
    write_file(f, &bytes_get_generator::get_common_func_code());
    for item in &bytes_get_generator::generate_func_code(PUB_KEY, PUB_KEY_NAME) {
        write_file(f, item);
    }
    for item in &bytes_get_generator::generate_func_code(KEY, INNER_KEY_NAME) {
        write_file(f, item);
    }
    for item in &bytes_get_generator::generate_func_code(RESOURCE_KEY, RESOURCE_KEY_NAME) {
        write_file(f, item);
    }
}

fn generate_func_field(name: &str) -> String {
    format!("lazy_static::lazy_static! {{ pub static ref {}: Vec<u8> = {}(); }}", name.to_ascii_uppercase(), name)
}

fn write_file(file: &mut File, content: &str) {
    file.write(content.as_bytes()).expect("generate common.rs failed!");
    file.write(&[b'\n']).expect("generate common.rs failed!");
}

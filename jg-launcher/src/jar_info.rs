use crate::common::{MAIN_CLASS_PREFIX, MANIFEST_FILE, SIGN_LEN_HEX_LEN};
use crate::util::byte_utils;
use base64::engine::general_purpose::STANDARD;
use base64::Engine;
use std::fs::File;
// use file_lock::{FileLock, FileOptions};
use std::io;
use zip::ZipArchive;

#[derive(Debug)]
pub struct JarInfo {
    path: String,
    // file: FileLock,
    signature: Vec<u8>,
    main_class: String
}

// const MANIFEST_FILE: &str = "META-INF/MANIFEST.MF";
// const MAIN_CLASS_PREFIX: &str = "Main-Class:";
// const SIGNATURE_PREFIX: &str = "JG-Signature:";

impl JarInfo {
    pub fn parse(path: &str) -> Self {
        let mut archive =  ZipArchive::new(File::open(path).expect(&format!("can not open jar: {}", path)))
            .expect(&format!("can not open jar: {}", path));
        let manifest = archive.by_name(MANIFEST_FILE).expect("not found MANIFEST.MF in jar");
        let manifest_content = io::read_to_string(manifest).expect("cannot read MANIFEST.MF in jar");
        let mut signature = None;
        let mut main_class = None;
        manifest_content.lines().for_each(|line| {
            if line.starts_with(MAIN_CLASS_PREFIX) {
                if main_class.is_none() {
                    main_class = Some(line[MAIN_CLASS_PREFIX.len()..].trim().to_string());
                }
            // } else if line.starts_with(SIGNATURE_PREFIX) {
            //     if signature.is_none() {
            //         signature = Some(line[SIGNATURE_PREFIX.len()..].trim().to_string());
            //     }
            }
        });
        if main_class.is_none() {
            panic!("not found Main Class in jar")
        }
        let comment = archive.comment();
        if comment.len() <= SIGN_LEN_HEX_LEN {
            panic!("jar not signature")
        }
        let len_without_suffix = comment.len() - SIGN_LEN_HEX_LEN;
        let sign_base64_len = byte_utils::byte_to_u32(
            &hex::decode(&comment[..len_without_suffix]).expect("jar signature info is invalid")) as usize;
        let sign_base64 = &comment[(len_without_suffix-sign_base64_len)..len_without_suffix];
        let sign = STANDARD.decode(sign_base64).expect("jar signature is invalid");
        signature = Some(sign);
        if signature.is_none() {
            // signature = Some(String::from("xxx"));
            panic!("not found Signature in jar")
        }
        if let (Some(main_class), Some(signature)) = (main_class, signature) {
            JarInfo {
                path: path.to_string(),
                // file: file_lock,
                signature,
                main_class
            }
        } else {
            panic!("jar is invalid: not found main class or not found signature")
        }
    }

    pub fn verify(&self) -> bool {
        // PUB_KEY
        true
    }

    pub fn path(&self) -> &String {
        &self.path
    }
    // pub fn file(&self) -> &FileLock {
    //     &self.file
    // }
    pub fn signature(&self) -> &Vec<u8> {
        &self.signature
    }
    pub fn main_class(&self) -> &String {
        &self.main_class
    }
}
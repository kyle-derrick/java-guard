use crate::base::common::{pub_key_pair, MAIN_CLASS_PREFIX, MANIFEST_FILE, SIGN_LEN_HEX_LEN};
use crate::util::byte_utils;
use base64::prelude::BASE64_URL_SAFE_NO_PAD;
use base64::Engine;
use std::fs::File;
// use file_lock::{FileLock, FileOptions};
use std::{fs, io, str};
use zip::ZipArchive;

#[allow(unused)]
#[derive(Debug)]
pub struct JarInfo {
    path: String,
    // file: FileLock,
    signature: Vec<u8>,
    jar_data_end_index: usize,
    main_class: String
}

// const MANIFEST_FILE: &str = "META-INF/MANIFEST.MF";
// const MAIN_CLASS_PREFIX: &str = "Main-Class:";
// const SIGNATURE_PREFIX: &str = "JG-Signature:";

fn extract_sign_from_comment(comment: &[u8]) -> Vec<u8> {
    if comment.len() <= SIGN_LEN_HEX_LEN {
        panic!("jar not signature")
    }
    let len_without_suffix = comment.len() - SIGN_LEN_HEX_LEN;
    let sign_len_hex = str::from_utf8(&comment[len_without_suffix..])
        .expect("jar signature info is invalid");
    let sign_base64_len = byte_utils::byte_to_u16(&hex::decode(sign_len_hex)
        .expect("jar signature info is invalid")) as usize;
    let sign_base64 = str::from_utf8(&comment[(len_without_suffix-sign_base64_len)..len_without_suffix])
        .expect("jar signature is invalid");
    BASE64_URL_SAFE_NO_PAD.decode(sign_base64)
        .expect("jar signature is invalid")
}

impl JarInfo {
    pub fn parse(path: &str) -> Self {
        let jar_file = File::open(path).expect(&format!("can not open jar: {}", path));
        let jar_file_len = jar_file.metadata().expect("cannot get jar file metadata").len();
        if jar_file_len > usize::MAX as u64 {
            // Currently only supports 4G and below, support later
            panic!("The jar file is too large, exceeding {}", usize::MAX)
        }
        let mut archive =  ZipArchive::new(jar_file)
            .expect(&format!("can not open jar: {}", path));
        let manifest = archive.by_name(MANIFEST_FILE).expect("not found MANIFEST.MF in jar");
        let manifest_content = io::read_to_string(manifest).expect("cannot read MANIFEST.MF in jar");
        let mut main_class = None;
        manifest_content.lines().for_each(|line| {
            if line.starts_with(MAIN_CLASS_PREFIX) {
                if main_class.is_none() {
                    main_class = Some(line[MAIN_CLASS_PREFIX.len()..].trim().to_string());
                }
            }
        });
        if main_class.is_none() {
            panic!("not found Main Class in jar")
        }
        let comment = archive.comment();
        let sign = extract_sign_from_comment(comment);
        let signature = Some(sign);
        if signature.is_none() {
            // signature = Some(Vec::new());
            panic!("not found Signature in jar")
        }
        if let (Some(main_class), Some(signature)) = (main_class, signature) {
            JarInfo {
                path: path.to_string(),
                // file: file_lock,
                signature,
                jar_data_end_index: jar_file_len as usize - comment.len() - 2,
                main_class
            }
        } else {
            panic!("jar is invalid: not found main class or not found signature")
        }
    }

    pub fn verify(&self) {
        let content = fs::read(&self.path).expect(&format!("cannot read jar file: {}", &self.path));
        pub_key_pair().verify(&content[..self.jar_data_end_index], &self.signature)
            .expect("jar signature verify failed");
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
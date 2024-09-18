use std::fs::File;
// use file_lock::{FileLock, FileOptions};
use std::io;
use zip::ZipArchive;

#[derive(Debug)]
pub struct JarInfo {
    path: String,
    // file: FileLock,
    signature: String,
    main_class: String
}

const MANIFEST_FILE: &str = "META-INF/MANIFEST.MF";
const MAIN_CLASS_PREFIX: &str = "Main-Class:";
const SIGNATURE_PREFIX: &str = "JG-Signature:";

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
            } else if line.starts_with(SIGNATURE_PREFIX) {
                if signature.is_none() {
                    signature = Some(line[SIGNATURE_PREFIX.len()..].trim().to_string());
                }
            }
        });
        if main_class.is_none() {
            panic!("not found Main Class in jar")
        }
        if signature.is_none() {
            signature = Some(String::from("xxx"));
            // panic!("not found Signature in jar")
        }
        if let (Some(main_class), Some(signature)) = (main_class, signature) {
            JarInfo {
                path: path.to_string(),
                // file: file_lock,
                signature,
                main_class
            }
        } else {
            panic!("")
        }
    }
    pub fn path(&self) -> &String {
        &self.path
    }
    // pub fn file(&self) -> &FileLock {
    //     &self.file
    // }
    pub fn signature(&self) -> &String {
        &self.signature
    }
    pub fn main_class(&self) -> &String {
        &self.main_class
    }
}
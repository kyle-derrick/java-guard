use std::path::{Path, PathBuf};
#[cfg(target_os = "windows")]
const DEFAULT_INCLUDES: &str = "jdk_include/windows";

#[cfg(target_os = "linux")]
const DEFAULT_INCLUDES: &str = "jdk_include/linux";

#[cfg(target_os = "macos")]
const DEFAULT_INCLUDES: &str = "jdk_include/darwin";

fn main() {
    let mut builder = cc::Build::new();
    let includes = if let Some(path) = jdk_includes() {
        path
    } else {
        vec![PathBuf::from(DEFAULT_INCLUDES)]
    };
    println!(">>> jdk includes: {:?}", includes);
    builder
        .includes(&includes)
        .file("c_src/lib.c")
        .compile("jg-jvmti-wrapper");
}

fn jdk_includes() -> Option<Vec<PathBuf>> {
    let include_dir = java_locator::locate_file("jvmti.h").ok()?;
    let mut vec = Vec::new();
    let include_dir = Path::new(&include_dir);
    vec.push(include_dir.to_path_buf());
    let child = include_dir.read_dir().ok()?;
    for entry in child {
        let entry = entry.ok()?;
        let path_buf = entry.path();
        if path_buf.is_dir() {
            vec.push(path_buf.clone())
        }
    }
    Some(vec)
}
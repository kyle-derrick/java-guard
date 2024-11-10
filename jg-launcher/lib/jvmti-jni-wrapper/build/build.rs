fn main() {
    let mut builder = cc::Build::new();
    let includes = [
        "jdk_include",
        "jdk_include/linux",
    ];
    builder
        .includes(&includes)
        .file("src/lib.c")
        .compile("jvmti-jni-wrapper");
}
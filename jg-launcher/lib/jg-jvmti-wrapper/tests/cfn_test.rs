
use jg_jvmti_wrapper::{struct_test, test_base};

#[test]
fn c_base_test() {
    let i = 2;
    let result = unsafe {
        test_base(i)
    };
    println!("::: {result}");
}

#[test]
fn c_struct_test() {
    let result = unsafe {
        struct_test()
    };
    println!("::: {result}");
}
use args_parser::LauncherArg;
use crate::jvm_launcher::jvm_launch;

mod args_parser;
mod jvm_launcher;
mod jar_info;

include!(concat!(env!("OUT_DIR"), "/_common.rs"));

fn main() {
    let arg = LauncherArg::get();
    println!("{:#?}", arg);
    jvm_launch(arg);
}
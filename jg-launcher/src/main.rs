use args_parser::LauncherArg;
use crate::jvm_launcher::jvm_launch;

mod args_parser;
mod jvm_launcher;
mod jar_info;
mod util;
mod common;

fn main() {
    let arg = LauncherArg::get();
    println!("{:#?}", arg);
    jvm_launch(arg);
}